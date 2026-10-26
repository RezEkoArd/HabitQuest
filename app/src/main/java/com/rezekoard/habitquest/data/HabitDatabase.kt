package com.rezekoard.habitquest.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "user_profile")
data class UserProfileEntity(@PrimaryKey val id: Int = 1, val displayName: String)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val target: Int,
    val unit: String,
    val reminderTime: String?,
    val startedOn: Long,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    @ColumnInfo(defaultValue = "127") val weekdays: Int = 127,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
)

@Entity(tableName = "habit_plans", primaryKeys = ["habitId", "effectiveFrom"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)])
data class HabitPlanEntity(val habitId: Long, val effectiveFrom: Long, val target: Int, val unit: String, val weekdays: Int)

@Entity(
    tableName = "daily_progress",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
)
data class DailyProgressEntity(val habitId: Long, val date: Long, val target: Int, val amount: Int, val xpEarned: Int = 0)

@Entity(
    tableName = "reminder_deliveries", primaryKeys = ["habitId", "date"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
)
data class ReminderDeliveryEntity(val habitId: Long, val date: Long)

@Entity(tableName = "habit_notes", primaryKeys = ["habitId", "date"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)])
data class HabitNoteEntity(val habitId: Long, val date: Long, val body: String)

@Entity(tableName = "reminder_snoozes",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)])
data class ReminderSnoozeEntity(@PrimaryKey val habitId: Long, val date: Long, val triggerAt: Long, val reminderTime: String)

@Dao
interface HabitDao {
    @Query("SELECT * FROM user_profile WHERE id = 1") fun observeProfile(): Flow<UserProfileEntity?>
    @Query("SELECT * FROM habits ORDER BY sortOrder, id") fun observeHabits(): Flow<List<HabitEntity>>
    @Query("SELECT * FROM daily_progress ORDER BY date, habitId") fun observeProgress(): Flow<List<DailyProgressEntity>>
    @Query("SELECT * FROM user_profile WHERE id = 1") suspend fun profile(): UserProfileEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertProfile(profile: UserProfileEntity)
    @Insert suspend fun insertHabit(habit: HabitEntity): Long
    @Query("SELECT * FROM habits WHERE id = :id") suspend fun habit(id: Long): HabitEntity?
    @Query("SELECT * FROM daily_progress WHERE habitId = :habitId AND date = :date")
    suspend fun progress(habitId: Long, date: Long): DailyProgressEntity?
    @Upsert suspend fun saveProgress(progress: DailyProgressEntity)
    @Query("SELECT * FROM habits ORDER BY sortOrder, id") suspend fun allHabits(): List<HabitEntity>
    @Query("UPDATE habits SET reminderTime = :time WHERE id = :id") suspend fun updateReminder(id: Long, time: String?)
    @Query("SELECT COUNT(*) FROM reminder_deliveries WHERE habitId = :id AND date = :date")
    suspend fun deliveryCount(id: Long, date: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun claimDelivery(delivery: ReminderDeliveryEntity): Long
    @Query("SELECT * FROM habit_plans ORDER BY habitId, effectiveFrom") fun observePlans(): Flow<List<HabitPlanEntity>>
    @Query("SELECT * FROM habit_plans ORDER BY habitId, effectiveFrom") suspend fun allPlans(): List<HabitPlanEntity>
    @Query("SELECT * FROM daily_progress ORDER BY date, habitId") suspend fun allProgress(): List<DailyProgressEntity>
    @Query("SELECT * FROM reminder_deliveries") suspend fun allDeliveries(): List<ReminderDeliveryEntity>
    @Query("SELECT * FROM habit_plans WHERE habitId = :id AND effectiveFrom <= :date ORDER BY effectiveFrom DESC LIMIT 1") suspend fun planAt(id: Long, date: Long): HabitPlanEntity?
    @Upsert suspend fun savePlan(plan: HabitPlanEntity)
    @Update suspend fun updateHabit(habit: HabitEntity)
    @Query("UPDATE user_profile SET displayName = :name WHERE id = 1") suspend fun updateProfileName(name: String)
    @Query("DELETE FROM habit_plans WHERE habitId = :id AND effectiveFrom > :date") suspend fun deleteFuturePlans(id: Long, date: Long)
    @Query("DELETE FROM reminder_deliveries") suspend fun deleteDeliveries()
    @Query("DELETE FROM daily_progress") suspend fun deleteProgress()
    @Query("DELETE FROM habit_plans") suspend fun deletePlans()
    @Query("DELETE FROM habits") suspend fun deleteHabits()
    @Query("DELETE FROM user_profile") suspend fun deleteProfile()
    @Insert suspend fun insertHabits(items: List<HabitEntity>)
    @Insert suspend fun insertPlans(items: List<HabitPlanEntity>)
    @Insert suspend fun insertProgress(items: List<DailyProgressEntity>)
    @Insert suspend fun insertDeliveries(items: List<ReminderDeliveryEntity>)
    @Query("SELECT * FROM habit_notes ORDER BY date, habitId") fun observeNotes(): Flow<List<HabitNoteEntity>>
    @Query("SELECT * FROM habit_notes ORDER BY date, habitId") suspend fun allNotes(): List<HabitNoteEntity>
    @Upsert suspend fun saveNote(note: HabitNoteEntity)
    @Query("DELETE FROM habit_notes WHERE habitId = :id AND date = :date") suspend fun deleteNote(id: Long, date: Long)
    @Query("DELETE FROM habit_notes") suspend fun deleteNotes()
    @Insert suspend fun insertNotes(items: List<HabitNoteEntity>)
    @Query("SELECT * FROM reminder_snoozes WHERE habitId = :id") suspend fun snooze(id: Long): ReminderSnoozeEntity?
    @Upsert suspend fun saveSnooze(snooze: ReminderSnoozeEntity)
    @Query("DELETE FROM reminder_snoozes WHERE habitId = :id") suspend fun deleteSnooze(id: Long)
    @Query("DELETE FROM reminder_snoozes") suspend fun deleteSnoozes()
}

@Database(entities = [UserProfileEntity::class, HabitEntity::class, DailyProgressEntity::class, ReminderDeliveryEntity::class, HabitPlanEntity::class, HabitNoteEntity::class, ReminderSnoozeEntity::class], version = 4, exportSchema = true)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS habit_notes (habitId INTEGER NOT NULL, date INTEGER NOT NULL, body TEXT NOT NULL, PRIMARY KEY(habitId, date), FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE TABLE IF NOT EXISTS reminder_snoozes (habitId INTEGER NOT NULL PRIMARY KEY, date INTEGER NOT NULL, triggerAt INTEGER NOT NULL, reminderTime TEXT NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN weekdays INTEGER NOT NULL DEFAULT 127")
                db.execSQL("CREATE TABLE IF NOT EXISTS habit_plans (habitId INTEGER NOT NULL, effectiveFrom INTEGER NOT NULL, target INTEGER NOT NULL, unit TEXT NOT NULL, weekdays INTEGER NOT NULL, PRIMARY KEY(habitId, effectiveFrom), FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("INSERT INTO habit_plans (habitId, effectiveFrom, target, unit, weekdays) SELECT id, startedOn, target, unit, 127 FROM habits")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_deliveries` (`habitId` INTEGER NOT NULL, `date` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `date`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            }
        }
        @Volatile private var instance: HabitDatabase? = null
        fun getInstance(context: Context): HabitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HabitDatabase::class.java, "habitquest.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }
    }
}
