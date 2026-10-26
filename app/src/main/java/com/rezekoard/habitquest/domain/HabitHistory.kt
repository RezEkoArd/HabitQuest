package com.rezekoard.habitquest.domain

import java.time.LocalDate
import java.time.YearMonth

/** The target is captured with the record so past results do not change with a new target. */
data class DailyHabitProgress(val habitId: Long, val date: LocalDate, val target: Int, val amount: Int) {
    init {
        require(target > 0)
        require(amount in 0..target)
    }
    val isComplete: Boolean get() = amount >= target
}

enum class DayStatus(val label: String) {
    COMPLETE("Selesai"), PARTIAL("Sebagian"), MISSED("Terlewat"),
    TODAY_EMPTY("Belum dikerjakan"), FUTURE("Belum tiba"), NOT_STARTED("Belum dimulai"),
    REST("Tidak dijadwalkan"), ARCHIVED("Diarsipkan"),
}

data class MonthlyHabitSummary(val completedDays: Int, val scheduledDays: Int, val totalAmount: Long) {
    val completionPercent: Int get() = if (scheduledDays == 0) 0 else completedDays * 100 / scheduledDays
}

fun dayStatus(habit: Habit, date: LocalDate, today: LocalDate, record: DailyHabitProgress?): DayStatus = when {
    date.isBefore(habit.startedOn) -> DayStatus.NOT_STARTED
    date.isAfter(today) -> if (habit.planOn(date).weekdays == 0) DayStatus.ARCHIVED else if (!habit.isScheduledOn(date)) DayStatus.REST else DayStatus.FUTURE
    record?.isComplete == true -> DayStatus.COMPLETE
    (record?.amount ?: 0) > 0 -> DayStatus.PARTIAL
    habit.planOn(date).weekdays == 0 -> DayStatus.ARCHIVED
    !habit.isScheduledOn(date) -> DayStatus.REST
    date == today -> DayStatus.TODAY_EMPTY
    else -> DayStatus.MISSED
}

fun summarizeMonth(habit: Habit, records: List<DailyHabitProgress>, month: YearMonth, today: LocalDate): MonthlyHabitSummary {
    val start = maxOf(month.atDay(1), habit.startedOn)
    val end = minOf(month.atEndOfMonth(), today)
    if (start.isAfter(end)) return MonthlyHabitSummary(0, 0, 0)
    val days = records.filter { it.habitId == habit.id && !it.date.isBefore(start) && !it.date.isAfter(end) }.associateBy { it.date }
    var scheduled = 0
    var date = start
    while (date <= end) {
        if (habit.isScheduledOn(date) || (days[date]?.amount ?: 0) > 0) scheduled++
        date = date.plusDays(1)
    }
    return MonthlyHabitSummary(
        completedDays = days.values.count { it.isComplete },
        scheduledDays = scheduled,
        totalAmount = days.values.sumOf { it.amount.toLong() },
    )
}

fun bestStreak(habit: Habit, records: List<DailyHabitProgress>, today: LocalDate): Int {
    val byDate = records.filter { it.habitId == habit.id }.associateBy { it.date }
    var streak = 0
    var longest = 0
    var date = habit.startedOn
    while (date <= today) {
        val record = byDate[date]
        if (habit.isScheduledOn(date) || (record?.amount ?: 0) > 0) {
            streak = if (record?.isComplete == true) streak + 1 else 0
            longest = maxOf(longest, streak)
        }
        date = date.plusDays(1)
    }
    return longest
}
