package com.rezekoard.habitquest.domain

import java.time.LocalTime
import java.time.LocalDate

data class Habit(
    val id: Long,
    val name: String,
    val category: String,
    val target: Int,
    val unit: String,
    val progress: Int = 0,
    val reminderTime: LocalTime? = null,
    val startedOn: LocalDate = LocalDate.now(),
    val isArchived: Boolean = false,
    val plans: List<HabitPlan> = emptyList(),
    val configuredTarget: Int = target,
    val configuredWeekdays: Int = 127,
) {
    init { require(target > 0); require(progress in 0..target) }
    val isComplete: Boolean get() = progress >= target
    fun planOn(date: LocalDate): HabitPlan {
        val index = plans.binarySearch { it.effectiveFrom.compareTo(date) }
        return plans.getOrNull(if (index >= 0) index else -index - 2) ?: HabitPlan(startedOn, target, unit, 127)
    }
    fun isScheduledOn(date: LocalDate): Boolean = date >= startedOn && planOn(date).includes(date)
}

data class HabitPlan(val effectiveFrom: LocalDate, val target: Int, val unit: String, val weekdays: Int) {
    init { require(target > 0 && weekdays in 0..127) }
    fun includes(date: LocalDate): Boolean = weekdays and (1 shl (date.dayOfWeek.value - 1)) != 0
}

fun weekdayLabel(mask: Int): String = if (mask == 127) "Setiap hari" else if (mask == 0) "Diarsipkan" else
    listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString(" · ")

data class ProgressResult(val habit: Habit, val earnedXp: Int)

fun advanceHabit(habit: Habit, amount: Int = 1): ProgressResult {
    require(amount > 0)
    val updated = habit.copy(progress = (habit.progress.toLong() + amount).coerceAtMost(habit.target.toLong()).toInt())
    return ProgressResult(updated, if (!habit.isComplete && updated.isComplete) 20 else 0)
}
