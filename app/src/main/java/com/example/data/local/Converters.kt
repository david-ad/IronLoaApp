package com.example.data.local

import androidx.room.TypeConverter
import com.example.ironlog.core.model.EquipmentType
import com.example.ironlog.core.model.ExerciseCategory
import com.example.ironlog.core.model.IllustrationPose
import com.example.ironlog.core.model.MeasurementType
import com.example.ironlog.core.model.MuscleGroup
import com.example.ironlog.core.model.SetType

class IronLogConverters {
  @TypeConverter
  fun fromMuscleList(list: List<MuscleGroup>): String = list.joinToString(",") { it.name }

  @TypeConverter
  fun toMuscleList(raw: String): List<MuscleGroup> =
    if (raw.isBlank()) emptyList()
    else raw.split(",").mapNotNull { name -> runCatching { MuscleGroup.valueOf(name) }.getOrNull() }

  @TypeConverter fun fromMuscle(v: MuscleGroup): String = v.name
  @TypeConverter fun toMuscle(v: String): MuscleGroup = MuscleGroup.valueOf(v)

  @TypeConverter fun fromEquipment(v: EquipmentType): String = v.name
  @TypeConverter fun toEquipment(v: String): EquipmentType = EquipmentType.valueOf(v)

  @TypeConverter fun fromCategory(v: ExerciseCategory): String = v.name
  @TypeConverter fun toCategory(v: String): ExerciseCategory = ExerciseCategory.valueOf(v)

  @TypeConverter fun fromMeasurement(v: MeasurementType): String = v.name
  @TypeConverter fun toMeasurement(v: String): MeasurementType = MeasurementType.valueOf(v)

  @TypeConverter fun fromSetType(v: SetType): String = v.name
  @TypeConverter fun toSetType(v: String): SetType = SetType.valueOf(v)

  @TypeConverter fun fromPose(v: IllustrationPose): String = v.name
  @TypeConverter fun toPose(v: String): IllustrationPose = IllustrationPose.valueOf(v)
}
