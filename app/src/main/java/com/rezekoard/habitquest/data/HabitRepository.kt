package com.rezekoard.habitquest.data

import androidx.room.withTransaction
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class StoredHabitData(val profile: UserProfileEntity?, val habits: List<HabitEntity>, val progress: List<DailyProgressEntity>)

class HabitRepository(private val database: HabitDatabase, private val today: () -> LocalDate = { LocalDate.now() }) {
    private val dao = database.habitDao()

    fun observe(): Flow<StoredHabitData> = combine(dao.observeProfile(), dao.observeHabits(), dao.observeProgress()) { profile, habits, progress ->
        StoredHabitData(profile, habits, progress)
    }

    suspend fun completeOnboarding(name: String) {
        val normalized = name.trim().replace(Regex("\\s+"), " ")
        require(normalized.isNotBlank() && normalized.length <= 30)
        dao.insertProfile(UserProfileEntity(displayName = normalized))
    }

    suspend fun updateReminder(id: Long, time: LocalTime?) {
        checkNotNull(dao.habit(id)) { "Habit tidak ditemukan." }
        dao.updateReminder(id, time?.toString())
    }

    suspend fun addHabit(name: String, category: String, target: Int, unit: String, reminderTime: LocalTime?): Long {
        require(name.isNotBlank() && name.trim().length <= 60 && unit.isNotBlank() && target in 1..10_000)
        return database.withTransaction {
            check(dao.profile() != null) { "Isi nama terlebih dahulu." }
            dao.insertHabit(HabitEntity(name = name.trim(), category = category, target = target, unit = unit.trim(), reminderTime = reminderTime?.toString(), startedOn = today().toEpochDay()))
        }
    }

    /** Progress and XP are committed together; the habit/date primary key prevents duplicate rewards. */
    suspend fun advance(id: Long, amount: Int): Int = database.withTransaction {
        require(amount in 1..10_000)
        val habit = checkNotNull(dao.habit(id)) { "Habit tidak ditemukan." }
        val date = today().toEpochDay()
        check(date >= habit.startedOn) { "Tanggal perangkat mendahului tanggal pembuatan habit." }
        val previous = dao.progress(id, date)
        val target = previous?.target ?: habit.target
        val updated = ((previous?.amount ?: 0).toLong() + amount).coerceAtMost(target.toLong()).toInt()
        val earned = if (updated >= target && (previous?.xpEarned ?: 0) == 0) 20 else 0
        dao.saveProgress(DailyProgressEntity(id, date, target, updated, (previous?.xpEarned ?: 0) + earned))
        earned
    }
}
