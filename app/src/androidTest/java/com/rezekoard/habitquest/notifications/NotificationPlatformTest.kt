package com.rezekoard.habitquest.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rezekoard.habitquest.data.HabitDatabase
import com.rezekoard.habitquest.data.HabitRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses only a temporary test habit; existing user habits and profile are retained. */
@RunWith(AndroidJUnit4::class)
class NotificationPlatformTest {
    private fun screenText(node: AccessibilityNodeInfo?): List<String> = buildList {
        if (node == null) return@buildList
        node.text?.let { add(it.toString()) }
        node.contentDescription?.let { add(it.toString()) }
        repeat(node.childCount) { addAll(screenText(node.getChild(it))) }
    }
    @Test fun realNotificationHasPartialProgressAndOpensTheHabitLog() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val notifications = AndroidReminderNotifications(context)
        assumeTrue("Notification channel is disabled by user", notifications.enabled())
        val db = HabitDatabase.getInstance(context)
        val repo = HabitRepository(db)
        val createdProfile = db.habitDao().profile() == null
        var habitId: Long? = null
        try {
            if (createdProfile) repo.completeOnboarding("Pengujian notifikasi")
            val id = repo.addHabit("Uji pengingat sementara", "Belajar", 15, "halaman", LocalTime.MIDNIGHT)
            habitId = id
            repo.advance(id, 10)
            val manager = HabitReminderManager(context)
            manager.deliver(id, LocalDate.now(), LocalTime.MIDNIGHT)
            val system = context.getSystemService(NotificationManager::class.java)
            withTimeout(5_000) { while (system.activeNotifications.none { it.tag == "habit:$id" }) delay(100) }
            val notification = system.activeNotifications.first { it.tag == "habit:$id" }.notification
            assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("Tinggal 5 halaman"))
            notification.contentIntent.send()
            withTimeout(30_000) {
                while (true) {
                    val texts = screenText(instrumentation.uiAutomation.rootInActiveWindow)
                    if (texts.any { it.contains("Catat progres") } && texts.any { it.contains("Uji pengingat sementara") }) break
                    delay(200)
                }
            }
            repo.advance(id, 5)
            manager.reconcileHabit(id)
            assertTrue(system.activeNotifications.none { it.tag == "habit:$id" })
            assertEquals(1, db.habitDao().deliveryCount(id, LocalDate.now().toEpochDay()))
        } finally {
            habitId?.let { id ->
                AndroidReminderScheduler(context).cancel(id)
                notifications.cancel(id)
                db.withTransaction { db.openHelper.writableDatabase.execSQL("DELETE FROM habits WHERE id = ?", arrayOf(id)) }
            }
            if (createdProfile) db.withTransaction {
                db.openHelper.writableDatabase.execSQL("DELETE FROM user_profile WHERE id = 1 AND displayName = 'Pengujian notifikasi'")
            }
        }
    }
}
