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
) {
    init { require(target > 0); require(progress in 0..target) }
    val isComplete: Boolean get() = progress >= target
}

data class ProgressResult(val habit: Habit, val earnedXp: Int)

fun advanceHabit(habit: Habit, amount: Int = 1): ProgressResult {
    require(amount > 0)
    val updated = habit.copy(progress = (habit.progress.toLong() + amount).coerceAtMost(habit.target.toLong()).toInt())
    return ProgressResult(updated, if (!habit.isComplete && updated.isComplete) 20 else 0)
}
