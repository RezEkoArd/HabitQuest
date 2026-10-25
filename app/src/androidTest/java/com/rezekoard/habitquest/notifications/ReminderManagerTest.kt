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
        override fun schedule(id: Long, time: LocalTime, at: ZonedDateTime) { scheduled[id] = at }
        override fun cancel(id: Long) { scheduled.remove(id) }
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
    private suspend fun fixture(block: suspend (HabitRepository, HabitReminderManager, Scheduler, Notifications, Long) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(db) { now.toLocalDate() }
            repo.completeOnboarding("Pengujian")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", time)
            val scheduler = Scheduler()
            val notifications = Notifications()
            val manager = HabitReminderManager(context, db, scheduler, notifications) { now }
            block(repo, manager, scheduler, notifications, id)
        } finally { db.close() }
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
}
