package com.rezekoard.habitquest.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class HabitNote(val habitId: Long, val date: LocalDate, val body: String)

data class PeriodSummary(val completed: Int, val scheduled: Int, val activeDays: Int, val xp: Int) {
    val percent: Int get() = if (scheduled == 0) 0 else completed * 100 / scheduled
}

/** Only elapsed dates count. Recorded activity is retained even if the habit was archived that day. */
fun summarizePeriod(habits: List<Habit>, records: List<DailyHabitProgress>, start: LocalDate, end: LocalDate, today: LocalDate): PeriodSummary {
    val until = minOf(end, today)
    if (start > until) return PeriodSummary(0, 0, 0, 0)
    val byHabit = records.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.date } }
    val active = mutableSetOf<LocalDate>()
    var scheduled = 0
    var complete = 0
    habits.forEach { habit ->
        var date = maxOf(start, habit.startedOn)
        while (date <= until) {
            val record = byHabit[habit.id]?.get(date)
            if (habit.isScheduledOn(date) || (record?.amount ?: 0) > 0) scheduled++
            if (record?.isComplete == true) complete++
            if ((record?.amount ?: 0) > 0) active += date
            date = date.plusDays(1)
        }
    }
    return PeriodSummary(complete, scheduled, active.size, complete * 20)
}

data class PeriodComparison(val start: LocalDate, val end: LocalDate, val current: PeriodSummary, val previous: PeriodSummary) {
    val difference: Int? get() = if (current.scheduled == 0 || previous.scheduled == 0) null else current.percent - previous.percent
}

fun comparePeriods(habits: List<Habit>, records: List<DailyHabitProgress>, today: LocalDate, monthly: Boolean, offset: Int): PeriodComparison {
    require(offset in 0..120)
    val start = if (monthly) YearMonth.from(today).minusMonths(offset.toLong()).atDay(1)
        else today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset.toLong())
    val end = if (monthly) YearMonth.from(start).atEndOfMonth() else start.plusDays(6)
    val previousStart = if (monthly) start.minusMonths(1) else start.minusWeeks(1)
    val previousEnd = if (monthly) YearMonth.from(previousStart).atEndOfMonth() else previousStart.plusDays(6)
    // Current, unfinished periods compare the same number of elapsed days.
    val comparisonEnd = if (today < end) minOf(previousEnd, previousStart.plusDays(java.time.temporal.ChronoUnit.DAYS.between(start, today))) else previousEnd
    return PeriodComparison(start, end, summarizePeriod(habits, records, start, end, today),
        summarizePeriod(habits, records, previousStart, comparisonEnd, today))
}
