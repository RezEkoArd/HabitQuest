package com.rezekoard.habitquest.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rezekoard.habitquest.data.HabitDatabase
import com.rezekoard.habitquest.data.HabitRepository
import com.rezekoard.habitquest.domain.DailyHabitProgress
import com.rezekoard.habitquest.domain.Habit
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.rezekoard.habitquest.notifications.HabitReminderManager

data class HabitUiState(
    val date: LocalDate = LocalDate.now(),
    val habits: List<Habit> = emptyList(),
    val xp: Int = 0,
    val history: List<DailyHabitProgress> = emptyList(),
    val userName: String = "",
    val isLoading: Boolean = false,
    val storageError: String? = null,
) {
    val level: Int get() = xp / 100 + 1
    val completedCount: Int get() = habits.count { it.isComplete }
    val initial: String get() = userName.trim().takeIf { it.isNotEmpty() }
        ?.let { String(Character.toChars(it.codePointAt(0))).uppercase() } ?: "?"

    fun recordsFor(habit: Habit): List<DailyHabitProgress> =
        history.filter { it.habitId == habit.id && it.date != date } + DailyHabitProgress(habit.id, date, habit.target, habit.progress)
}

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HabitRepository(HabitDatabase.getInstance(application))
    private val reminders = HabitReminderManager(application)
    private val dates = MutableStateFlow(LocalDate.now())
    private val mutableState = MutableStateFlow(HabitUiState(isLoading = true))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init {
        reload()
        viewModelScope.launch { while (true) { dates.value = LocalDate.now(); delay(30_000) } }
    }

    fun reload() {
        observation?.cancel()
        mutableState.value = HabitUiState(isLoading = true)
        observation = viewModelScope.launch {
            try {
                repository.observe().combine(dates) { stored, today ->
                    val current = stored.progress.filter { it.date == today.toEpochDay() }.associateBy { it.habitId }
                    HabitUiState(
                        date = today,
                        userName = stored.profile?.displayName.orEmpty(),
                        habits = stored.habits.map { h ->
                            Habit(h.id, h.name, h.category, h.target, h.unit, (current[h.id]?.amount ?: 0).coerceAtMost(h.target), h.reminderTime?.let(LocalTime::parse), LocalDate.ofEpochDay(h.startedOn))
                        },
                        history = stored.progress.map { DailyHabitProgress(it.habitId, LocalDate.ofEpochDay(it.date), it.target, it.amount) },
                        xp = stored.progress.sumOf { it.xpEarned },
                    )
                }.collect { mutableState.value = it }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = HabitUiState(storageError = "Data lokal belum bisa dibuka. Coba lagi tanpa menghapus data aplikasi.")
            }
        }
    }

    suspend fun completeOnboarding(name: String) = repository.completeOnboarding(name)
    suspend fun advance(id: Long, amount: Int): Int {
        val xp = repository.advance(id, amount)
        updateSchedule { reminders.reconcileHabit(id) }
        return xp
    }
    suspend fun addHabit(name: String, category: String, target: Int, unit: String, reminderTime: LocalTime?) {
        val id = repository.addHabit(name, category, target, unit, reminderTime)
        updateSchedule { reminders.reconcileHabit(id) }
    }
    suspend fun updateReminder(id: Long, time: LocalTime?) {
        repository.updateReminder(id, time)
        updateSchedule { reminders.reminderChanged(id) }
    }
    suspend fun syncReminders() {
        dates.value = LocalDate.now()
        updateSchedule { reminders.reconcileAll() }
    }
    private suspend fun updateSchedule(block: suspend () -> Unit) {
        try { block() }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { Log.e("HabitReminder", "Data tersimpan; penjadwalan akan dicoba lagi saat aplikasi dibuka", error) }
    }
}
