package com.rezekoard.habitquest.ui

import android.app.Application
import android.util.Log
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rezekoard.habitquest.data.HabitDatabase
import com.rezekoard.habitquest.data.HabitRepository
import com.rezekoard.habitquest.domain.DailyHabitProgress
import com.rezekoard.habitquest.domain.Habit
import com.rezekoard.habitquest.domain.HabitPlan
import com.rezekoard.habitquest.domain.HabitNote
import com.rezekoard.habitquest.data.BackupCodec
import com.rezekoard.habitquest.data.BackupSummary
import com.rezekoard.habitquest.data.StoredHabitData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.ByteArrayOutputStream
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
    val notes: List<HabitNote> = emptyList(),
) {
    val level: Int get() = xp / 100 + 1
    val completedCount: Int get() = habits.count { it.isComplete }
    val todayHabits: List<Habit> get() = habits.filter { !it.isArchived && it.isScheduledOn(date) }
    val activeHabits: List<Habit> get() = habits.filter { !it.isArchived }
    val initial: String get() = userName.trim().takeIf { it.isNotEmpty() }
        ?.let { String(Character.toChars(it.codePointAt(0))).uppercase() } ?: "?"

    fun recordsFor(habit: Habit): List<DailyHabitProgress> =
        history.filter { it.habitId == habit.id && it.date != date } + DailyHabitProgress(habit.id, date, habit.target, habit.progress)
}

private fun InputStream.readBytesUpTo(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (output.size() < limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit - output.size()))
        if (count == -1) break
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HabitRepository(HabitDatabase.getInstance(application))
    private val reminders = HabitReminderManager(application)
    private val dates = MutableStateFlow(LocalDate.now())
    private val mutableState = MutableStateFlow(HabitUiState(isLoading = true))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var pendingRestore: StoredHabitData? = null

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
                    val plans = stored.plans.groupBy { it.habitId }
                    HabitUiState(
                        date = today,
                        userName = stored.profile?.displayName.orEmpty(),
                        habits = stored.habits.map { h ->
                            val versions = plans[h.id].orEmpty().map { HabitPlan(LocalDate.ofEpochDay(it.effectiveFrom), it.target, it.unit, it.weekdays) }
                            val plan = versions.lastOrNull { it.effectiveFrom <= today }
                            val target = current[h.id]?.target ?: plan?.target ?: h.target
                            Habit(h.id, h.name, h.category, target, h.unit, (current[h.id]?.amount ?: 0).coerceAtMost(target), h.reminderTime?.let(LocalTime::parse), LocalDate.ofEpochDay(h.startedOn), h.archived, versions, h.target, h.weekdays)
                        },
                        history = stored.progress.map { DailyHabitProgress(it.habitId, LocalDate.ofEpochDay(it.date), it.target, it.amount) },
                        xp = stored.progress.sumOf { it.xpEarned },
                        notes = stored.notes.map { HabitNote(it.habitId, LocalDate.ofEpochDay(it.date), it.body) },
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
    suspend fun addHabit(name: String, category: String, target: Int, unit: String, reminderTime: LocalTime?, weekdays: Int) {
        val id = repository.addHabit(name, category, target, unit, reminderTime, weekdays)
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
    suspend fun editHabit(id: Long, name: String, category: String, target: Int, weekdays: Int) {
        repository.editHabit(id, name, category, target, weekdays)
        updateSchedule { reminders.reminderChanged(id) }
    }
    suspend fun setArchived(id: Long, archived: Boolean) {
        repository.setArchived(id, archived)
        updateSchedule { reminders.reminderChanged(id) }
    }
    suspend fun correctProgress(id: Long, date: LocalDate, amount: Int): Int {
        val delta = repository.correctProgress(id, date, amount)
        updateSchedule { reminders.reconcileHabit(id) }
        return delta
    }
    suspend fun editProfileName(name: String) = repository.editProfileName(name)
    suspend fun saveNote(id: Long, date: LocalDate, body: String) = repository.saveNote(id, date, body)
    suspend fun moveHabit(id: Long, direction: Int) = repository.moveHabit(id, direction)

    suspend fun exportBackup(uri: Uri) = withContext(Dispatchers.IO) {
        val bytes = BackupCodec.encode(repository.snapshot()).toByteArray(Charsets.UTF_8)
        checkNotNull(getApplication<Application>().contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
    }
    suspend fun prepareRestore(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        pendingRestore = null
        val bytes = checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)).use { it.readBytesUpTo(BackupCodec.MAX_BYTES + 1) }
        require(bytes.size <= BackupCodec.MAX_BYTES) { "File backup melebihi 10 MB." }
        val data = BackupCodec.decode(bytes.toString(Charsets.UTF_8))
        pendingRestore = data
        BackupCodec.summary(data)
    }
    suspend fun restoreBackup() {
        val data = checkNotNull(pendingRestore)
        val previous = repository.snapshot()
        updateSchedule { reminders.cancelAll(previous.habits.map { it.id }) }
        try { repository.restore(data); pendingRestore = null; reload() }
        finally { syncReminders() }
    }
    private suspend fun updateSchedule(block: suspend () -> Unit) {
        try { block() }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { Log.e("HabitReminder", "Data tersimpan; penjadwalan akan dicoba lagi saat aplikasi dibuka", error) }
    }
}
