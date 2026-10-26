package com.rezekoard.habitquest.domain

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PeriodSummaryTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private val habit = Habit(1, "Lari", "Olahraga", 3, "km", startedOn = monday,
        plans = listOf(HabitPlan(monday, 3, "km", 21)))

    @Test fun summaryUsesScheduledDatesAndRetainsArchivedActivity() {
        val archived = habit.copy(isArchived = true, plans = habit.plans + HabitPlan(monday.plusDays(2), 3, "km", 0))
        val records = listOf(DailyHabitProgress(1, monday, 3, 3), DailyHabitProgress(1, monday.plusDays(2), 3, 1))
        val result = summarizePeriod(listOf(archived), records, monday, monday.plusDays(6), monday.plusDays(6))
        assertEquals(PeriodSummary(1, 2, 2, 20), result)
        assertEquals(50, result.percent)
    }

    @Test fun currentWeekComparesEqualElapsedDays() {
        val today = monday.plusDays(8) // Tuesday of next week.
        val records = listOf(DailyHabitProgress(1, monday, 3, 3), DailyHabitProgress(1, monday.plusDays(7), 3, 3))
        val result = comparePeriods(listOf(habit), records, today, monthly = false, offset = 0)
        assertEquals(1, result.current.scheduled)
        assertEquals(1, result.previous.scheduled)
        assertEquals(0, result.difference)
    }

    @Test fun monthComparisonClampsToPreviousMonthAndSkipsFutureDates() {
        val start = LocalDate.of(2026, 1, 1)
        val daily = habit.copy(startedOn = start, plans = listOf(HabitPlan(start, 3, "km", 127)))
        val result = comparePeriods(listOf(daily), emptyList(), LocalDate.of(2026, 3, 31), monthly = true, offset = 0)
        assertEquals(31, result.current.scheduled)
        assertEquals(28, result.previous.scheduled)
        val running = comparePeriods(listOf(daily), emptyList(), LocalDate.of(2026, 3, 5), monthly = true, offset = 0)
        assertEquals(5, running.current.scheduled)
        assertEquals(5, running.previous.scheduled)
        val completedFebruary = comparePeriods(listOf(daily), emptyList(), LocalDate.of(2026, 3, 5), monthly = true, offset = 1)
        assertEquals(28, completedFebruary.current.scheduled)
        assertEquals(31, completedFebruary.previous.scheduled)
    }

    @Test fun emptyPeriodsHaveNoMisleadingComparison() {
        val result = comparePeriods(emptyList(), emptyList(), monday, monthly = false, offset = 1)
        assertEquals(0, result.current.percent)
        assertNull(result.difference)
    }
}
