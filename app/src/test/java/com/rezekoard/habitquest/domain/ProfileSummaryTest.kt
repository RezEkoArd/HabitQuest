package com.rezekoard.habitquest.domain

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ProfileSummaryTest {
    private val today = LocalDate.of(2026, 10, 25)
    private val start = LocalDate.of(2026, 9, 1)
    private val habits = listOf(
        Habit(1, "Baca", "Belajar", 10, "halaman", startedOn = start),
        Habit(2, "Lari", "Olahraga", 3, "km", startedOn = start),
    )

    @Test fun activeDaysAreUniqueAndExcludeOtherMonthsAndFutureRecords() {
        val records = listOf(
            DailyHabitProgress(1, today, 10, 5),
            DailyHabitProgress(2, today, 3, 3),
            DailyHabitProgress(1, today.minusDays(1), 10, 0),
            DailyHabitProgress(1, start, 10, 10),
            DailyHabitProgress(1, today.plusDays(1), 10, 10),
            DailyHabitProgress(99, today.minusDays(2), 10, 10),
        )
        val summary = summarizeProfile(habits, records, today)
        assertEquals(1, summary.activeDaysThisMonth)
        assertEquals(2, summary.habitCount)
        assertEquals(2, summary.completedHabitDays)
    }

    @Test fun partialProgressDoesNotUnlockFirstCompletionBadge() {
        val summary = summarizeProfile(habits, listOf(DailyHabitProgress(1, today, 10, 5)), today)
        assertEquals(1, summary.activeDaysThisMonth)
        assertFalse(achievementsFor(summary).first().isUnlocked)
        assertEquals(ProfileSummary(0, 0, 0, 0, 0), summarizeProfile(emptyList(), emptyList(), today))
    }

    @Test fun sevenDayBadgeRequiresOneHabitRatherThanAlternatingHabits() {
        val alternating = (0..6).map { i -> DailyHabitProgress(if (i % 2 == 0) 1 else 2, today.minusDays(i.toLong()), 10, 10) }
        val summary = summarizeProfile(habits, alternating, today)
        assertEquals(1, summary.longestHabitStreak)
        assertFalse(achievementsFor(summary).first { it.id == "week" }.isUnlocked)
        val consecutive = (0..6).map { i -> DailyHabitProgress(1, today.minusDays(i.toLong()), 10, 10) }
        assertTrue(achievementsFor(summarizeProfile(habits, consecutive, today)).first { it.id == "week" }.isUnlocked)
    }

    @Test fun monthBadgeCountsDistinctCompletedDatesForOneHabit() {
        val thirtyDays = (0..29).map { i -> DailyHabitProgress(1, today.minusDays(i.toLong()), 10, 10) }
        val duplicated = thirtyDays + thirtyDays.first()
        val badges = achievementsFor(summarizeProfile(habits, duplicated, today))
        assertEquals(30, badges.first { it.id == "month" }.progress)
        assertTrue(badges.first { it.id == "month" }.isUnlocked)
        assertFalse(achievementsFor(summarizeProfile(habits, thirtyDays.dropLast(1), today)).first { it.id == "month" }.isUnlocked)
    }
}
