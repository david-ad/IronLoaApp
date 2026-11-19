package com.example.ironlog.core

import com.example.ironlog.core.backup.BackupCodec
import com.example.ironlog.core.math.StrengthCalculators
import com.example.ironlog.core.model.AppLanguage
import com.example.ironlog.core.model.MeasurementType
import com.example.ironlog.core.model.OneRmFormula
import com.example.ironlog.core.model.SetType
import com.example.ironlog.core.model.SyncEnvelope
import com.example.ironlog.core.model.WeightUnit
import com.example.ironlog.core.model.WorkoutExerciseDto
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ironlog.core.model.WorkoutSetDto
import com.example.ironlog.core.seed.ExerciseCatalogSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorsAndBackupTest {

  @Test
  fun plateCalculator_loads100kgCorrectly() {
    val result = StrengthCalculators.calculatePlates(
      targetWeight = 100.0,
      barWeight = 20.0,
      availablePlates = StrengthCalculators.defaultKgPlates,
    )
    assertEquals(100.0, result.achievedWeight, 0.01)
    assertEquals(0.0, result.remainder, 0.01)
    assertEquals(25.0, result.perSidePlates[0].weight, 0.01)
    assertEquals(15.0, result.perSidePlates[1].weight, 0.01)
  }

  @Test
  fun warmupAnd1RmCalculations_areAccurate() {
    val warmups = StrengthCalculators.calculateWarmupSets(100.0, 20.0, 3, WeightUnit.KG)
    assertEquals(3, warmups.size)
    assertEquals(45.0, warmups[0].weightDisplay, 0.01)

    val epley = StrengthCalculators.estimate1Rm(100.0, 5, OneRmFormula.EPLEY)
    assertEquals(116.66, epley, 0.2)
  }

  @Test
  fun backupJsonRoundTripAndIntegrityCheck() {
    val session = WorkoutSessionDto(
      id = "s-1",
      name = "Push A",
      startedAtEpochMs = 1700000000000L,
      finishedAtEpochMs = 1700003600000L,
      isActive = false,
      exercises = listOf(
        WorkoutExerciseDto(
          id = "we-1",
          sessionId = "s-1",
          exerciseId = "ex_bench_press_barbell",
          sortOrder = 0,
          sets = listOf(
            WorkoutSetDto("set-1", "we-1", 0, SetType.NORMAL, 80.0, 8, isCompleted = true),
          ),
        )
      ),
    )
    val env = SyncEnvelope(
      sourceDeviceId = "phone-1",
      sourceDeviceType = "phone",
      exercises = ExerciseCatalogSeed.defaultExercises.take(2),
      sessions = listOf(session),
    )
    val json = BackupCodec.encodeBackupJson(env)
    val decoded = BackupCodec.decodeAndVerifyBackup(json)
    assertTrue(decoded.isSuccess)
    assertEquals(1, decoded.getOrThrow().sessions.size)

    val csv = BackupCodec.encodeWorkoutHistoryCsv(
      sessions = listOf(session),
      exercisesById = ExerciseCatalogSeed.defaultExercises.associateBy { it.id },
      language = AppLanguage.RU,
    )
    assertTrue(csv.contains("Жим штанги лёжа"))
    assertEquals(
      640.0,
      StrengthCalculators.sessionVolumeKg(session, mapOf("ex_bench_press_barbell" to MeasurementType.WEIGHT_REPS)),
      0.01,
    )
  }
}
