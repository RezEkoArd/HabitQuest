package com.rezekoard.habitquest.domain

import java.time.LocalDate
import java.time.YearMonth

data class ProfileSummary(
    val activeDaysThisMonth: Int,
    val longestHabitStreak: Int,
    val habitCount: Int,
    val completedHabitDays: Int,
    val mostCompletedDaysForOneHabit: Int,
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val unit: String,
) {
    val isUnlocked: Boolean get() = progress >= target
    val displayedProgress: Int get() = progress.coerceAtMost(target)
}

fun summarizeProfile(habits: List<Habit>, records: List<DailyHabitProgress>, today: LocalDate): ProfileSummary {
    val byId = habits.associateBy { it.id }
    val validRecords = records.filter { record ->
        val habit = byId[record.habitId]
        habit != null && !record.date.isBefore(habit.startedOn) && !record.date.isAfter(today)
    }.associateBy { it.habitId to it.date }.values.toList()
    val completed = validRecords.filter { it.isComplete }
    return ProfileSummary(
        activeDaysThisMonth = validRecords.filter { it.amount > 0 && YearMonth.from(it.date) == YearMonth.from(today) }
            .map { it.date }.distinct().size,
        longestHabitStreak = habits.maxOfOrNull { bestStreak(it, validRecords, today) } ?: 0,
        habitCount = habits.count { !it.isArchived },
        completedHabitDays = completed.size,
        mostCompletedDaysForOneHabit = completed.groupingBy { it.habitId }.eachCount().values.maxOrNull() ?: 0,
    )
}

fun achievementsFor(summary: ProfileSummary): List<Achievement> = listOf(
    Achievement("first", "Langkah awal", "Selesaikan target harian pertamamu pada satu habit.", summary.completedHabitDays, 1, "target"),
    Achievement("week", "7 hari konsisten", "Selesaikan target pada habit yang sama selama 7 hari terjadwal berturut-turut. Hari istirahat tidak memutus streak.", summary.longestHabitStreak, 7, "hari terjadwal"),
    Achievement("month", "30 hari tumbuh", "Selesaikan target pada habit yang sama di 30 hari. Tidak harus berturut-turut.", summary.mostCompletedDaysForOneHabit, 30, "hari selesai"),
)
