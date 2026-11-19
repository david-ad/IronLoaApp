package com.example.data.repository

import com.example.data.local.ExerciseEntity
import com.example.data.local.HeartRateSampleEntity
import com.example.data.local.RoutineEntity
import com.example.data.local.RoutineExerciseEntity
import com.example.data.local.RoutineFolderEntity
import com.example.data.local.RoutineWithExercises
import com.example.data.local.WorkoutExerciseEntity
import com.example.data.local.WorkoutExerciseWithSets
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSessionFull
import com.example.data.local.WorkoutSetEntity
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.HeartRateSampleDto
import com.example.ironlog.core.model.RoutineDto
import com.example.ironlog.core.model.RoutineExerciseDto
import com.example.ironlog.core.model.RoutineFolderDto
import com.example.ironlog.core.model.WorkoutExerciseDto
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ironlog.core.model.WorkoutSetDto

fun ExerciseEntity.toDto() = ExerciseDto(
  id = id,
  nameRu = nameRu,
  nameEn = nameEn,
  descriptionRu = descriptionRu,
  descriptionEn = descriptionEn,
  primaryMuscle = primaryMuscle,
  secondaryMuscles = secondaryMuscles,
  equipment = equipment,
  category = category,
  measurementType = measurementType,
  illustrationPose = illustrationPose,
  isCustom = isCustom,
  isArchived = isArchived,
  version = version,
  updatedAt = updatedAt,
)

fun ExerciseDto.toEntity() = ExerciseEntity(
  id = id,
  nameRu = nameRu,
  nameEn = nameEn,
  descriptionRu = descriptionRu,
  descriptionEn = descriptionEn,
  primaryMuscle = primaryMuscle,
  secondaryMuscles = secondaryMuscles,
  equipment = equipment,
  category = category,
  measurementType = measurementType,
  illustrationPose = illustrationPose,
  isCustom = isCustom,
  isArchived = isArchived,
  version = version,
  updatedAt = updatedAt,
)

fun RoutineFolderEntity.toDto() = RoutineFolderDto(
  id = id,
  name = name,
  colorHex = colorHex,
  sortOrder = sortOrder,
  version = version,
  updatedAt = updatedAt,
  isDeleted = isDeleted,
)

fun RoutineFolderDto.toEntity() = RoutineFolderEntity(
  id = id,
  name = name,
  colorHex = colorHex,
  sortOrder = sortOrder,
  version = version,
  updatedAt = updatedAt,
  isDeleted = isDeleted,
)

fun RoutineExerciseEntity.toDto() = RoutineExerciseDto(
  id = id,
  routineId = routineId,
  exerciseId = exerciseId,
  sortOrder = sortOrder,
  supersetGroupId = supersetGroupId,
  targetSets = targetSets,
  targetReps = targetReps,
  targetWeightKg = targetWeightKg,
  restSeconds = restSeconds,
  notes = notes,
)

fun RoutineExerciseDto.toEntity() = RoutineExerciseEntity(
  id = id,
  routineId = routineId,
  exerciseId = exerciseId,
  sortOrder = sortOrder,
  supersetGroupId = supersetGroupId,
  targetSets = targetSets,
  targetReps = targetReps,
  targetWeightKg = targetWeightKg,
  restSeconds = restSeconds,
  notes = notes,
)

fun RoutineWithExercises.toDto() = RoutineDto(
  id = routine.id,
  folderId = routine.folderId,
  name = routine.name,
  description = routine.description,
  sortOrder = routine.sortOrder,
  exercises = exercises.sortedBy { it.sortOrder }.map { it.toDto() },
  version = routine.version,
  updatedAt = routine.updatedAt,
  isDeleted = routine.isDeleted,
)

fun WorkoutSetEntity.toDto() = WorkoutSetDto(
  id = id,
  workoutExerciseId = workoutExerciseId,
  setIndex = setIndex,
  setType = setType,
  weightKg = weightKg,
  reps = reps,
  durationSeconds = durationSeconds,
  distanceMeters = distanceMeters,
  rpe = rpe,
  isCompleted = isCompleted,
  completedAtEpochMs = completedAtEpochMs,
)

fun WorkoutSetDto.toEntity() = WorkoutSetEntity(
  id = id,
  workoutExerciseId = workoutExerciseId,
  setIndex = setIndex,
  setType = setType,
  weightKg = weightKg,
  reps = reps,
  durationSeconds = durationSeconds,
  distanceMeters = distanceMeters,
  rpe = rpe,
  isCompleted = isCompleted,
  completedAtEpochMs = completedAtEpochMs,
)

fun WorkoutExerciseWithSets.toDto() = WorkoutExerciseDto(
  id = exercise.id,
  sessionId = exercise.sessionId,
  exerciseId = exercise.exerciseId,
  sortOrder = exercise.sortOrder,
  supersetGroupId = exercise.supersetGroupId,
  restSecondsOverride = exercise.restSecondsOverride,
  notes = exercise.notes,
  sets = sets.sortedBy { it.setIndex }.map { it.toDto() },
)

fun HeartRateSampleEntity.toDto() = HeartRateSampleDto(
  id = id,
  sessionId = sessionId,
  timestampEpochMs = timestampEpochMs,
  bpm = bpm,
)

fun HeartRateSampleDto.toEntity() = HeartRateSampleEntity(
  id = id,
  sessionId = sessionId,
  timestampEpochMs = timestampEpochMs,
  bpm = bpm,
)

fun WorkoutSessionFull.toDto() = WorkoutSessionDto(
  id = session.id,
  routineId = session.routineId,
  name = session.name,
  startedAtEpochMs = session.startedAtEpochMs,
  finishedAtEpochMs = session.finishedAtEpochMs,
  pausedDurationMs = session.pausedDurationMs,
  pauseStartedAtElapsedMs = session.pauseStartedAtElapsedMs,
  isActive = session.isActive,
  isPaused = session.isPaused,
  notes = session.notes,
  sourceDevice = session.sourceDevice,
  avgHeartRate = session.avgHeartRate,
  maxHeartRate = session.maxHeartRate,
  exercises = exercises.sortedBy { it.exercise.sortOrder }.map { it.toDto() },
  heartRateSamples = heartRateSamples.sortedBy { it.timestampEpochMs }.map { it.toDto() },
  version = session.version,
  updatedAt = session.updatedAt,
  isDeleted = session.isDeleted,
)
