package com.rezekoard.habitquest.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderMigrationTest {
    @Test fun migrationPreservesExistingProfileHabitProgressAndXp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-reminders-${UUID.randomUUID()}.db"
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.rezekoard.habitquest.data.HabitDatabase/1.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
                val entities = schema.getJSONArray("entities")
                repeat(entities.length()) { index ->
                    val entity = entities.getJSONObject(index)
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
                val setup = schema.getJSONArray("setupQueries")
                repeat(setup.length()) { db.execSQL(setup.getString(it)) }
                db.execSQL("INSERT INTO user_profile (id, displayName) VALUES (1, 'Sari')")
                db.execSQL("INSERT INTO habits (id, name, category, target, unit, reminderTime, startedOn) VALUES (1, 'Baca', 'Belajar', 15, 'halaman', '20:00', 20000)")
                db.execSQL("INSERT INTO daily_progress (habitId, date, target, amount, xpEarned) VALUES (1, 20000, 15, 15, 20)")
                db.version = 1
            }
            val database = Room.databaseBuilder(context, HabitDatabase::class.java, name).addMigrations(HabitDatabase.MIGRATION_1_2).build()
            try {
                // Opening Room applies the real migration and validates every table against schema v2.
                val db = database.openHelper.writableDatabase
                assertEquals(2, db.version)
                db.query("SELECT displayName FROM user_profile").use { it.moveToFirst(); assertEquals("Sari", it.getString(0)) }
                db.query("SELECT xpEarned FROM daily_progress").use { it.moveToFirst(); assertEquals(20, it.getInt(0)) }
                db.query("SELECT COUNT(*) FROM reminder_deliveries").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            } finally { database.close() }
        } finally { context.deleteDatabase(name) }
    }
}
