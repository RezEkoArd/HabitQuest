package com.rezekoard.habitquest.notifications

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rezekoard.habitquest.data.HabitDatabase
import com.rezekoard.habitquest.data.HabitRepository
import java.time.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = ZonedDateTime.of(2026, 10, 25, 7, 5, 0, 0, ZoneId.of("Asia/Jakarta"))
    private val time = LocalTime.of(7, 0)
    private class Scheduler : ReminderScheduler {
        val scheduled = mutableMapOf<Long, ZonedDateTime>()
        val snoozes = mutableMapOf<Long, Long>()
        override fun schedule(id: Long, time: LocalTime, at: ZonedDateTime) { scheduled[id] = at }
        override fun cancel(id: Long) { scheduled.remove(id) }
        override fun scheduleSnooze(id: Long, date: LocalDate, triggerAt: Long) { snoozes[id] = triggerAt }
        override fun cancelSnooze(id: Long) { snoozes.remove(id) }
    }
    private class Notifications : ReminderNotifications {
        var allowed = true
        val sent = mutableListOf<ReminderMessage>()
        val visible = mutableMapOf<Long, ReminderMessage>()
        override fun enabled() = allowed
        override fun show(message: ReminderMessage) { sent += message; visible[message.habitId] = message }
        override fun cancel(id: Long) { visible.remove(id) }
        override fun clearStale(today: LocalDate) { visible.entries.removeAll { it.value.date != today } }
    }
    private suspend fun fixture(weekdays: Int = 127, block: suspend (HabitRepository, HabitReminderManager, Scheduler, Notifications, Long) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(db) { now.toLocalDate() }
            repo.completeOnboarding("Pengujian")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", time, weekdays)
            val scheduler = Scheduler()
            val notifications = Notifications()
            val manager = HabitReminderManager(context, db, scheduler, notifications) { now }
            block(repo, manager, scheduler, notifications, id)
        } finally { db.close() }
    }

    @Test fun customDaysAndArchiveControlReminders() = runBlocking {
        fixture(21) { repo, manager, scheduler, notifications, id ->
            manager.deliver(id, now.toLocalDate(), time) // Sunday is not selected.
            assertTrue(notifications.sent.isEmpty())
            assertEquals(now.toLocalDate().plusDays(1), scheduler.scheduled[id]?.toLocalDate())
            repo.setArchived(id, true)
            manager.reminderChanged(id)
            assertTrue(scheduler.scheduled.isEmpty())
        }
    }

    @Test fun partialReminderPostsOnceEvenAfterDismissalAndCancelsOnCompletion() = runBlocking {
        fixture { repo, manager, scheduler, notifications, id ->
            repo.advance(id, 1)
            manager.deliver(id, now.toLocalDate(), time)
            assertEquals(1, notifications.sent.single().amount)
            assertEquals(now.toLocalDate().plusDays(1), scheduler.scheduled[id]?.toLocalDate())
            notifications.visible.clear() // User swipes the notification away.
            manager.deliver(id, now.toLocalDate(), time)
            assertEquals(1, notifications.sent.size)
            repo.advance(id, 2)
            manager.reconcileHabit(id)
            assertTrue(notifications.visible.isEmpty())
        }
    }
    @Test fun completedAndExpiredRemindersAreSkipped() = runBlocking {
        fixture { repo, manager, _, notifications, id ->
            manager.deliver(id, now.toLocalDate().minusDays(1), time)
            assertTrue(notifications.sent.isEmpty())
            repo.advance(id, 3)
            manager.deliver(id, now.toLocalDate(), time)
            assertTrue(notifications.sent.isEmpty())
        }
    }
    @Test fun changingOrDisablingReminderReplacesScheduleAndRejectsOldTrigger() = runBlocking {
        fixture { repo, manager, scheduler, notifications, id ->
            repo.updateReminder(id, LocalTime.of(8, 0))
            manager.reminderChanged(id)
            assertEquals(LocalTime.of(8, 0), scheduler.scheduled[id]?.toLocalTime())
            manager.deliver(id, now.toLocalDate(), time)
            assertTrue(notifications.sent.isEmpty())
            repo.updateReminder(id, null)
            manager.reminderChanged(id)
            assertTrue(scheduler.scheduled.isEmpty())
            assertTrue(notifications.visible.isEmpty())
        }
    }
    @Test fun deniedPermissionDoesNotPostOrScheduleUntilReenabled() = runBlocking {
        fixture { _, manager, scheduler, notifications, id ->
            notifications.allowed = false
            manager.deliver(id, now.toLocalDate(), time)
            assertTrue(notifications.sent.isEmpty())
            assertTrue(scheduler.scheduled.isEmpty())
            notifications.allowed = true
            manager.reconcileAll()
            assertTrue(scheduler.scheduled.containsKey(id))
        }
    }

    @Test fun snoozeSurvivesReconcileDeliversOnceAndKeepsDailyLedger(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        var clock = now
        try {
            val repo = HabitRepository(db) { clock.toLocalDate() }
            repo.completeOnboarding("Uji")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", time)
            val scheduler = Scheduler()
            val notifications = Notifications()
            fun manager() = HabitReminderManager(context, db, scheduler, notifications) { clock }
            manager().deliver(id, clock.toLocalDate(), time)
            manager().snooze(id, clock.toLocalDate(), 10)
            val trigger = clock.plusMinutes(10).toInstant().toEpochMilli()
            assertEquals(trigger, scheduler.snoozes[id])
            assertTrue(notifications.visible.isEmpty())
            manager().reconcileAll() // New manager, same persisted snooze (also used after boot).
            assertEquals(trigger, scheduler.snoozes[id])
            manager().deliverSnooze(id, clock.toLocalDate(), trigger)
            assertEquals(1, notifications.sent.size) // Early alarm ignored.
            clock = clock.plusMinutes(10)
            manager().deliverSnooze(id, clock.toLocalDate(), trigger)
            manager().deliverSnooze(id, clock.toLocalDate(), trigger)
            assertEquals(2, notifications.sent.size)
            assertEquals(1, db.habitDao().deliveryCount(id, clock.toLocalDate().toEpochDay()))
            assertNull(db.habitDao().snooze(id))
            manager().snooze(id, clock.toLocalDate(), 30)
            repo.advance(id, 3)
            manager().reconcileHabit(id)
            assertTrue(scheduler.snoozes.isEmpty())
            assertNull(db.habitDao().snooze(id))
        } finally { db.close() }
    }

    @Test fun snoozeRejectsOldDatesAndStopsAfterArchiveOrTimeChange(): Unit = runBlocking {
        fixture { repo, manager, scheduler, notifications, id ->
            manager.deliver(id, now.toLocalDate(), time)
            manager.snooze(id, now.toLocalDate().minusDays(1), 10)
            manager.snooze(id, now.toLocalDate(), 5)
            assertTrue(scheduler.snoozes.isEmpty())
            manager.snooze(id, now.toLocalDate(), 30)
            assertEquals(1, scheduler.snoozes.size)
            repo.updateReminder(id, LocalTime.of(8, 0))
            manager.reminderChanged(id)
            assertTrue(scheduler.snoozes.isEmpty())
            manager.snooze(id, now.toLocalDate(), 10)
            repo.setArchived(id, true)
            manager.reconcileHabit(id)
            assertTrue(scheduler.snoozes.isEmpty())
            assertTrue(notifications.visible.isEmpty())
        }
    }

    @Test fun snoozeDoesNotCrossMidnight(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        val late = now.withHour(23).withMinute(55)
        try {
            val repo = HabitRepository(db) { late.toLocalDate() }
            repo.completeOnboarding("Uji")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", time)
            val scheduler = Scheduler()
            val notifications = Notifications()
            val manager = HabitReminderManager(context, db, scheduler, notifications) { late }
            manager.deliver(id, late.toLocalDate(), time)
            manager.snooze(id, late.toLocalDate(), 10)
            assertTrue(scheduler.snoozes.isEmpty())
            assertNull(db.habitDao().snooze(id))
        } finally { db.close() }
    }
}
