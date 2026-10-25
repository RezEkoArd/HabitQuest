package com.rezekoard.habitquest.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun firstLaunchIsEmptyAndOnboardingIsStoredOnlyOnce() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val repo = HabitRepository(db)
            val empty = repo.observe().first()
            assertNull(empty.profile)
            assertTrue(empty.habits.isEmpty())
            assertTrue(empty.progress.isEmpty())
            repo.completeOnboarding("  Sari   Putri  ")
            repo.completeOnboarding("Nama kedua")
            assertEquals("Sari Putri", repo.observe().first().profile?.displayName)
        } finally { db.close() }
    }

    @Test fun nameHabitProgressAndXpSurviveDatabaseReopen() = runBlocking {
        val name = "habitquest-test-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, HabitDatabase::class.java, name).build()
        try {
            val repo = HabitRepository(db)
            repo.completeOnboarding("Sari")
            val id = repo.addHabit("Baca buku", "Belajar", 15, "halaman", LocalTime.of(20, 0))
            assertEquals(0, repo.advance(id, 5))
            assertEquals(20, repo.advance(id, 10))
            db.close()
            db = Room.databaseBuilder(context, HabitDatabase::class.java, name).build()
            val reopened = HabitRepository(db)
            val stored = reopened.observe().first()
            assertEquals("Sari", stored.profile?.displayName)
            assertEquals("Baca buku", stored.habits.single().name)
            assertEquals("20:00", stored.habits.single().reminderTime)
            assertEquals(15, stored.progress.single().amount)
            assertEquals(20, stored.progress.sumOf { it.xpEarned })
            assertEquals(0, reopened.advance(id, 5))
            assertEquals(20, reopened.observe().first().progress.sumOf { it.xpEarned })
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun eachDayKeepsItsOwnProgressAndOneXpReward() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        var today = LocalDate.of(2026, 10, 25)
        try {
            val repo = HabitRepository(db) { today }
            repo.completeOnboarding("Sari")
            val id = repo.addHabit("Lari", "Olahraga", 3, "km", null)
            assertEquals(20, repo.advance(id, 3))
            today = today.plusDays(1)
            assertEquals(0, repo.advance(id, 1))
            assertEquals(20, repo.advance(id, 2))
            today = today.minusDays(1)
            assertEquals(0, repo.advance(id, 3))
            val records = repo.observe().first().progress
            assertEquals(2, records.size)
            assertEquals(40, records.sumOf { it.xpEarned })
        } finally { db.close() }
    }
}
