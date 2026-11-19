package com.example.ironlog.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseDto(
  val id: String,
  val nameRu: String,
  val nameEn: String,
  val descriptionRu: String,
  val descriptionEn: String,
  val primaryMuscle: MuscleGroup,
  val secondaryMuscles: List<MuscleGroup> = emptyList(),
  val equipment: EquipmentType,
  val category: ExerciseCategory,
  val measurementType: MeasurementType,
  val illustrationPose: IllustrationPose = IllustrationPose.BENCH_PRESS,
  val isCustom: Boolean = false,
  val isArchived: Boolean = false,
  val version: Long = 1L,
  val updatedAt: Long = System.currentTimeMillis(),
) {
  fun displayName(lang: AppLanguage): String =
    if (lang == AppLanguage.RU) nameRu.ifBlank { nameEn } else nameEn.ifBlank { nameRu }

  fun displayDescription(lang: AppLanguage): String =
    if (lang == AppLanguage.RU) descriptionRu.ifBlank { descriptionEn }
    else descriptionEn.ifBlank { descriptionRu }
}

@Serializable
data class RoutineFolderDto(
  val id: String,
  val name: String,
  val colorHex: Long = 0xFFC8FF2E,
  val sortOrder: Int = 0,
  val version: Long = 1L,
  val updatedAt: Long = System.currentTimeMillis(),
  val isDeleted: Boolean = false,
)

@Serializable
data class RoutineExerciseDto(
  val id: String,
  val routineId: String,
  val exerciseId: String,
  val sortOrder: Int,
  val supersetGroupId: Int? = null,
  val targetSets: Int = 3,
  val targetReps: Int = 10,
  val targetWeightKg: Double = 0.0,
  val restSeconds: Int = 90,
  val notes: String = "",
)

@Serializable
data class RoutineDto(
  val id: String,
  val folderId: String? = null,
  val name: String,
  val description: String = "",
  val sortOrder: Int = 0,
  val exercises: List<RoutineExerciseDto> = emptyList(),
  val version: Long = 1L,
  val updatedAt: Long = System.currentTimeMillis(),
  val isDeleted: Boolean = false,
)

@Serializable
data class WorkoutSetDto(
  val id: String,
  val workoutExerciseId: String,
  val setIndex: Int,
  val setType: SetType = SetType.NORMAL,
  val weightKg: Double = 0.0,
  val reps: Int = 0,
  val durationSeconds: Int = 0,
  val distanceMeters: Double = 0.0,
  val rpe: Double? = null,
  val isCompleted: Boolean = false,
  val completedAtEpochMs: Long? = null,
)

@Serializable
data class WorkoutExerciseDto(
  val id: String,
  val sessionId: String,
  val exerciseId: String,
  val sortOrder: Int,
  val supersetGroupId: Int? = null,
  val restSecondsOverride: Int? = null,
  val notes: String = "",
  val sets: List<WorkoutSetDto> = emptyList(),
)

@Serializable
data class HeartRateSampleDto(
  val id: String,
  val sessionId: String,
  val timestampEpochMs: Long,
  val bpm: Int,
)

@Serializable
data class WorkoutSessionDto(
  val id: String,
  val routineId: String? = null,
  val name: String,
  val startedAtEpochMs: Long,
  val finishedAtEpochMs: Long? = null,
  val pausedDurationMs: Long = 0L,
  val pauseStartedAtElapsedMs: Long? = null,
  val isActive: Boolean = false,
  val isPaused: Boolean = false,
  val notes: String = "",
  val sourceDevice: String = "phone",
  val avgHeartRate: Int? = null,
  val maxHeartRate: Int? = null,
  val exercises: List<WorkoutExerciseDto> = emptyList(),
  val heartRateSamples: List<HeartRateSampleDto> = emptyList(),
  val version: Long = 1L,
  val updatedAt: Long = System.currentTimeMillis(),
  val isDeleted: Boolean = false,
)

@Serializable
data class DeletedRecordDto(
  val entityId: String,
  val entityType: String,
  val deletedAtEpochMs: Long,
)

@Serializable
data class SyncEnvelope(
  val schemaVersion: Int = 1,
  val sourceDeviceId: String,
  val sourceDeviceType: String,
  val exportedAtEpochMs: Long = System.currentTimeMillis(),
  val sha256Checksum: String = "",
  val exercises: List<ExerciseDto> = emptyList(),
  val folders: List<RoutineFolderDto> = emptyList(),
  val routines: List<RoutineDto> = emptyList(),
  val sessions: List<WorkoutSessionDto> = emptyList(),
  val deletions: List<DeletedRecordDto> = emptyList(),
)

@Serializable
data class WearSyncAck(
  val receiverDeviceId: String,
  val acknowledgedSessionIds: List<String> = emptyList(),
  val acknowledgedRoutineIds: List<String> = emptyList(),
  val timestampEpochMs: Long = System.currentTimeMillis(),
)
