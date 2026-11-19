package com.example.ironlog.wear.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "wear_exercises")
data class WearExerciseEntity(
  @PrimaryKey val id: String,
  val jsonPayload: String,
  val updatedAt: Long,
)

@Entity(tableName = "wear_routines")
data class WearRoutineEntity(
  @PrimaryKey val id: String,
  val jsonPayload: String,
  val sortOrder: Int,
  val updatedAt: Long,
  val isDeleted: Boolean,
)

@Entity(tableName = "wear_sessions")
data class WearSessionEntity(
  @PrimaryKey val id: String,
  val isActive: Boolean,
  val startedAtEpochMs: Long,
  val jsonPayload: String,
  val updatedAt: Long,
  val pendingSync: Boolean,
  val isDeleted: Boolean,
)

@Dao
interface WearDao {
  @Query("SELECT * FROM wear_exercises")
  fun observeExercises(): Flow<List<WearExerciseEntity>>

  @Query("SELECT * FROM wear_exercises")
  suspend fun getExercisesOnce(): List<WearExerciseEntity>

  @Query("SELECT COUNT(*) FROM wear_exercises")
  suspend fun countExercises(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExercises(items: List<WearExerciseEntity>)

  @Query("SELECT * FROM wear_routines WHERE isDeleted = 0 ORDER BY sortOrder ASC")
  fun observeRoutines(): Flow<List<WearRoutineEntity>>

  @Query("SELECT * FROM wear_routines")
  suspend fun getRoutinesOnce(): List<WearRoutineEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRoutine(item: WearRoutineEntity)

  @Query("SELECT * FROM wear_sessions WHERE isActive = 1 AND isDeleted = 0 ORDER BY startedAtEpochMs DESC LIMIT 1")
  fun observeActiveSession(): Flow<WearSessionEntity?>

  @Query("SELECT * FROM wear_sessions WHERE isActive = 0 AND isDeleted = 0 ORDER BY startedAtEpochMs DESC")
  fun observeCompletedSessions(): Flow<List<WearSessionEntity>>

  @Query("SELECT * FROM wear_sessions WHERE pendingSync = 1 AND isActive = 0")
  suspend fun getPendingSyncSessions(): List<WearSessionEntity>

  @Query("SELECT * FROM wear_sessions")
  suspend fun getAllSessionsOnce(): List<WearSessionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSession(item: WearSessionEntity)

  @Query("UPDATE wear_sessions SET pendingSync = 0 WHERE id IN (:ids)")
  suspend fun markSessionsSynced(ids: List<String>)
}

@Database(
  entities = [WearExerciseEntity::class, WearRoutineEntity::class, WearSessionEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class WearDatabase : RoomDatabase() {
  abstract fun dao(): WearDao

  companion object {
    @Volatile private var INSTANCE: WearDatabase? = null

    fun getInstance(context: Context): WearDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          WearDatabase::class.java,
          "ironlog_wear_offline.db",
        ).build().also { INSTANCE = it }
      }
    }
  }
}
