package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.ironlog.core.model.EquipmentType
import com.example.ironlog.core.model.ExerciseCategory
import com.example.ironlog.core.model.IllustrationPose
import com.example.ironlog.core.model.MeasurementType
import com.example.ironlog.core.model.MuscleGroup
import com.example.ironlog.core.model.SetType

@Entity(tableName = "exercises")
data class ExerciseEntity(
  @PrimaryKey val id: String,
  val nameRu: String,
  val nameEn: String,
  val descriptionRu: String,
  val descriptionEn: String,
  val primaryMuscle: MuscleGroup,
  val secondaryMuscles: List<MuscleGroup>,
  val equipment: EquipmentType,
  val category: ExerciseCategory,
  val measurementType: MeasurementType,
  val illustrationPose: IllustrationPose,
  val isCustom: Boolean,
  val isArchived: Boolean,
  val version: Long,
  val updatedAt: Long,
)

@Entity(tableName = "routine_folders")
data class RoutineFolderEntity(
  @PrimaryKey val id: String,
  val name: String,
  val colorHex: Long,
  val sortOrder: Int,
  val version: Long,
  val updatedAt: Long,
  val isDeleted: Boolean,
)

@Entity(tableName = "routines")
data class RoutineEntity(
  @PrimaryKey val id: String,
  val folderId: String?,
  val name: String,
  val description: String,
  val sortOrder: Int,
  val version: Long,
  val updatedAt: Long,
  val isDeleted: Boolean,
)

@Entity(
  tableName = "routine_exercises",
  foreignKeys = [
    ForeignKey(
      entity = RoutineEntity::class,
      parentColumns = ["id"],
      childColumns = ["routineId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("routineId")],
)
data class RoutineExerciseEntity(
  @PrimaryKey val id: String,
  val routineId: String,
  val exerciseId: String,
  val sortOrder: Int,
  val supersetGroupId: Int?,
  val targetSets: Int,
  val targetReps: Int,
  val targetWeightKg: Double,
  val restSeconds: Int,
  val notes: String,
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
  @PrimaryKey val id: String,
  val routineId: String?,
  val name: String,
  val startedAtEpochMs: Long,
  val finishedAtEpochMs: Long?,
  val pausedDurationMs: Long,
  val pauseStartedAtElapsedMs: Long?,
  val isActive: Boolean,
  val isPaused: Boolean,
  val notes: String,
  val sourceDevice: String,
  val avgHeartRate: Int?,
  val maxHeartRate: Int?,
  val version: Long,
  val updatedAt: Long,
  val isDeleted: Boolean,
)

@Entity(
  tableName = "workout_exercises",
  foreignKeys = [
    ForeignKey(
      entity = WorkoutSessionEntity::class,
      parentColumns = ["id"],
      childColumns = ["sessionId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("sessionId")],
)
data class WorkoutExerciseEntity(
  @PrimaryKey val id: String,
  val sessionId: String,
  val exerciseId: String,
  val sortOrder: Int,
  val supersetGroupId: Int?,
  val restSecondsOverride: Int?,
  val notes: String,
)

@Entity(
  tableName = "workout_sets",
  foreignKeys = [
    ForeignKey(
      entity = WorkoutExerciseEntity::class,
      parentColumns = ["id"],
      childColumns = ["workoutExerciseId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("workoutExerciseId")],
)
data class WorkoutSetEntity(
  @PrimaryKey val id: String,
  val workoutExerciseId: String,
  val setIndex: Int,
  val setType: SetType,
  val weightKg: Double,
  val reps: Int,
  val durationSeconds: Int,
  val distanceMeters: Double,
  val rpe: Double?,
  val isCompleted: Boolean,
  val completedAtEpochMs: Long?,
)

@Entity(
  tableName = "heart_rate_samples",
  foreignKeys = [
    ForeignKey(
      entity = WorkoutSessionEntity::class,
      parentColumns = ["id"],
      childColumns = ["sessionId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("sessionId")],
)
data class HeartRateSampleEntity(
  @PrimaryKey val id: String,
  val sessionId: String,
  val timestampEpochMs: Long,
  val bpm: Int,
)

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
  @PrimaryKey val entityId: String,
  val entityType: String,
  val version: Long,
  val updatedAt: Long,
  val isDeleted: Boolean,
  val pendingPush: Boolean,
)

data class RoutineWithExercises(
  @Embedded val routine: RoutineEntity,
  @Relation(parentColumn = "id", entityColumn = "routineId")
  val exercises: List<RoutineExerciseEntity>,
)

data class WorkoutExerciseWithSets(
  @Embedded val exercise: WorkoutExerciseEntity,
  @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
  val sets: List<WorkoutSetEntity>,
)

data class WorkoutSessionFull(
  @Embedded val session: WorkoutSessionEntity,
  @Relation(entity = WorkoutExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
  val exercises: List<WorkoutExerciseWithSets>,
  @Relation(parentColumn = "id", entityColumn = "sessionId")
  val heartRateSamples: List<HeartRateSampleEntity>,
)
