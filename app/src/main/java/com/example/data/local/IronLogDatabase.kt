package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [
    ExerciseEntity::class,
    RoutineFolderEntity::class,
    RoutineEntity::class,
    RoutineExerciseEntity::class,
    WorkoutSessionEntity::class,
    WorkoutExerciseEntity::class,
    WorkoutSetEntity::class,
    HeartRateSampleEntity::class,
    SyncOperationEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
@TypeConverters(IronLogConverters::class)
abstract class IronLogDatabase : RoomDatabase() {
  abstract fun dao(): IronLogDao

  companion object {
    @Volatile private var INSTANCE: IronLogDatabase? = null

    fun getInstance(context: Context): IronLogDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          IronLogDatabase::class.java,
          "ironlog_offline.db",
        ).build().also { INSTANCE = it }
      }
    }
  }
}
