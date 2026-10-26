package com.rezekoard.habitquest.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class HabitAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_HABIT_ID, -1)
        val date = intent.getLongExtra(EXTRA_DATE, Long.MIN_VALUE)
        val time = intent.getStringExtra(EXTRA_TIME) ?: return
        if (id <= 0 || date == Long.MIN_VALUE) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { withTimeout(8_000) { HabitReminderManager(context.applicationContext).deliver(id, LocalDate.ofEpochDay(date), LocalTime.parse(time)) } }
            catch (error: Exception) { Log.e("HabitReminder", "Pengingat belum dapat dikirim", error) }
            finally { pending.finish() }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { withTimeout(8_000) { HabitReminderManager(context.applicationContext).reconcileAll() } }
            catch (error: Exception) { Log.e("HabitReminder", "Jadwal pengingat belum dapat diperbarui", error) }
            finally { pending.finish() }
        }
    }
}

class HabitSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_HABIT_ID, -1)
        val day = intent.getLongExtra(EXTRA_DATE, Long.MIN_VALUE)
        if (id <= 0 || day == Long.MIN_VALUE || intent.action !in setOf(ACTION_REQUEST_SNOOZE, ACTION_DELIVER_SNOOZE)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    val manager = HabitReminderManager(context.applicationContext)
                    val date = LocalDate.ofEpochDay(day)
                    if (intent.action == ACTION_REQUEST_SNOOZE) manager.snooze(id, date, intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 0))
                    else manager.deliverSnooze(id, date, intent.getLongExtra(EXTRA_TRIGGER_AT, -1))
                }
            } catch (error: Exception) { Log.e("HabitReminder", "Pengingat tunda belum dapat diproses", error) }
            finally { pending.finish() }
        }
    }
}
