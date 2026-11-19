package com.example.ironlog.core.math

import com.example.ironlog.core.model.MeasurementType
import com.example.ironlog.core.model.MuscleGroup
import com.example.ironlog.core.model.OneRmFormula
import com.example.ironlog.core.model.SetType
import com.example.ironlog.core.model.WeightUnit
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ironlog.core.model.WorkoutSetDto
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

data class PlateOption(
  val weight: Double,
  val enabled: Boolean = true,
  val countAvailable: Int = 8,
  val colorHex: Long,
)

data class PlateBreakdownItem(
  val weight: Double,
  val countPerSide: Int,
  val colorHex: Long,
)

data class PlateCalculationResult(
  val targetWeight: Double,
  val achievedWeight: Double,
  val barWeight: Double,
  val perSidePlates: List<PlateBreakdownItem>,
  val remainder: Double,
)

data class WarmupSetRecommendation(
  val setNumber: Int,
  val percentage: Int,
  val weightDisplay: Double,
  val reps: Int,
)

data class PersonalRecord(
  val exerciseId: String,
  val sessionId: String,
  val achievedAtEpochMs: Long,
  val maxWeightKg: Double,
  val estimated1RmKg: Double,
  val bestSetVolumeKg: Double,
  val bestReps: Int,
)

object StrengthCalculators {

  val defaultKgPlates: List<PlateOption> = listOf(
    PlateOption(25.0, true, 8, 0xFFEF4444),
    PlateOption(20.0, true, 8, 0xFF3B82F6),
    PlateOption(15.0, true, 4, 0xFFEAB308),
    PlateOption(10.0, true, 4, 0xFF22C55E),
    PlateOption(5.0, true, 4, 0xFFF3F4F6),
    PlateOption(2.5, true, 4, 0xFFEF4444),
    PlateOption(1.25, true, 4, 0xFF94A3B8),
  )

  val defaultLbsPlates: List<PlateOption> = listOf(
    PlateOption(45.0, true, 8, 0xFF3B82F6),
    PlateOption(35.0, true, 4, 0xFFEAB308),
    PlateOption(25.0, true, 4, 0xFF22C55E),
    PlateOption(10.0, true, 4, 0xFFF3F4F6),
    PlateOption(5.0, true, 4, 0xFF38BDF8),
    PlateOption(2.5, true, 4, 0xFF94A3B8),
  )

  fun calculatePlates(
    targetWeight: Double,
    barWeight: Double,
    availablePlates: List<PlateOption>,
  ): PlateCalculationResult {
    if (targetWeight <= barWeight) {
      return PlateCalculationResult(
        targetWeight = targetWeight,
        achievedWeight = barWeight,
        barWeight = barWeight,
        perSidePlates = emptyList(),
        remainder = (targetWeight - barWeight).coerceAtMost(0.0),
      )
    }

    var remainingPerSide = (targetWeight - barWeight) / 2.0
    val sorted = availablePlates.filter { it.enabled && it.countAvailable >= 2 }
      .sortedByDescending { it.weight }
    val used = mutableListOf<PlateBreakdownItem>()

    for (plate in sorted) {
      val maxPairs = plate.countAvailable / 2
      if (maxPairs <= 0 || plate.weight <= 0.0) continue
      val needed = ((remainingPerSide + 1e-6) / plate.weight).toInt().coerceAtMost(maxPairs)
      if (needed > 0) {
        used.add(PlateBreakdownItem(plate.weight, needed, plate.colorHex))
        remainingPerSide -= needed * plate.weight
      }
    }

    val perSideTotal = used.sumOf { it.weight * it.countPerSide }
    val achieved = barWeight + perSideTotal * 2.0
    val remainder = ((targetWeight - achieved) * 100.0).roundToInt() / 100.0

    return PlateCalculationResult(
      targetWeight = targetWeight,
      achievedWeight = achieved,
      barWeight = barWeight,
      perSidePlates = used,
      remainder = if (abs(remainder) < 0.01) 0.0 else remainder,
    )
  }

  fun calculateWarmupSets(
    workingWeight: Double,
    barWeight: Double,
    warmupCount: Int,
    unit: WeightUnit,
  ): List<WarmupSetRecommendation> {
    if (workingWeight <= barWeight || warmupCount <= 0) return emptyList()
    val step = if (unit == WeightUnit.KG) 2.5 else 5.0
    val percentages = when (warmupCount) {
      1 -> listOf(60)
      2 -> listOf(50, 75)
      3 -> listOf(45, 65, 85)
      4 -> listOf(40, 55, 70, 85)
      else -> listOf(35, 50, 65, 75, 88).take(warmupCount)
    }
    val repsList = when (warmupCount) {
      1 -> listOf(8)
      2 -> listOf(8, 5)
      3 -> listOf(10, 6, 3)
      4 -> listOf(10, 6, 4, 2)
      else -> listOf(10, 8, 5, 3, 2).take(warmupCount)
    }

    return percentages.mapIndexed { idx, pct ->
      val raw = workingWeight * (pct / 100.0)
      val rounded = ((raw / step).roundToInt() * step).coerceAtLeast(barWeight)
      WarmupSetRecommendation(
        setNumber = idx + 1,
        percentage = pct,
        weightDisplay = rounded,
        reps = repsList.getOrElse(idx) { 5 },
      )
    }
  }

