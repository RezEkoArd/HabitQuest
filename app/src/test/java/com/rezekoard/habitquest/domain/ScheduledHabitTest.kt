package com.rezekoard.habitquest.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ScheduledHabitTest {
    private val monday = LocalDate.of(2026, 10, 26)
    private val habit = Habit(1, "Lari", "Olahraga", 3, "km", startedOn = monday,
        plans = listOf(HabitPlan(monday, 3, "km", 21))) // Monday, Wednesday, Friday.

    @Test fun restDaysDoNotBreakScheduledStreak() {
        val records = listOf(0L, 2L, 4L).map { DailyHabitProgress(1, monday.plusDays(it), 3, 3) }
        assertEquals(3, bestStreak(habit, records, monday.plusDays(6)))
        assertEquals(DayStatus.REST, dayStatus(habit, monday.plusDays(1), monday.plusDays(6), null))
        assertEquals(3, summarizeMonth(habit, records, YearMonth.from(monday), monday.plusDays(5)).scheduledDays)
    }
    @Test fun missingOneScheduledDayBreaksStreak() {
        val records = listOf(0L, 4L).map { DailyHabitProgress(1, monday.plusDays(it), 3, 3) }
        assertEquals(1, bestStreak(habit, records, monday.plusDays(4)))
    }
    @Test fun futureChangesDoNotRewritePreviousTargetsOrRestDays() {
        val changed = habit.copy(plans = habit.plans + HabitPlan(monday.plusDays(7), 5, "km", 127))
        assertEquals(3, changed.planOn(monday).target)
        assertEquals(5, changed.planOn(monday.plusDays(8)).target)
        assertFalse(changed.isScheduledOn(monday.plusDays(1)))
        assertTrue(changed.isScheduledOn(monday.plusDays(8)))
    }
    @Test fun archivedProgressRemainsVisibleAndCounted() {
        val archived = habit.copy(isArchived = true, plans = habit.plans + HabitPlan(monday.plusDays(2), 3, "km", 0))
        val records = listOf(DailyHabitProgress(1, monday, 3, 3), DailyHabitProgress(1, monday.plusDays(2), 3, 3))
        val summary = summarizeMonth(archived, records, YearMonth.from(monday), monday.plusDays(4))
        assertEquals(2, summary.completedDays)
        assertEquals(2, summary.scheduledDays)
        assertEquals(6L, summary.totalAmount)
        assertEquals(DayStatus.ARCHIVED, dayStatus(archived, monday.plusDays(3), monday.plusDays(4), null))
    }
    @Test fun remindersSkipRestDaysAndKeepLocalTime() {
        val fridayAfter = monday.plusDays(4).atTime(8, 0).atZone(ZoneId.of("Asia/Jakarta"))
        val next = nextScheduledReminderAt(LocalTime.of(7, 0), fridayAfter, false, habit::isScheduledOn)
        assertEquals(monday.plusDays(7), next?.toLocalDate())
        assertEquals(LocalTime.of(7, 0), next?.toLocalTime())
    }
    @Test fun noActiveScheduleDoesNotCreateReminder() {
        assertNull(nextScheduledReminderAt(LocalTime.of(7, 0), monday.atStartOfDay(ZoneId.of("Asia/Jakarta")), false) { false })
    }
}
