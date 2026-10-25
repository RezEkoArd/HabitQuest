package com.rezekoard.habitquest.domain

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitHistoryTest {
    private val today = LocalDate.of(2026, 10, 25)
    private val habit = Habit(1, "Baca", "Belajar", 15, "halaman", startedOn = today.withDayOfMonth(1))

    @Test fun totalsIncludePartialProgressButOnlyCompletedTargetsCountAsSuccess() {
        val records = listOf(
            DailyHabitProgress(1, today.minusDays(1), 15, 15),
            DailyHabitProgress(1, today, 15, 10),
            DailyHabitProgress(1, today.plusDays(1), 15, 15),
            DailyHabitProgress(2, today, 15, 15),
        )
        val result = summarizeMonth(habit, records, YearMonth.from(today), today)
        assertEquals(1, result.completedDays)
        assertEquals(25, result.scheduledDays)
        assertEquals(25L, result.totalAmount)
        assertEquals(4, result.completionPercent)
    }

    @Test fun newHabitIsMeasuredFromItsCreationDate() {
        val newHabit = habit.copy(startedOn = today)
        val result = summarizeMonth(newHabit, listOf(DailyHabitProgress(1, today, 15, 15)), YearMonth.from(today), today)
        assertEquals(1, result.scheduledDays)
        assertEquals(100, result.completionPercent)
        assertEquals(MonthlyHabitSummary(0, 0, 0), summarizeMonth(newHabit, emptyList(), YearMonth.of(2026, 9), today))
    }

    @Test fun leapYearAndEmptyFutureMonthHaveCorrectDenominators() {
        val earlierHabit = habit.copy(startedOn = LocalDate.of(2024, 2, 1))
        assertEquals(29, summarizeMonth(earlierHabit, emptyList(), YearMonth.of(2024, 2), today).scheduledDays)
        assertEquals(0, summarizeMonth(habit, emptyList(), YearMonth.of(2026, 11), today).scheduledDays)
    }

    @Test fun calendarDistinguishesIncompleteTodayFromMissedAndUnscheduledDates() {
        assertEquals(DayStatus.TODAY_EMPTY, dayStatus(habit, today, today, null))
        assertEquals(DayStatus.MISSED, dayStatus(habit, today.minusDays(1), today, null))
        assertEquals(DayStatus.NOT_STARTED, dayStatus(habit, habit.startedOn.minusDays(1), today, null))
        assertEquals(DayStatus.FUTURE, dayStatus(habit, today.plusDays(1), today, null))
        assertEquals(DayStatus.PARTIAL, dayStatus(habit, today, today, DailyHabitProgress(1, today, 15, 10)))
        assertEquals(DayStatus.COMPLETE, dayStatus(habit, today, today, DailyHabitProgress(1, today, 15, 15)))
    }

    @Test fun streakUsesConsecutiveDatesAndCapturedHistoricalTargets() {
        val records = listOf(1, 2, 3, 5, 6).map { day -> DailyHabitProgress(1, today.withDayOfMonth(day), 10, 10) } +
            DailyHabitProgress(1, today.withDayOfMonth(4), 10, 5)
        assertEquals(3, bestStreak(habit, records, today))
        assertEquals(5, summarizeMonth(habit, records, YearMonth.from(today), today).completedDays)
    }
}