  fun estimate1Rm(weightKg: Double, reps: Int, formula: OneRmFormula = OneRmFormula.EPLEY): Double {
    if (weightKg <= 0.0 || reps <= 0) return 0.0
    if (reps == 1) return weightKg
    return when (formula) {
      OneRmFormula.EPLEY -> weightKg * (1.0 + reps / 30.0)
      OneRmFormula.BRZYCKI -> {
        val denom = (37 - reps).coerceAtLeast(1)
        weightKg * (36.0 / denom)
      }
      OneRmFormula.LOMBARDI -> weightKg * reps.toDouble().pow(0.10)
      OneRmFormula.OCONNER -> weightKg * (1.0 + 0.025 * reps)
    }
  }

  fun setVolumeKg(set: WorkoutSetDto, measurementType: MeasurementType): Double {
    if (!set.isCompleted) return 0.0
    return when (measurementType) {
      MeasurementType.WEIGHT_REPS -> (set.weightKg * set.reps).coerceAtLeast(0.0)
      MeasurementType.WEIGHT_DURATION -> (set.weightKg * (set.durationSeconds / 10.0)).coerceAtLeast(0.0)
      else -> 0.0
    }
  }

  fun sessionVolumeKg(
    session: WorkoutSessionDto,
    measurementByExercise: Map<String, MeasurementType>,
  ): Double {
    return session.exercises.sumOf { ex ->
      val type = measurementByExercise[ex.exerciseId] ?: MeasurementType.WEIGHT_REPS
      ex.sets.filter { it.isCompleted && it.setType != SetType.WARMUP }
        .sumOf { setVolumeKg(it, type) }
    }
  }

  fun computePersonalRecords(
    sessions: List<WorkoutSessionDto>,
    formula: OneRmFormula = OneRmFormula.EPLEY,
  ): Map<String, PersonalRecord> {
    val bestByExercise = mutableMapOf<String, PersonalRecord>()
    val completedSessions = sessions
      .filter { !it.isActive && !it.isDeleted && it.finishedAtEpochMs != null }
      .sortedBy { it.startedAtEpochMs }

    for (session in completedSessions) {
      for (exercise in session.exercises) {
        val workingSets = exercise.sets.filter { it.isCompleted && it.setType != SetType.WARMUP }
        if (workingSets.isEmpty()) continue
        var current = bestByExercise[exercise.exerciseId] ?: PersonalRecord(
          exerciseId = exercise.exerciseId,
          sessionId = session.id,
          achievedAtEpochMs = session.startedAtEpochMs,
          maxWeightKg = 0.0,
          estimated1RmKg = 0.0,
          bestSetVolumeKg = 0.0,
          bestReps = 0,
        )
        for (set in workingSets) {
          val est1Rm = estimate1Rm(set.weightKg, set.reps, formula)
          val setVol = set.weightKg * set.reps
          val improved =
            set.weightKg > current.maxWeightKg ||
              est1Rm > current.estimated1RmKg ||
              setVol > current.bestSetVolumeKg ||
              set.reps > current.bestReps
          if (improved) {
            current = PersonalRecord(
              exerciseId = exercise.exerciseId,
              sessionId = session.id,
              achievedAtEpochMs = session.startedAtEpochMs,
              maxWeightKg = maxOf(current.maxWeightKg, set.weightKg),
              estimated1RmKg = maxOf(current.estimated1RmKg, est1Rm),
              bestSetVolumeKg = maxOf(current.bestSetVolumeKg, setVol),
              bestReps = maxOf(current.bestReps, set.reps),
            )
          }
        }
        bestByExercise[exercise.exerciseId] = current
      }
    }
    return bestByExercise
  }

  fun computeMuscleWorkingSets(
    sessions: List<WorkoutSessionDto>,
    primaryMuscleByExercise: Map<String, MuscleGroup>,
    secondaryMusclesByExercise: Map<String, List<MuscleGroup>>,
  ): Map<MuscleGroup, Double> {
    val totals = mutableMapOf<MuscleGroup, Double>()
    for (session in sessions.filter { !it.isActive && !it.isDeleted }) {
      for (ex in session.exercises) {
        val workingCount = ex.sets.count { it.isCompleted && it.setType != SetType.WARMUP }
        if (workingCount == 0) continue
        val primary = primaryMuscleByExercise[ex.exerciseId] ?: continue
        totals[primary] = (totals[primary] ?: 0.0) + workingCount
        secondaryMusclesByExercise[ex.exerciseId].orEmpty().forEach { sec ->
          totals[sec] = (totals[sec] ?: 0.0) + (workingCount * 0.5)
        }
      }
    }
    return totals
  }
}
