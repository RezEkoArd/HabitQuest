package com.rezekoard.habitquest.notifications

import android.content.Context
import androidx.room.withTransaction
import com.rezekoard.habitquest.data.HabitDatabase
import com.rezekoard.habitquest.data.ReminderDeliveryEntity
import com.rezekoard.habitquest.domain.nextReminderAt
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

    suspend fun reconcileAll() {
        notifications.clearStale(clock().toLocalDate())
        dao.allHabits().forEach { reconcileHabit(it.id) }
    }

    suspend fun reminderChanged(id: Long) {
        scheduler.cancel(id)
        notifications.cancel(id)
        reconcileHabit(id)
    }

    suspend fun reconcileHabit(id: Long) {
        val habit = dao.habit(id)
        val time = habit?.reminderTime?.let(LocalTime::parse)
        if (habit == null || time == null || !notifications.enabled()) {
            scheduler.cancel(id)
            notifications.cancel(id)
            return
        }
        val now = clock()
        val progress = dao.progress(id, now.toLocalDate().toEpochDay())
        val complete = progress != null && progress.amount >= progress.target
        if (complete) notifications.cancel(id)
        val handled = complete || dao.deliveryCount(id, now.toLocalDate().toEpochDay()) > 0
        scheduler.schedule(id, time, nextReminderAt(time, now, handled))
    }

    suspend fun deliver(id: Long, date: LocalDate, requestedTime: LocalTime) {
        try {
            database.withTransaction {
                val habit = dao.habit(id) ?: return@withTransaction
                val now = clock()
                val record = dao.progress(id, date.toEpochDay())
                val complete = record != null && record.amount >= record.target
                if (!notifications.enabled() || !shouldDeliverReminder(date, requestedTime, habit.reminderTime?.let(LocalTime::parse), now, complete)) return@withTransaction
                if (date.toEpochDay() < habit.startedOn) return@withTransaction
                if (dao.claimDelivery(ReminderDeliveryEntity(id, date.toEpochDay())) == -1L) return@withTransaction
                // Posting inside this short transaction lets completion serialize before cancelling the notification.
                notifications.show(ReminderMessage(id, date, habit.name, record?.amount ?: 0, record?.target ?: habit.target, habit.unit))
            }
        } finally {
            reconcileHabit(id)
        }
    }
}
