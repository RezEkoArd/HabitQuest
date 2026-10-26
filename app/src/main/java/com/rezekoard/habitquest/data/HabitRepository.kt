package com.rezekoard.habitquest.data

import androidx.room.withTransaction
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

data class StoredHabitData(val profile: UserProfileEntity?, val habits: List<HabitEntity>, val progress: List<DailyProgressEntity>, val plans: List<HabitPlanEntity> = emptyList(), val deliveries: List<ReminderDeliveryEntity> = emptyList(), val notes: List<HabitNoteEntity> = emptyList())

class HabitRepository(private val database: HabitDatabase, private val today: () -> LocalDate = { LocalDate.now() }) {
    private val dao = database.habitDao()

    fun observe(): Flow<StoredHabitData> = combine(dao.observeProfile(), dao.observeHabits(), dao.observeProgress(), dao.observePlans(), dao.observeNotes()) { _, _, _, _, _ -> Unit }
        .map { snapshot() }.distinctUntilChanged()

    suspend fun snapshot(): StoredHabitData = database.withTransaction {
        StoredHabitData(dao.profile(), dao.allHabits(), dao.allProgress(), dao.allPlans(), dao.allDeliveries(), dao.allNotes())
    }

    suspend fun editProfileName(name: String) {
        val normalized = name.trim().replace(Regex("\\s+"), " ")
        require(normalized.isNotBlank() && normalized.length <= 30)
        check(dao.profile() != null)
        dao.updateProfileName(normalized)
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

    suspend fun addHabit(name: String, category: String, target: Int, unit: String, reminderTime: LocalTime?, weekdays: Int = 127): Long {
        require(name.isNotBlank() && name.trim().length <= 60 && unit.isNotBlank() && unit.length <= 20 && target in 1..10_000 && weekdays in 1..127)
        return database.withTransaction {
            check(dao.profile() != null) { "Isi nama terlebih dahulu." }
            val date = today().toEpochDay()
            val id = dao.insertHabit(HabitEntity(name = name.trim(), category = category, target = target, unit = unit.trim(), reminderTime = reminderTime?.toString(), startedOn = date, weekdays = weekdays, sortOrder = (dao.allHabits().maxOfOrNull { it.sortOrder } ?: -1) + 1))
            dao.savePlan(HabitPlanEntity(id, date, target, unit.trim(), weekdays))
            id
        }
    }

    /** Progress and XP are committed together; the habit/date primary key prevents duplicate rewards. */
    suspend fun advance(id: Long, amount: Int): Int = database.withTransaction {
        require(amount in 1..10_000)
        val habit = checkNotNull(dao.habit(id)) { "Habit tidak ditemukan." }
        val date = today().toEpochDay()
        val plan = checkNotNull(dao.planAt(id, date))
        check(!habit.archived && plan.weekdays and (1 shl (today().dayOfWeek.value - 1)) != 0) { "Habit tidak dijadwalkan hari ini." }
        check(date >= habit.startedOn) { "Tanggal perangkat mendahului tanggal pembuatan habit." }
        val previous = dao.progress(id, date)
        val target = previous?.target ?: plan.target
        val updated = ((previous?.amount ?: 0).toLong() + amount).coerceAtMost(target.toLong()).toInt()
        val earned = if (updated >= target && (previous?.xpEarned ?: 0) == 0) 20 else 0
        dao.saveProgress(DailyProgressEntity(id, date, target, updated, (previous?.xpEarned ?: 0) + earned))
        earned
    }

    suspend fun editHabit(id: Long, name: String, category: String, target: Int, weekdays: Int) = database.withTransaction {
        require(name.isNotBlank() && name.trim().length <= 60 && target in 1..10_000 && weekdays in 1..127)
        val habit = checkNotNull(dao.habit(id))
        dao.updateHabit(habit.copy(name = name.trim(), category = category, target = target, weekdays = weekdays))
        if (!habit.archived) dao.savePlan(HabitPlanEntity(id, today().plusDays(1).toEpochDay(), target, habit.unit, weekdays))
    }

    suspend fun setArchived(id: Long, archived: Boolean) = database.withTransaction {
        val habit = checkNotNull(dao.habit(id))
        if (habit.archived == archived) return@withTransaction
        val date = today().toEpochDay()
        val plan = checkNotNull(dao.planAt(id, date))
        dao.updateHabit(habit.copy(archived = archived))
        if (archived) {
            dao.deleteFuturePlans(id, date)
            dao.savePlan(plan.copy(effectiveFrom = date, weekdays = 0))
        } else {
            dao.savePlan(HabitPlanEntity(id, date + 1, habit.target, habit.unit, habit.weekdays))
        }
    }

    suspend fun correctProgress(id: Long, date: LocalDate, amount: Int): Int = database.withTransaction {
        val habit = checkNotNull(dao.habit(id))
        require(date <= today() && date.toEpochDay() >= habit.startedOn)
        val previous = dao.progress(id, date.toEpochDay())
        val plan = checkNotNull(dao.planAt(id, date.toEpochDay()))
        check(previous != null || plan.weekdays and (1 shl (date.dayOfWeek.value - 1)) != 0)
        val target = previous?.target ?: plan.target
        require(amount in 0..target)
        val xp = if (amount == target) 20 else 0
        dao.saveProgress(DailyProgressEntity(id, date.toEpochDay(), target, amount, xp))
        xp - (previous?.xpEarned ?: 0)
    }

    suspend fun saveNote(id: Long, date: LocalDate, body: String) = database.withTransaction {
        val habit = checkNotNull(dao.habit(id))
        require(date <= today() && date.toEpochDay() >= habit.startedOn && body.length <= 2_000)
        val text = body.trim()
        if (text.isEmpty()) dao.deleteNote(id, date.toEpochDay())
        else dao.saveNote(HabitNoteEntity(id, date.toEpochDay(), text))
    }

    /** Move within active habits; normalize all positions to keep ordering bounded and deterministic. */
    suspend fun moveHabit(id: Long, direction: Int) = database.withTransaction {
        require(direction == -1 || direction == 1)
        val all = dao.allHabits().toMutableList()
        val active = all.filter { !it.archived }
        val index = active.indexOfFirst { it.id == id }
        check(index >= 0)
        val neighbor = active.getOrNull(index + direction) ?: return@withTransaction
        val from = all.indexOfFirst { it.id == id }
        val to = all.indexOfFirst { it.id == neighbor.id }
        val temporary = all[from]; all[from] = all[to]; all[to] = temporary
        all.forEachIndexed { position, habit -> dao.updateHabit(habit.copy(sortOrder = position)) }
    }

    suspend fun restore(data: StoredHabitData) {
        BackupCodec.validate(data)
        database.withTransaction {
            dao.deleteSnoozes(); dao.deleteNotes(); dao.deleteDeliveries(); dao.deleteProgress(); dao.deletePlans(); dao.deleteHabits(); dao.deleteProfile()
            dao.insertProfile(checkNotNull(data.profile))
            dao.insertHabits(data.habits); dao.insertPlans(data.plans); dao.insertProgress(data.progress); dao.insertDeliveries(data.deliveries)
            dao.insertNotes(data.notes)
        }
    }
}
