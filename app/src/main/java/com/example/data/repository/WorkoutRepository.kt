package com.example.data.repository

import android.os.SystemClock
import androidx.room.withTransaction
import com.example.data.local.IronLogDatabase
import com.example.data.local.RoutineEntity
import com.example.data.local.SyncOperationEntity
import com.example.data.local.WorkoutExerciseEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.ironlog.core.backup.BackupCodec
import com.example.ironlog.core.model.AppLanguage
import com.example.ironlog.core.model.DeletedRecordDto
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.RoutineDto
import com.example.ironlog.core.model.RoutineFolderDto
import com.example.ironlog.core.model.SetType
import com.example.ironlog.core.model.SyncEnvelope
import com.example.ironlog.core.model.WorkoutExerciseDto
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ironlog.core.model.WorkoutSetDto
import com.example.ironlog.core.seed.ExerciseCatalogSeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class WorkoutRepository(
  private val db: IronLogDatabase,
  private val prefs: UserPreferencesRepository,
) {
  private val dao = db.dao()

  val exercisesFlow: Flow<List<ExerciseDto>> =
    dao.observeAllExercises().map { list -> list.map { it.toDto() } }

  val foldersFlow: Flow<List<RoutineFolderDto>> =
    dao.observeFolders().map { list -> list.map { it.toDto() } }

  val routinesFlow: Flow<List<RoutineDto>> =
    dao.observeRoutinesWithExercises().map { list -> list.map { it.toDto() } }

  val activeWorkoutFlow: Flow<WorkoutSessionDto?> =
    dao.observeActiveWorkout().map { it?.toDto() }

  val completedWorkoutsFlow: Flow<List<WorkoutSessionDto>> =
    dao.observeCompletedWorkouts().map { list -> list.map { it.toDto() } }

  suspend fun ensureSeeded() {
    if (dao.countExercises() == 0) {
      db.withTransaction {
        dao.upsertExercises(ExerciseCatalogSeed.defaultExercises.map { it.toEntity() })
        dao.upsertFolders(ExerciseCatalogSeed.defaultFolders.map { it.toEntity() })
        ExerciseCatalogSeed.defaultRoutines.forEach { routine ->
          dao.saveRoutineFull(
            RoutineEntity(
              id = routine.id,
              folderId = routine.folderId,
              name = routine.name,
              description = routine.description,
              sortOrder = routine.sortOrder,
              version = routine.version,
              updatedAt = routine.updatedAt,
              isDeleted = false,
            ),
            routine.exercises.map { it.toEntity() },
          )
        }
      }
    }
  }

  // Exercises
  suspend fun saveExercise(exercise: ExerciseDto) {
    val updated = exercise.copy(version = exercise.version + 1, updatedAt = System.currentTimeMillis())
    db.withTransaction {
      dao.upsertExercise(updated.toEntity())
      if (updated.isCustom) {
        dao.upsertSyncOp(
          SyncOperationEntity(
            entityId = updated.id,
            entityType = "exercise",
            version = updated.version,
            updatedAt = updated.updatedAt,
            isDeleted = false,
            pendingPush = true,
          )
        )
      }
    }
  }

  suspend fun archiveExercise(exercise: ExerciseDto, archived: Boolean) {
    saveExercise(exercise.copy(isArchived = archived))
  }

  suspend fun deleteCustomExercise(exerciseId: String) {
    db.withTransaction {
      dao.deleteCustomExercise(exerciseId)
      dao.upsertSyncOp(
        SyncOperationEntity(
          entityId = exerciseId,
          entityType = "exercise",
          version = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis(),
          isDeleted = true,
          pendingPush = true,
        )
      )
    }
  }

  // Folders
  suspend fun saveFolder(folder: RoutineFolderDto) {
    val updated = folder.copy(version = folder.version + 1, updatedAt = System.currentTimeMillis())
    db.withTransaction {
      dao.upsertFolder(updated.toEntity())
      dao.upsertSyncOp(
        SyncOperationEntity(
          entityId = updated.id,
          entityType = "folder",
          version = updated.version,
          updatedAt = updated.updatedAt,
          isDeleted = updated.isDeleted,
          pendingPush = true,
        )
      )
    }
  }

  suspend fun deleteFolder(folder: RoutineFolderDto) {
    saveFolder(folder.copy(isDeleted = true))
  }

  // Routines
  suspend fun saveRoutine(routine: RoutineDto) {
    val now = System.currentTimeMillis()
    val updated = routine.copy(
      version = routine.version + 1,
      updatedAt = now,
      exercises = routine.exercises.mapIndexed { idx, ex -> ex.copy(routineId = routine.id, sortOrder = idx) },
    )
    db.withTransaction {
      dao.saveRoutineFull(
        RoutineEntity(
          id = updated.id,
          folderId = updated.folderId,
          name = updated.name,
          description = updated.description,
          sortOrder = updated.sortOrder,
          version = updated.version,
          updatedAt = updated.updatedAt,
          isDeleted = updated.isDeleted,
        ),
        updated.exercises.map { it.toEntity() },
      )
      dao.upsertSyncOp(
        SyncOperationEntity(
          entityId = updated.id,
          entityType = "routine",
          version = updated.version,
          updatedAt = updated.updatedAt,
          isDeleted = updated.isDeleted,
          pendingPush = true,
        )
      )
    }
  }

  suspend fun duplicateRoutine(source: RoutineDto, copySuffix: String) {
    val newRoutineId = UUID.randomUUID().toString()
    val copy = source.copy(
      id = newRoutineId,
      name = "${source.name} ($copySuffix)",
      version = 1L,
      updatedAt = System.currentTimeMillis(),
      exercises = source.exercises.map { ex ->
        ex.copy(id = UUID.randomUUID().toString(), routineId = newRoutineId)
      },
    )
    saveRoutine(copy)
  }

  suspend fun deleteRoutine(routine: RoutineDto) {
    saveRoutine(routine.copy(isDeleted = true))
  }

  // Active Workout Lifecycle
  suspend fun startWorkoutFromRoutine(routine: RoutineDto, completedHistory: List<WorkoutSessionDto>): WorkoutSessionDto {
    val existingActive = dao.getAllWorkoutsOnce().firstOrNull { it.session.isActive }
    if (existingActive != null) return existingActive.toDto()

    val sessionId = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    val workoutExercises = routine.exercises.sortedBy { it.sortOrder }.mapIndexed { idx, rEx ->
      val weId = UUID.randomUUID().toString()
      val prevSets = findPreviousExerciseSets(rEx.exerciseId, completedHistory)
      val sets = (0 until rEx.targetSets.coerceAtLeast(1)).map { setIdx ->
        val prev = prevSets.getOrNull(setIdx)
        WorkoutSetDto(
          id = UUID.randomUUID().toString(),
          workoutExerciseId = weId,
          setIndex = setIdx,
          setType = SetType.NORMAL,
          weightKg = prev?.weightKg ?: rEx.targetWeightKg,
          reps = prev?.reps ?: rEx.targetReps,
          durationSeconds = prev?.durationSeconds ?: 0,
          distanceMeters = prev?.distanceMeters ?: 0.0,
          rpe = null,
          isCompleted = false,
        )
      }
      WorkoutExerciseDto(
        id = weId,
        sessionId = sessionId,
        exerciseId = rEx.exerciseId,
        sortOrder = idx,
        supersetGroupId = rEx.supersetGroupId,
        restSecondsOverride = rEx.restSeconds,
        notes = rEx.notes,
        sets = sets,
      )
    }

    val session = WorkoutSessionDto(
      id = sessionId,
      routineId = routine.id,
      name = routine.name,
      startedAtEpochMs = now,
      isActive = true,
      isPaused = false,
      sourceDevice = "phone",
      exercises = workoutExercises,
      version = 1L,
      updatedAt = now,
    )
    persistWorkoutSession(session, markSyncPending = false)
    return session
  }

  suspend fun startEmptyWorkout(defaultTitle: String): WorkoutSessionDto {
    val existingActive = dao.getAllWorkoutsOnce().firstOrNull { it.session.isActive }
    if (existingActive != null) return existingActive.toDto()

    val now = System.currentTimeMillis()
    val session = WorkoutSessionDto(
      id = UUID.randomUUID().toString(),
      routineId = null,
      name = defaultTitle,
      startedAtEpochMs = now,
      isActive = true,
      isPaused = false,
      sourceDevice = "phone",
      exercises = emptyList(),
      version = 1L,
      updatedAt = now,
    )
    persistWorkoutSession(session, markSyncPending = false)
    return session
  }

  suspend fun persistWorkoutSession(session: WorkoutSessionDto, markSyncPending: Boolean = !session.isActive) {
    val now = System.currentTimeMillis()
    val normalized = session.copy(
      version = session.version + 1,
      updatedAt = now,
      exercises = session.exercises.mapIndexed { exIdx, ex ->
        ex.copy(
          sessionId = session.id,
          sortOrder = exIdx,
          sets = ex.sets.mapIndexed { setIdx, s -> s.copy(workoutExerciseId = ex.id, setIndex = setIdx) },
        )
      },
    )

    val sessionEntity = WorkoutSessionEntity(
      id = normalized.id,
      routineId = normalized.routineId,
      name = normalized.name,
      startedAtEpochMs = normalized.startedAtEpochMs,
      finishedAtEpochMs = normalized.finishedAtEpochMs,
      pausedDurationMs = normalized.pausedDurationMs,
      pauseStartedAtElapsedMs = normalized.pauseStartedAtElapsedMs,
      isActive = normalized.isActive,
      isPaused = normalized.isPaused,
      notes = normalized.notes,
      sourceDevice = normalized.sourceDevice,
      avgHeartRate = normalized.avgHeartRate,
      maxHeartRate = normalized.maxHeartRate,
      version = normalized.version,
      updatedAt = normalized.updatedAt,
      isDeleted = normalized.isDeleted,
    )

    val exEntities = normalized.exercises.map { ex ->
      WorkoutExerciseEntity(
        id = ex.id,
        sessionId = normalized.id,
        exerciseId = ex.exerciseId,
        sortOrder = ex.sortOrder,
        supersetGroupId = ex.supersetGroupId,
        restSecondsOverride = ex.restSecondsOverride,
        notes = ex.notes,
      )
    }

    val setEntities = normalized.exercises.flatMap { ex -> ex.sets.map { it.toEntity() } }
    val hrEntities = normalized.heartRateSamples.map { it.toEntity() }

    db.withTransaction {
      dao.saveWorkoutFull(sessionEntity, exEntities, setEntities, hrEntities)
      if (markSyncPending) {
        dao.upsertSyncOp(
          SyncOperationEntity(
            entityId = normalized.id,
            entityType = "session",
            version = normalized.version,
            updatedAt = normalized.updatedAt,
            isDeleted = normalized.isDeleted,
            pendingPush = true,
          )
        )
      }
    }
  }

  suspend fun togglePauseWorkout(session: WorkoutSessionDto): WorkoutSessionDto {
    val nowElapsed = SystemClock.elapsedRealtime()
    val updated = if (!session.isPaused) {
      session.copy(isPaused = true, pauseStartedAtElapsedMs = nowElapsed)
    } else {
      val pausedDelta = session.pauseStartedAtElapsedMs?.let { (nowElapsed - it).coerceAtLeast(0L) } ?: 0L
      session.copy(
        isPaused = false,
        pausedDurationMs = session.pausedDurationMs + pausedDelta,
        pauseStartedAtElapsedMs = null,
      )
    }
    persistWorkoutSession(updated, markSyncPending = false)
    return updated
  }

  suspend fun finishWorkout(session: WorkoutSessionDto): WorkoutSessionDto {
    val now = System.currentTimeMillis()
    val nowElapsed = SystemClock.elapsedRealtime()
    val extraPause = if (session.isPaused && session.pauseStartedAtElapsedMs != null) {
      (nowElapsed - session.pauseStartedAtElapsedMs!!).coerceAtLeast(0L)
    } else 0L

    val completedOnlyExercises = session.exercises.map { ex ->
      val doneSets = ex.sets.filter { it.isCompleted }
      ex.copy(sets = doneSets.ifEmpty { ex.sets })
    }
    val finished = session.copy(
      finishedAtEpochMs = now,
      pausedDurationMs = session.pausedDurationMs + extraPause,
      pauseStartedAtElapsedMs = null,
      isActive = false,
      isPaused = false,
      exercises = completedOnlyExercises,
    )
    persistWorkoutSession(finished, markSyncPending = true)
    prefs.clearRestTimer()
    return finished
  }

  suspend fun discardActiveWorkout(session: WorkoutSessionDto) {
    persistWorkoutSession(session.copy(isActive = false, isDeleted = true), markSyncPending = false)
    prefs.clearRestTimer()
  }

  suspend fun deleteCompletedWorkout(session: WorkoutSessionDto) {
    persistWorkoutSession(session.copy(isDeleted = true), markSyncPending = true)
  }

  fun findPreviousExerciseSets(
    exerciseId: String,
    completedHistory: List<WorkoutSessionDto>,
  ): List<WorkoutSetDto> {
    return completedHistory
      .asSequence()
      .filter { !it.isActive && !it.isDeleted }
      .sortedByDescending { it.startedAtEpochMs }
      .mapNotNull { session -> session.exercises.firstOrNull { it.exerciseId == exerciseId } }
      .firstOrNull { it.sets.any { s -> s.isCompleted } }
      ?.sets
      ?.filter { it.isCompleted }
      ?: emptyList()
  }

  // Backup & Sync Export/Import
  suspend fun buildFullEnvelope(onlyPendingSync: Boolean = false): SyncEnvelope {
    val deviceId = prefs.getOrCreateDeviceId()
    val exercises = dao.getAllExercisesOnce().map { it.toDto() }.filter { !onlyPendingSync || it.isCustom }
    val folders = dao.getAllFoldersOnce().map { it.toDto() }
    val routines = dao.getAllRoutinesOnce().map { it.toDto() }
    val sessions = dao.getAllWorkoutsOnce()
      .map { it.toDto() }
      .filter { !it.isActive }
    val deletions = dao.getAllSyncOpsOnce()
      .filter { it.isDeleted }
      .map { DeletedRecordDto(it.entityId, it.entityType, it.updatedAt) }

    return SyncEnvelope(
      schemaVersion = 1,
      sourceDeviceId = deviceId,
      sourceDeviceType = "phone",
      exportedAtEpochMs = System.currentTimeMillis(),
      exercises = exercises,
      folders = folders,
      routines = routines,
      sessions = sessions,
      deletions = deletions,
    )
  }

  suspend fun exportJsonBackup(): String {
    val envelope = buildFullEnvelope(onlyPendingSync = false)
    return BackupCodec.encodeBackupJson(envelope)
  }

  suspend fun exportCsvHistory(language: AppLanguage): String {
    val exercisesMap = dao.getAllExercisesOnce().associate { it.id to it.toDto() }
    val sessions = dao.getAllWorkoutsOnce().map { it.toDto() }
    return BackupCodec.encodeWorkoutHistoryCsv(sessions, exercisesMap, language)
  }

  suspend fun mergeIncomingEnvelope(envelope: SyncEnvelope): Pair<Int, Int> {
    var importedRoutines = 0
    var importedSessions = 0
    val localDeviceId = prefs.getOrCreateDeviceId()
    if (envelope.sourceDeviceId == localDeviceId) return 0 to 0

    db.withTransaction {
      // Exercises (custom or newer)
      val existingExercises = dao.getAllExercisesOnce().associateBy { it.id }
      envelope.exercises.forEach { inc ->
        val cur = existingExercises[inc.id]
        if (cur == null || inc.updatedAt > cur.updatedAt) {
          dao.upsertExercise(inc.toEntity())
        }
      }

      // Folders
      val existingFolders = dao.getAllFoldersOnce().associateBy { it.id }
      envelope.folders.forEach { inc ->
        val cur = existingFolders[inc.id]
        if (cur == null || inc.updatedAt > cur.updatedAt) {
          dao.upsertFolder(inc.toEntity())
        }
      }

      // Routines (conflict resolution by updatedAt)
      val existingRoutines = dao.getAllRoutinesOnce().associateBy { it.routine.id }
      envelope.routines.forEach { inc ->
        val cur = existingRoutines[inc.id]
        if (cur == null || inc.updatedAt > cur.routine.updatedAt) {
          dao.saveRoutineFull(
            RoutineEntity(
              id = inc.id,
              folderId = inc.folderId,
              name = inc.name,
              description = inc.description,
              sortOrder = inc.sortOrder,
              version = inc.version,
              updatedAt = inc.updatedAt,
              isDeleted = inc.isDeleted,
            ),
            inc.exercises.map { it.toEntity() },
          )
          importedRoutines++
        }
      }

      // Completed Sessions (UUID deduplication + updatedAt check)
      val existingSessions = dao.getAllWorkoutsOnce().associateBy { it.session.id }
      envelope.sessions.filter { !it.isActive }.forEach { inc ->
        val cur = existingSessions[inc.id]
        if (cur == null || inc.updatedAt > cur.session.updatedAt) {
          val sessionEntity = WorkoutSessionEntity(
            id = inc.id,
            routineId = inc.routineId,
            name = inc.name,
            startedAtEpochMs = inc.startedAtEpochMs,
            finishedAtEpochMs = inc.finishedAtEpochMs,
            pausedDurationMs = inc.pausedDurationMs,
            pauseStartedAtElapsedMs = inc.pauseStartedAtElapsedMs,
            isActive = false,
            isPaused = false,
            notes = inc.notes,
            sourceDevice = inc.sourceDevice,
            avgHeartRate = inc.avgHeartRate,
            maxHeartRate = inc.maxHeartRate,
            version = inc.version,
            updatedAt = inc.updatedAt,
            isDeleted = inc.isDeleted,
          )
          val exEntities = inc.exercises.map { ex ->
            WorkoutExerciseEntity(
              id = ex.id,
              sessionId = inc.id,
              exerciseId = ex.exerciseId,
              sortOrder = ex.sortOrder,
              supersetGroupId = ex.supersetGroupId,
              restSecondsOverride = ex.restSecondsOverride,
              notes = ex.notes,
            )
          }
          val setEntities = inc.exercises.flatMap { ex -> ex.sets.map { it.toEntity() } }
          val hrEntities = inc.heartRateSamples.map { it.toEntity() }
          dao.saveWorkoutFull(sessionEntity, exEntities, setEntities, hrEntities)
          importedSessions++
        }
      }

      // Apply tombstones
      envelope.deletions.forEach { del ->
        when (del.entityType) {
          "routine" -> existingRoutines[del.entityId]?.let { cur ->
            if (del.deletedAtEpochMs >= cur.routine.updatedAt) {
              dao.upsertRoutine(cur.routine.copy(isDeleted = true, updatedAt = del.deletedAtEpochMs))
            }
          }
          "session" -> existingSessions[del.entityId]?.let { cur ->
            if (del.deletedAtEpochMs >= cur.session.updatedAt) {
              dao.upsertSession(cur.session.copy(isDeleted = true, updatedAt = del.deletedAtEpochMs))
            }
          }
        }
      }
    }
    return importedRoutines to importedSessions
  }

  suspend fun markSynced(ids: List<String>) {
    if (ids.isNotEmpty()) {
      dao.markSynced(ids)
      prefs.setLastSyncEpochMs(System.currentTimeMillis())
    }
  }

  suspend fun wipeAllData() {
    dao.wipeAllUserData()
    prefs.clearRestTimer()
    ensureSeeded()
  }
}
