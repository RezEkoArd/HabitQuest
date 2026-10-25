package com.rezekoard.habitquest.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.rezekoard.habitquest.MainActivity
import com.rezekoard.habitquest.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

const val EXTRA_HABIT_ID = "habit_id"
const val EXTRA_DATE = "reminder_date"
const val EXTRA_TIME = "reminder_time"
const val REMINDER_CHANNEL = "habit_reminders"
const val ACTION_OPEN_REMINDER = "com.rezekoard.habitquest.OPEN_REMINDER"

interface ReminderScheduler {
    fun schedule(id: Long, time: LocalTime, at: ZonedDateTime)
    fun cancel(id: Long)
}

data class ReminderMessage(val habitId: Long, val date: LocalDate, val name: String, val amount: Int, val target: Int, val unit: String)

interface ReminderNotifications {
    fun enabled(): Boolean
    fun show(message: ReminderMessage)
    fun cancel(id: Long)
    fun clearStale(today: LocalDate)
}

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private fun intent(id: Long) = Intent(context, HabitAlarmReceiver::class.java)
        .setData(Uri.parse("habitquest://reminder/$id")).putExtra(EXTRA_HABIT_ID, id)

    override fun schedule(id: Long, time: LocalTime, at: ZonedDateTime) {
        val pending = PendingIntent.getBroadcast(context, 0,
            intent(id).putExtra(EXTRA_DATE, at.toLocalDate().toEpochDay()).putExtra(EXTRA_TIME, time.toString()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pending)
    }

    override fun cancel(id: Long) {
        val pending = PendingIntent.getBroadcast(context, 0, intent(id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        alarms.cancel(pending)
        pending.cancel()
    }
}

class AndroidReminderNotifications(private val context: Context) : ReminderNotifications {
    private val notifications = context.getSystemService(NotificationManager::class.java)
    init {
        notifications.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "Pengingat habit", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Pengingat harian untuk target habit yang belum selesai."
        })
    }

    override fun enabled(): Boolean =
        (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            notifications.areNotificationsEnabled() && notifications.getNotificationChannel(REMINDER_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    @SuppressLint("MissingPermission") // enabled() checks runtime permission and channel before posting.
    override fun show(message: ReminderMessage) {
        check(enabled()) { "Notifikasi belum diizinkan." }
        val intent = Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_REMINDER)
            .setData(Uri.parse("habitquest://habit/${message.habitId}"))
            .putExtra(EXTRA_HABIT_ID, message.habitId).putExtra(EXTRA_DATE, message.date.toEpochDay())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = if (message.amount > 0) "Sudah ${message.amount} dari ${message.target} ${message.unit}. Tinggal ${message.target - message.amount} ${message.unit} lagi!"
            else "Target hari ini: ${message.target} ${message.unit}. Yuk, mulai langkah kecilmu!"
        val notification = Notification.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_sprout).setContentTitle("Waktunya: ${message.name}").setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(pending).setAutoCancel(true)
            .setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_REMINDER)
            .addExtras(Bundle().apply { putLong(EXTRA_DATE, message.date.toEpochDay()) }).build()
        notifications.notify("habit:${message.habitId}", 1, notification)
    }

    override fun cancel(id: Long) { notifications.cancel("habit:$id", 1) }
    override fun clearStale(today: LocalDate) {
        notifications.activeNotifications.filter { it.tag?.startsWith("habit:") == true && it.notification.extras.getLong(EXTRA_DATE, Long.MIN_VALUE) != today.toEpochDay() }
            .forEach { notifications.cancel(it.tag, it.id) }
    }
}
