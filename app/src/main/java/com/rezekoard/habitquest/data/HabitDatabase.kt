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
)

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

@Dao
interface HabitDao {
    @Query("SELECT * FROM user_profile WHERE id = 1") fun observeProfile(): Flow<UserProfileEntity?>
    @Query("SELECT * FROM habits ORDER BY id") fun observeHabits(): Flow<List<HabitEntity>>
    @Query("SELECT * FROM daily_progress ORDER BY date, habitId") fun observeProgress(): Flow<List<DailyProgressEntity>>
    @Query("SELECT * FROM user_profile WHERE id = 1") suspend fun profile(): UserProfileEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertProfile(profile: UserProfileEntity)
    @Insert suspend fun insertHabit(habit: HabitEntity): Long
    @Query("SELECT * FROM habits WHERE id = :id") suspend fun habit(id: Long): HabitEntity?
    @Query("SELECT * FROM daily_progress WHERE habitId = :habitId AND date = :date")
    suspend fun progress(habitId: Long, date: Long): DailyProgressEntity?
    @Upsert suspend fun saveProgress(progress: DailyProgressEntity)
    @Query("SELECT * FROM habits ORDER BY id") suspend fun allHabits(): List<HabitEntity>
    @Query("UPDATE habits SET reminderTime = :time WHERE id = :id") suspend fun updateReminder(id: Long, time: String?)
    @Query("SELECT COUNT(*) FROM reminder_deliveries WHERE habitId = :id AND date = :date")
    suspend fun deliveryCount(id: Long, date: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun claimDelivery(delivery: ReminderDeliveryEntity): Long
}

@Database(entities = [UserProfileEntity::class, HabitEntity::class, DailyProgressEntity::class, ReminderDeliveryEntity::class], version = 2, exportSchema = true)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_deliveries` (`habitId` INTEGER NOT NULL, `date` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `date`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            }
        }
        @Volatile private var instance: HabitDatabase? = null
        fun getInstance(context: Context): HabitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HabitDatabase::class.java, "habitquest.db")
                .addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
