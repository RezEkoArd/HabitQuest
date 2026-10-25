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
}

data class MonthlyHabitSummary(val completedDays: Int, val scheduledDays: Int, val totalAmount: Long) {
    val completionPercent: Int get() = if (scheduledDays == 0) 0 else completedDays * 100 / scheduledDays
}

fun dayStatus(habit: Habit, date: LocalDate, today: LocalDate, record: DailyHabitProgress?): DayStatus = when {
    date.isAfter(today) -> DayStatus.FUTURE
    date.isBefore(habit.startedOn) -> DayStatus.NOT_STARTED
    record?.isComplete == true -> DayStatus.COMPLETE
    (record?.amount ?: 0) > 0 -> DayStatus.PARTIAL
    date == today -> DayStatus.TODAY_EMPTY
    else -> DayStatus.MISSED
}

fun summarizeMonth(habit: Habit, records: List<DailyHabitProgress>, month: YearMonth, today: LocalDate): MonthlyHabitSummary {
    val start = maxOf(month.atDay(1), habit.startedOn)
    val end = minOf(month.atEndOfMonth(), today)
    if (start.isAfter(end)) return MonthlyHabitSummary(0, 0, 0)
    val days = records.filter { it.habitId == habit.id && !it.date.isBefore(start) && !it.date.isAfter(end) }
        .associateBy { it.date }.values
    return MonthlyHabitSummary(
        completedDays = days.count { it.isComplete },
        scheduledDays = (end.toEpochDay() - start.toEpochDay() + 1).toInt(),
        totalAmount = days.sumOf { it.amount.toLong() },
    )
}

fun bestStreak(habit: Habit, records: List<DailyHabitProgress>, today: LocalDate): Int {
    val completed = records.filter {
        it.habitId == habit.id && it.isComplete && !it.date.isBefore(habit.startedOn) && !it.date.isAfter(today)
    }.map { it.date }.distinct().sorted()
    var previous: LocalDate? = null
    var streak = 0
    var longest = 0
    completed.forEach { date ->
        streak = if (previous?.plusDays(1) == date) streak + 1 else 1
        longest = maxOf(longest, streak)
        previous = date
    }
    return longest
}

/** Explicit sample history for the three starter habits; never called for user-created habits. */
fun demoHistory(today: LocalDate, habits: List<Habit>): List<DailyHabitProgress> = buildList {
    habits.forEach { habit ->
        var date = habit.startedOn
        while (date.isBefore(today)) {
            val missed = when (habit.id) {
                1L -> date.dayOfMonth % 6 == 0
                2L -> date.dayOfMonth in listOf(5, 11, 17, 23)
                else -> date.dayOfMonth % 3 == 0
            }
            add(DailyHabitProgress(habit.id, date, habit.target, if (missed) 0 else habit.target))
            date = date.plusDays(1)
        }
    }
}
