package com.rezekoard.habitquest.data

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.graphics.Rect
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rezekoard.habitquest.MainActivity
import com.rezekoard.habitquest.notifications.HabitReminderManager
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitFeatureUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = buildList {
        if (node == null) return@buildList
        add(node)
        repeat(node.childCount) { addAll(nodes(node.getChild(it))) }
    }
    private fun tree() = nodes(instrumentation.uiAutomation.rootInActiveWindow)
    private suspend fun label(text: String, forward: Boolean = true, allowScroll: Boolean = true): AccessibilityNodeInfo {
        delay(300)
        return withTimeout(15_000) {
            var found: AccessibilityNodeInfo? = null
            while (found == null) {
                val all = tree()
                found = all.firstOrNull { it.isVisibleToUser && (it.text?.toString() == text || it.contentDescription?.toString() == text) }
                if (found == null && allowScroll) {
                    val scroll = all.firstOrNull { candidate ->
                        val rect = Rect().also(candidate::getBoundsInScreen)
                        candidate.isScrollable && !candidate.isEditable && candidate.isVisibleToUser && rect.height() > 250
                    } ?: instrumentation.uiAutomation.rootInActiveWindow
                    if (scroll != null) swipe(scroll, forward)
                    delay(200)
                }
                if (found == null) delay(150)
            }
            found
        }
    }
    private suspend fun tap(text: String, forward: Boolean = true) {
        println("UI tap: $text")
        val node = label(text, forward)
        click(node)
        delay(150)
    }
    private fun click(node: AccessibilityNodeInfo) {
        var actionable = node
        while (!actionable.isClickable && actionable.parent != null) actionable = actionable.parent
        if (actionable.isClickable && actionable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
        val bounds = Rect().also(node::getBoundsInScreen)
        val at = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(at, at + if (action == MotionEvent.ACTION_UP) 40 else 0, action, bounds.exactCenterX(), bounds.exactCenterY(), 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.uiAutomation.injectInputEvent(event, true)
            event.recycle()
        }
    }
    private fun swipe(node: AccessibilityNodeInfo, forward: Boolean) {
        val rect = Rect().also(node::getBoundsInScreen)
        val start = rect.top + rect.height() * if (forward) .75f else .25f
        val end = rect.top + rect.height() * if (forward) .25f else .75f
        val at = SystemClock.uptimeMillis()
        for (i in 0..12) {
            val action = if (i == 0) MotionEvent.ACTION_DOWN else if (i == 12) MotionEvent.ACTION_UP else MotionEvent.ACTION_MOVE
            val event = MotionEvent.obtain(at, SystemClock.uptimeMillis(), action, rect.exactCenterX(), start + (end - start) * i / 12f, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.uiAutomation.injectInputEvent(event, true)
            event.recycle()
            SystemClock.sleep(16)
        }
    }
    private suspend fun replace(value: String, replacement: String) {
        withTimeout(10_000) {
            while (true) {
                val input = tree().firstOrNull { it.isEditable && it.text?.toString() == value }
                if (input != null) {
                    check(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replacement)
                    }))
                    delay(400)
                    break
                }
                delay(150)
            }
        }
    }
    private fun monitor(action: String, file: File) = object : Instrumentation.ActivityMonitor() {
        override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? =
            if (intent.action == action) Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file))) else null
    }

    @Test fun profileNameAndBackupRestoreWorkThroughUi(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = HabitDatabase.getInstance(context)
        val repo = HabitRepository(db)
        // Remove only stale fixtures from an interrupted earlier run of this test.
        db.habitDao().allHabits().filter { it.name in listOf("Habit uji UI", "Habit uji diperbarui") }.forEach {
            db.withTransaction { db.openHelper.writableDatabase.execSQL("DELETE FROM habits WHERE id = ?", arrayOf(it.id)) }
        }
        val originalName = db.habitDao().profile()?.displayName
        val file = File(context.cacheDir, "habitquest-feature-ui-backup.json")
        var id: Long? = null
        val export = monitor(Intent.ACTION_CREATE_DOCUMENT, file)
        val restore = monitor(Intent.ACTION_OPEN_DOCUMENT, file)
        var monitoring = false
        try {
            if (originalName == null) repo.completeOnboarding("Pengujian UI")
            val habitId = repo.addHabit("Habit uji UI", "Olahraga", 3, "km", null)
            id = habitId
            repo.correctProgress(habitId, LocalDate.now(), 1)
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            tap("Catatan Habit uji UI")
            withTimeout(5_000) {
                while (true) {
                    val input = tree().firstOrNull { it.isEditable }
                    if (input != null) {
                        check(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Lari santai di taman")
                        }))
                        delay(400)
                        break
                    }
                    delay(100)
                }
            }
            tap("Simpan catatan")
            withTimeout(5_000) { while (db.habitDao().allNotes().none { it.habitId == habitId && it.body == "Lari santai di taman" }) delay(100) }
            tap("Progres")
            label("Ringkasan semua habit", allowScroll = false)
            tap("Bulanan")
            tap("Mingguan")
            tap("Profil")
            tap("Edit nama profil", forward = false)
            replace(originalName ?: "Pengujian UI", "Nama uji")
            tap("Simpan nama")
            withTimeout(5_000) { while (db.habitDao().profile()?.displayName != "Nama uji") delay(100) }

            // The document contracts receive cache-file URIs, so no user files or provider accounts are touched.
            instrumentation.addMonitor(export)
            instrumentation.addMonitor(restore)
            monitoring = true
            tap("Buat backup offline")
            withTimeout(8_000) { while (!file.exists() || file.length() == 0L) delay(100) }
            val backup = BackupCodec.decode(file.readText())
            assertEquals("Nama uji", backup.profile?.displayName)
            assertTrue(backup.habits.any { it.id == habitId })
            assertTrue(backup.notes.any { it.habitId == habitId && it.body == "Lari santai di taman" })
            tap("Pulihkan backup")
            label("Pulihkan backup?", allowScroll = false)
            tap("Batal")
            repo.editProfileName("Nama setelah backup")
            tap("Pulihkan backup")
            tap("Ganti dan pulihkan")
            label("Backup berhasil dipulihkan.", allowScroll = false)
            tap("Mengerti")
            assertEquals("Nama uji", db.habitDao().profile()?.displayName)
            assertEquals(1, db.habitDao().progress(habitId, LocalDate.now().toEpochDay())?.amount)
            assertTrue(db.habitDao().allNotes().any { it.habitId == habitId && it.body == "Lari santai di taman" })
        } catch (error: Throwable) {
            println("UI failure tree: " + tree().joinToString(" | ") { "${it.text}/${it.contentDescription} [scroll=${it.isScrollable}]" })
            throw error
        } finally {
            if (monitoring) { instrumentation.removeMonitor(export); instrumentation.removeMonitor(restore) }
            id?.let { key ->
                HabitReminderManager(context).cancelAll(listOf(key))
                db.withTransaction { db.openHelper.writableDatabase.execSQL("DELETE FROM habits WHERE id = ?", arrayOf(key)) }
            }
            if (originalName == null) db.withTransaction { db.openHelper.writableDatabase.execSQL("DELETE FROM user_profile WHERE id = 1 AND displayName IN ('Pengujian UI','Nama uji','Nama setelah backup')") }
            else repo.editProfileName(originalName)
            HabitReminderManager(context).reconcileAll()
            file.delete()
        }
    }
}
