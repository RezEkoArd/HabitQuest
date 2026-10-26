package com.rezekoard.habitquest.notifications

import android.content.Context
import androidx.room.withTransaction
import com.rezekoard.habitquest.data.HabitDatabase
import com.rezekoard.habitquest.data.ReminderDeliveryEntity
import com.rezekoard.habitquest.data.ReminderSnoozeEntity
import com.rezekoard.habitquest.domain.nextScheduledReminderAt
import com.rezekoard.habitquest.domain.shouldDeliverReminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class HabitReminderManager(
    context: Context,
    private val database: HabitDatabase = HabitDatabase.getInstance(context),
    private val scheduler: ReminderScheduler = AndroidReminderScheduler(context),
    private val notifications: ReminderNotifications = AndroidReminderNotifications(context),
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    private val dao = database.habitDao()
    fun cancelAll(ids: List<Long>) { ids.forEach { scheduler.cancel(it); scheduler.cancelSnooze(it); notifications.cancel(it) } }

    suspend fun reconcileAll() {
        notifications.clearStale(clock().toLocalDate())
        dao.allHabits().forEach { reconcileHabit(it.id) }
    }

    suspend fun reminderChanged(id: Long) {
        dao.deleteSnooze(id)
        scheduler.cancelSnooze(id)
        scheduler.cancel(id)
        notifications.cancel(id)
        reconcileHabit(id)
    }

    suspend fun reconcileHabit(id: Long) {
        val habit = dao.habit(id)
        val time = habit?.reminderTime?.let(LocalTime::parse)
        if (habit == null || habit.archived || time == null || !notifications.enabled()) {
            dao.deleteSnooze(id)
            scheduler.cancelSnooze(id)
            scheduler.cancel(id)
            notifications.cancel(id)
            return
        }
        val now = clock()
        val plans = dao.allPlans().filter { it.habitId == id }
        fun scheduled(date: LocalDate): Boolean = date.toEpochDay() >= habit.startedOn &&
            (plans.lastOrNull { it.effectiveFrom <= date.toEpochDay() }?.weekdays ?: 0) and (1 shl (date.dayOfWeek.value - 1)) != 0
        val progress = dao.progress(id, now.toLocalDate().toEpochDay())
        val complete = progress != null && progress.amount >= progress.target
        val snooze = dao.snooze(id)
        if (snooze != null && snooze.date == now.toLocalDate().toEpochDay() && snooze.reminderTime == time.toString() && !complete && scheduled(now.toLocalDate())) {
            scheduler.scheduleSnooze(id, now.toLocalDate(), snooze.triggerAt)
        } else {
            dao.deleteSnooze(id)
            scheduler.cancelSnooze(id)
        }
        if (complete || !scheduled(now.toLocalDate())) notifications.cancel(id)
        val handled = complete || dao.deliveryCount(id, now.toLocalDate().toEpochDay()) > 0
        val next = nextScheduledReminderAt(time, now, handled, ::scheduled)
        if (next == null) scheduler.cancel(id) else scheduler.schedule(id, time, next)
    }

    suspend fun deliver(id: Long, date: LocalDate, requestedTime: LocalTime) {
        try {
            database.withTransaction {
                val habit = dao.habit(id) ?: return@withTransaction
                if (habit.archived) return@withTransaction
                val plan = dao.planAt(id, date.toEpochDay()) ?: return@withTransaction
                if (plan.weekdays and (1 shl (date.dayOfWeek.value - 1)) == 0) return@withTransaction
                val now = clock()
                val record = dao.progress(id, date.toEpochDay())
                val complete = record != null && record.amount >= record.target
                if (!notifications.enabled() || !shouldDeliverReminder(date, requestedTime, habit.reminderTime?.let(LocalTime::parse), now, complete)) return@withTransaction
                if (date.toEpochDay() < habit.startedOn) return@withTransaction
                if (dao.claimDelivery(ReminderDeliveryEntity(id, date.toEpochDay())) == -1L) return@withTransaction
                // Posting inside this short transaction lets completion serialize before cancelling the notification.
                notifications.show(ReminderMessage(id, date, habit.name, record?.amount ?: 0, record?.target ?: plan.target, habit.unit))
            }
        } finally {
            reconcileHabit(id)
        }
    }

    suspend fun snooze(id: Long, date: LocalDate, minutes: Int) {
        if (minutes !in listOf(10, 30)) return
        database.withTransaction {
            val now = clock()
            val habit = dao.habit(id) ?: return@withTransaction
            val time = habit.reminderTime ?: return@withTransaction
            val plan = dao.planAt(id, date.toEpochDay()) ?: return@withTransaction
            val record = dao.progress(id, date.toEpochDay())
            if (date != now.toLocalDate() || habit.archived || !notifications.enabled() ||
                dao.deliveryCount(id, date.toEpochDay()) == 0 ||
                plan.weekdays and (1 shl (date.dayOfWeek.value - 1)) == 0 ||
                record?.let { it.amount >= it.target } == true) return@withTransaction
            val trigger = now.plusMinutes(minutes.toLong())
            // A reminder for today's target never carries over into tomorrow.
            if (trigger.toLocalDate() != date) { notifications.cancel(id); return@withTransaction }
            dao.saveSnooze(ReminderSnoozeEntity(id, date.toEpochDay(), trigger.toInstant().toEpochMilli(), time))
            notifications.cancel(id)
        }
        reconcileHabit(id)
    }

    suspend fun deliverSnooze(id: Long, date: LocalDate, triggerAt: Long) {
        try {
            database.withTransaction {
                val snooze = dao.snooze(id) ?: return@withTransaction
                // Ignore superseded alarms, including an alarm already delivered once.
                if (snooze.triggerAt != triggerAt || snooze.date != date.toEpochDay()) return@withTransaction
                val now = clock()
                if (now.toInstant().toEpochMilli() < triggerAt) return@withTransaction
                dao.deleteSnooze(id)
                val habit = dao.habit(id) ?: return@withTransaction
                val plan = dao.planAt(id, date.toEpochDay()) ?: return@withTransaction
                val record = dao.progress(id, date.toEpochDay())
                if (date != now.toLocalDate() || habit.archived || !notifications.enabled() || habit.reminderTime != snooze.reminderTime ||
                    plan.weekdays and (1 shl (date.dayOfWeek.value - 1)) == 0 || record?.let { it.amount >= it.target } == true) return@withTransaction
                notifications.show(ReminderMessage(id, date, habit.name, record?.amount ?: 0, record?.target ?: plan.target, habit.unit))
            }
        } finally { reconcileHabit(id) }
    }
}
