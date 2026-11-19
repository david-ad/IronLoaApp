package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IronLogDao {

  // Exercises
  @Query("SELECT * FROM exercises ORDER BY isCustom DESC, nameRu ASC")
  fun observeAllExercises(): Flow<List<ExerciseEntity>>

  @Query("SELECT * FROM exercises")
  suspend fun getAllExercisesOnce(): List<ExerciseEntity>

  @Query("SELECT COUNT(*) FROM exercises")
  suspend fun countExercises(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExercises(items: List<ExerciseEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExercise(item: ExerciseEntity)

  @Query("DELETE FROM exercises WHERE id = :id AND isCustom = 1")
  suspend fun deleteCustomExercise(id: String)

  // Folders
  @Query("SELECT * FROM routine_folders WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
  fun observeFolders(): Flow<List<RoutineFolderEntity>>

  @Query("SELECT * FROM routine_folders")
  suspend fun getAllFoldersOnce(): List<RoutineFolderEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFolder(folder: RoutineFolderEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFolders(folders: List<RoutineFolderEntity>)

  // Routines
  @Transaction
  @Query("SELECT * FROM routines WHERE isDeleted = 0 ORDER BY sortOrder ASC, updatedAt DESC")
  fun observeRoutinesWithExercises(): Flow<List<RoutineWithExercises>>

  @Transaction
  @Query("SELECT * FROM routines")
  suspend fun getAllRoutinesOnce(): List<RoutineWithExercises>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRoutine(routine: RoutineEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRoutineExercises(items: List<RoutineExerciseEntity>)

  @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
  suspend fun deleteRoutineExercisesForRoutine(routineId: String)

  @Transaction
  suspend fun saveRoutineFull(routine: RoutineEntity, exercises: List<RoutineExerciseEntity>) {
    upsertRoutine(routine)
    deleteRoutineExercisesForRoutine(routine.id)
    if (exercises.isNotEmpty()) {
      upsertRoutineExercises(exercises)
    }
  }

  // Workout Sessions
  @Transaction
  @Query("SELECT * FROM workout_sessions WHERE isActive = 1 AND isDeleted = 0 ORDER BY startedAtEpochMs DESC LIMIT 1")
  fun observeActiveWorkout(): Flow<WorkoutSessionFull?>

  @Transaction
  @Query("SELECT * FROM workout_sessions WHERE isActive = 0 AND isDeleted = 0 ORDER BY startedAtEpochMs DESC")
  fun observeCompletedWorkouts(): Flow<List<WorkoutSessionFull>>

  @Transaction
  @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0 ORDER BY startedAtEpochMs DESC")
  suspend fun getAllWorkoutsOnce(): List<WorkoutSessionFull>

  @Transaction
  @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
  suspend fun getWorkoutByIdOnce(sessionId: String): WorkoutSessionFull?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSession(session: WorkoutSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWorkoutExercise(item: WorkoutExerciseEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWorkoutExercises(items: List<WorkoutExerciseEntity>)

  @Query("DELETE FROM workout_exercises WHERE sessionId = :sessionId")
  suspend fun deleteWorkoutExercisesForSession(sessionId: String)

  @Query("DELETE FROM workout_exercises WHERE id = :workoutExerciseId")
  suspend fun deleteWorkoutExerciseById(workoutExerciseId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSet(set: WorkoutSetEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSets(sets: List<WorkoutSetEntity>)

  @Query("DELETE FROM workout_sets WHERE id = :setId")
  suspend fun deleteSetById(setId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHeartRateSamples(samples: List<HeartRateSampleEntity>)

  @Transaction
  suspend fun saveWorkoutFull(
    session: WorkoutSessionEntity,
    exercises: List<WorkoutExerciseEntity>,
    sets: List<WorkoutSetEntity>,
    hrSamples: List<HeartRateSampleEntity> = emptyList(),
  ) {
    upsertSession(session)
    deleteWorkoutExercisesForSession(session.id)
    if (exercises.isNotEmpty()) {
      upsertWorkoutExercises(exercises)
    }
    if (sets.isNotEmpty()) {
      upsertSets(sets)
    }
    if (hrSamples.isNotEmpty()) {
      upsertHeartRateSamples(hrSamples)
    }
  }

  // Sync outbox & tombstones
  @Query("SELECT * FROM sync_operations")
  suspend fun getAllSyncOpsOnce(): List<SyncOperationEntity>

  @Query("SELECT * FROM sync_operations WHERE pendingPush = 1")
  suspend fun getPendingSyncOps(): List<SyncOperationEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSyncOp(op: SyncOperationEntity)

  @Query("UPDATE sync_operations SET pendingPush = 0 WHERE entityId IN (:ids)")
  suspend fun markSynced(ids: List<String>)

  @Query("DELETE FROM workout_sets")
  suspend fun clearSets()

  @Query("DELETE FROM workout_exercises")
  suspend fun clearWorkoutExercises()

  @Query("DELETE FROM heart_rate_samples")
  suspend fun clearHeartRateSamples()

  @Query("DELETE FROM workout_sessions")
  suspend fun clearSessions()

  @Query("DELETE FROM routine_exercises")
  suspend fun clearRoutineExercises()

  @Query("DELETE FROM routines")
  suspend fun clearRoutines()

  @Query("DELETE FROM routine_folders")
  suspend fun clearFolders()

  @Query("DELETE FROM exercises WHERE isCustom = 1")
  suspend fun clearCustomExercises()

  @Query("DELETE FROM sync_operations")
  suspend fun clearSyncOps()

  @Transaction
  suspend fun wipeAllUserData() {
    clearSets()
    clearWorkoutExercises()
    clearHeartRateSamples()
    clearSessions()
    clearRoutineExercises()
    clearRoutines()
    clearFolders()
    clearCustomExercises()
    clearSyncOps()
  }
}
