package com.rezekoard.habitquest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rezekoard.habitquest.domain.Habit
import com.rezekoard.habitquest.domain.DailyHabitProgress
import com.rezekoard.habitquest.domain.demoHistory
import com.rezekoard.habitquest.domain.advanceHabit
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HabitUiState(
    val date: LocalDate = LocalDate.now(),
    val habits: List<Habit> = listOf(
        Habit(1, "Workout pagi", "Olahraga", 30, "menit", 30, LocalTime.of(7, 0), date.withDayOfMonth(1)),
        Habit(2, "Baca buku", "Belajar", 15, "halaman", 10, LocalTime.of(20, 0), date.withDayOfMonth(1)),
        Habit(3, "Buat konten", "Kreativitas", 1, "konten", 0, LocalTime.of(16, 0), date.withDayOfMonth(1)),
    ),
    val xp: Int = 460,
    val history: List<DailyHabitProgress> = demoHistory(date, habits),
) {
    val level: Int get() = xp / 100 + 1
    val completedCount: Int get() = habits.count { it.isComplete }

    fun recordsFor(habit: Habit): List<DailyHabitProgress> =
        history.filter { it.habitId == habit.id && it.date != date } +
            DailyHabitProgress(habit.id, date, habit.target, habit.progress)
}

/** Session-only demo data. Daily progress and XP will be persisted with Room later. */
class HabitViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(HabitUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                refreshDate()
                delay(30_000)
            }
        }
    }

    private fun refreshDate() {
        val current = mutableState.value
        val today = LocalDate.now()
        if (current.date != today) {
            val archived = current.history.filter { it.date != current.date } + current.habits.map {
                DailyHabitProgress(it.id, current.date, it.target, it.progress)
            }
            mutableState.value = current.copy(
                date = today,
                history = archived,
                habits = current.habits.map { habit ->
                    val existing = archived.lastOrNull { it.habitId == habit.id && it.date == today }
                    habit.copy(progress = (existing?.amount ?: 0).coerceAtMost(habit.target))
                },
            )
        }
    }

    fun advance(id: Long, amount: Int): Int {
        require(amount > 0)
        refreshDate()
        val current = mutableState.value
        val habit = current.habits.find { it.id == id } ?: return 0
        val result = advanceHabit(habit, amount)
        mutableState.value = current.copy(
            habits = current.habits.map { if (it.id == id) result.habit else it },
            xp = current.xp + result.earnedXp,
        )
        return result.earnedXp
    }

    fun addHabit(name: String, category: String, target: Int, unit: String, reminderTime: LocalTime?) {
        require(name.isNotBlank() && unit.isNotBlank() && target in 1..10_000)
        refreshDate()
        val current = mutableState.value
        val habit = Habit(
            id = (current.habits.maxOfOrNull { it.id } ?: 0) + 1,
            name = name.trim(), category = category, target = target, unit = unit.trim(), reminderTime = reminderTime,
            startedOn = current.date,
        )
        mutableState.value = current.copy(habits = current.habits + habit)
    }
}
