package com.rezekoard.habitquest.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitFeaturesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val monday = LocalDate.of(2026, 10, 26)

    @Test fun notesAreDatedEditableAndDoNotAffectXp(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(db) { monday }
            repo.completeOnboarding("Sari")
            val id = repo.addHabit("Baca", "Belajar", 15, "halaman", null)
            repo.saveNote(id, monday, "  Buku Kotlin bab 1  ")
            assertEquals("Buku Kotlin bab 1", repo.snapshot().notes.single().body)
            assertTrue(repo.snapshot().progress.isEmpty())
            repo.advance(id, 15)
            repo.saveNote(id, monday, "Bab 2")
            assertEquals(20, repo.snapshot().progress.single().xpEarned)
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.saveNote(id, monday.plusDays(1), "Besok") } }
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.saveNote(id, monday, "x".repeat(2001)) } }
            repo.setArchived(id, true)
            repo.saveNote(id, monday, "Catatan setelah arsip")
            assertEquals("Catatan setelah arsip", repo.snapshot().notes.single().body)
            repo.saveNote(id, monday, "   ")
            assertTrue(repo.snapshot().notes.isEmpty())
        } finally { db.close() }
    }

    @Test fun habitOrderSkipsArchivesAndNewHabitsGoLast(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(db) { monday }
            repo.completeOnboarding("Sari")
            val first = repo.addHabit("A", "Personal", 1, "kali", null)
            val archived = repo.addHabit("B", "Personal", 1, "kali", null)
            val last = repo.addHabit("C", "Personal", 1, "kali", null)
            repo.setArchived(archived, true)
            repo.moveHabit(last, -1)
            assertEquals(listOf(last, first), repo.snapshot().habits.filter { !it.archived }.map { it.id })
            repo.moveHabit(last, -1) // Already first.
            val added = repo.addHabit("D", "Personal", 1, "kali", null)
            assertEquals(listOf(last, first, added), repo.snapshot().habits.filter { !it.archived }.map { it.id })
            repo.moveHabit(last, 1)
            assertEquals(listOf(first, last, added), repo.snapshot().habits.filter { !it.archived }.map { it.id })
        } finally { db.close() }
    }

    @Test fun backupIncludesNotesAndOrderAndAcceptsLegacyVersionOne(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(db) { monday }
            repo.completeOnboarding("Sari")
            val first = repo.addHabit("A", "Personal", 1, "kali", null)
            val second = repo.addHabit("B", "Personal", 1, "kali", null)
            repo.saveNote(first, monday, "Catatan tersimpan")
            repo.moveHabit(second, -1)
            val snapshot = repo.snapshot()
            val json = BackupCodec.encode(snapshot)
            assertEquals(snapshot, BackupCodec.decode(json))
            repo.saveNote(first, monday, "Perubahan sementara")
            repo.restore(BackupCodec.decode(json))
            assertEquals(snapshot, repo.snapshot())
            val legacy = JSONObject(json).put("formatVersion", 1)
            legacy.remove("notes")
            val legacyHabits = legacy.getJSONArray("habits")
            repeat(legacyHabits.length()) { legacyHabits.getJSONObject(it).remove("sortOrder") }
            val decoded = BackupCodec.decode(legacy.toString())
            assertTrue(decoded.notes.isEmpty())
            assertTrue(decoded.habits.all { it.sortOrder == 0 })
        } finally { db.close() }
    }

    @Test fun editsApplyTomorrowAndRetainHistoricalTarget(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        var date = monday
        try {
            val repo = HabitRepository(db) { date }
            repo.completeOnboarding("Sari")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", null)
            repo.advance(id, 3)
            repo.editHabit(id, "Lari pagi", "Olahraga", 5, 21)
            assertEquals("Lari pagi", repo.snapshot().habits.single().name)
            assertEquals(3, db.habitDao().planAt(id, monday.toEpochDay())?.target)
            date = monday.plusDays(1)
            assertThrows(IllegalStateException::class.java) { runBlocking { repo.advance(id, 1) } }
            date = monday.plusDays(2)
            assertEquals(20, repo.advance(id, 5))
            assertEquals(listOf(3, 5), repo.snapshot().progress.map { it.target })
        } finally { db.close() }
    }

    @Test fun correctionsRecalculateXpInsteadOfFarmingRewards(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(db) { monday }
            repo.completeOnboarding("Sari")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", null)
            assertEquals(20, repo.advance(id, 3))
            assertEquals(-20, repo.correctProgress(id, monday, 1))
            assertEquals(20, repo.correctProgress(id, monday, 3))
            assertEquals(0, repo.correctProgress(id, monday, 3))
            assertEquals(20, repo.snapshot().progress.sumOf { it.xpEarned })
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.correctProgress(id, monday, 4) } }
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.correctProgress(id, monday.plusDays(1), 1) } }
            assertEquals(3, repo.snapshot().progress.single().amount)
        } finally { db.close() }
    }

    @Test fun archiveAndRestoreKeepHistoryAndPauseTheSchedule(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        var date = monday
        try {
            val repo = HabitRepository(db) { date }
            repo.completeOnboarding("Sari")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", null)
            repo.advance(id, 3)
            repo.setArchived(id, true)
            assertTrue(repo.snapshot().habits.single().archived)
            assertEquals(20, repo.snapshot().progress.single().xpEarned)
            assertThrows(IllegalStateException::class.java) { runBlocking { repo.advance(id, 1) } }
            date = monday.plusDays(1)
            repo.setArchived(id, false)
            assertEquals(0, db.habitDao().planAt(id, date.toEpochDay())?.weekdays)
            date = date.plusDays(1)
            assertEquals(20, repo.advance(id, 3))
            assertEquals(2, repo.snapshot().progress.size)
        } finally { db.close() }
    }

    @Test fun profileNameIsEditableWithoutRemovingHabits(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(db) { monday }
            repo.completeOnboarding("Sari")
            repo.addHabit("Lari", "Olahraga", 3, "km", null)
            repo.editProfileName("  Sari   Putri ")
            assertEquals("Sari Putri", repo.snapshot().profile?.displayName)
            assertEquals(1, repo.snapshot().habits.size)
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.editProfileName("   ") } }
        } finally { db.close() }
    }

    @Test fun backupRoundTripRestoresPlansArchivedHabitsAndXp(): Unit = runBlocking {
        val source = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        val destination = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(source) { monday }
            repo.completeOnboarding("Sari")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", null, 21)
            repo.advance(id, 3)
            repo.editHabit(id, "Lari pagi", "Olahraga", 5, 21)
            val archived = repo.addHabit("Baca", "Belajar", 15, "halaman", null)
            repo.setArchived(archived, true)
            source.habitDao().claimDelivery(ReminderDeliveryEntity(id, monday.toEpochDay()))
            val snapshot = repo.snapshot()
            val decoded = BackupCodec.decode(BackupCodec.encode(snapshot))
            val target = HabitRepository(destination) { monday }
            target.completeOnboarding("Nama lama")
            target.addHabit("Habit lama", "Personal", 1, "kali", null)
            target.restore(decoded)
            assertEquals(snapshot, target.snapshot())
            assertEquals(20, target.snapshot().progress.sumOf { it.xpEarned })
        } finally { source.close(); destination.close() }
    }

    @Test fun invalidBackupDoesNotReplaceExistingData(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(db) { monday }
            repo.completeOnboarding("Sari")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", null)
            repo.advance(id, 3)
            val before = repo.snapshot()
            val broken = before.copy(progress = before.progress.map { it.copy(xpEarned = 999) })
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.restore(broken) } }
            val json = JSONObject(BackupCodec.encode(before)).put("formatVersion", 999)
            assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(json.toString()) }
            assertEquals(before, repo.snapshot())
        } finally { db.close() }
    }
}
