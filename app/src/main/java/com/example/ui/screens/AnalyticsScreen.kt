package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserPreferences
import com.example.ironlog.core.i18n.AppStrings
import com.example.ironlog.core.math.PersonalRecord
import com.example.ironlog.core.math.StrengthCalculators
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.MuscleGroup
import com.example.ironlog.core.model.RoutineDto
import com.example.ironlog.core.model.SetType
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ui.components.ChartPoint
import com.example.ui.components.LineTrendChart
import com.example.ui.components.MuscleHeatmapView
import com.example.ui.theme.MonoNumericTextStyle
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TelemetryCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
  strings: AppStrings,
  prefs: UserPreferences,
  exercises: List<ExerciseDto>,
  routines: List<RoutineDto>,
  completedWorkouts: List<WorkoutSessionDto>,
  personalRecords: Map<String, PersonalRecord>,
  muscleWorkingSets: Map<MuscleGroup, Double>,
) {
  var periodDays by remember { mutableIntStateOf(30) } // 7, 30, 90, 0(all)
  var showBackBody by remember { mutableStateOf(false) }
  var selectedExerciseId by remember(exercises) {
    mutableStateOf(exercises.firstOrNull()?.id ?: "")
  }
  var selectedMuscleNotice by remember { mutableStateOf<MuscleGroup?>(null) }

  val exercisesById = remember(exercises) { exercises.associateBy { it.id } }
  val measurementMap = remember(exercises) { exercises.associate { it.id to it.measurementType } }
  val dateLabelFmt = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }

  val periodSessions = remember(completedWorkouts, periodDays) {
    val cutoff = if (periodDays == 0) 0L else System.currentTimeMillis() - periodDays * 24L * 3600L * 1000L
    completedWorkouts.filter { it.startedAtEpochMs >= cutoff }.sortedBy { it.startedAtEpochMs }
  }

  val totalHours = remember(periodSessions) {
    val sec = periodSessions.sumOf {
      val end = it.finishedAtEpochMs ?: it.startedAtEpochMs
      ((end - it.startedAtEpochMs - it.pausedDurationMs) / 1000L).coerceAtLeast(0L)
    }
    (sec / 360.0).toInt() / 10.0
  }

  val freqPerWeek = remember(periodSessions, periodDays) {
    val weeks = if (periodDays > 0) (periodDays / 7.0).coerceAtLeast(1.0) else {
      val first = periodSessions.firstOrNull()?.startedAtEpochMs ?: System.currentTimeMillis()
      ((System.currentTimeMillis() - first) / (7.0 * 24 * 3600 * 1000)).coerceAtLeast(1.0)
    }
    ((periodSessions.size / weeks) * 10).toInt() / 10.0
  }

  // Exercise-specific series
  val exercisePoints = remember(periodSessions, selectedExerciseId, prefs.weightUnit, prefs.preferred1RmFormula) {
    periodSessions.mapNotNull { session ->
      val ex = session.exercises.firstOrNull { it.exerciseId == selectedExerciseId } ?: return@mapNotNull null
      val working = ex.sets.filter { it.isCompleted && it.setType != SetType.WARMUP }
      if (working.isEmpty()) return@mapNotNull null
      val lbl = dateLabelFmt.format(Date(session.startedAtEpochMs))
      val maxWeight = prefs.weightUnit.toDisplay(working.maxOf { it.weightKg })
      val maxReps = working.maxOf { it.reps }.toDouble()
      val best1Rm = prefs.weightUnit.toDisplay(
        working.maxOf { StrengthCalculators.estimate1Rm(it.weightKg, it.reps, prefs.preferred1RmFormula) }
      )
      Triple(lbl, maxWeight, Pair(maxReps, best1Rm))
    }
  }

  val volumePoints = remember(periodSessions, prefs.weightUnit, measurementMap) {
    periodSessions.map { session ->
      ChartPoint(
        label = dateLabelFmt.format(Date(session.startedAtEpochMs)),
        value = prefs.weightUnit.toDisplay(StrengthCalculators.sessionVolumeKg(session, measurementMap)),
      )
    }
  }

  val setDynamicsPoints = remember(periodSessions) {
    periodSessions.map { session ->
      ChartPoint(
        label = dateLabelFmt.format(Date(session.startedAtEpochMs)),
        value = session.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }.toDouble(),
      )
    }
  }

  val unitStr = prefs.weightUnit.symbol(prefs.language)

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 720.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text(strings.analyticsTitle, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(7 to strings.period7d, 30 to strings.period30d, 90 to strings.period90d, 0 to strings.periodAll)
            .forEach { (days, lbl) ->
              FilterChip(
                selected = periodDays == days,
                onClick = { periodDays = days },
                label = { Text(lbl) },
              )
            }
        }
      }

      // KPI Summary
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          KpiBox(strings.workoutsCount, "${periodSessions.size}", Modifier.weight(1f))
          KpiBox(strings.totalTimeLabel, "${totalHours}h", Modifier.weight(1f))
          KpiBox(strings.frequencyPerWeek, "$freqPerWeek", Modifier.weight(1f))
        }
      }

      // Muscle Heatmap
      item {
        ElevatedCard(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(strings.muscleHeatmapTitle, style = MaterialTheme.typography.titleMedium)
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !showBackBody, onClick = { showBackBody = false }, label = { Text(strings.frontBody) })
                FilterChip(selected = showBackBody, onClick = { showBackBody = true }, label = { Text(strings.backBody) })
              }
            }
            Spacer(Modifier.height(10.dp))
            MuscleHeatmapView(
              workingSetsByMuscle = muscleWorkingSets,
              showBack = showBackBody,
              language = prefs.language,
              onSelectMuscle = { selectedMuscleNotice = it },
            )
            selectedMuscleNotice?.let { m ->
              Spacer(Modifier.height(6.dp))
              Text(
                text = "${m.label(prefs.language)}: ${muscleWorkingSets[m] ?: 0.0} ${strings.setsSuffix}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      // Exercise Selector for Progression Charts
      item {
        Text(strings.weightProgression, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          exercises.filter { !it.isArchived }.take(15).forEach { ex ->
            FilterChip(
              selected = selectedExerciseId == ex.id,
              onClick = { selectedExerciseId = ex.id },
              label = { Text(ex.displayName(prefs.language), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
          }
        }
      }

      item {
        LineTrendChart(
          points = exercisePoints.map { ChartPoint(it.first, it.second) },
          lineColor = MaterialTheme.colorScheme.primary,
          unitSuffix = unitStr,
          emptyText = strings.noWorkoutsYet,
        )
      }

      item {
        Text(strings.oneRmProgression, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        LineTrendChart(
          points = exercisePoints.map { ChartPoint(it.first, it.third.second) },
          lineColor = TelemetryCyan,
          unitSuffix = unitStr,
          emptyText = strings.noWorkoutsYet,
        )
      }

      item {
        Text(strings.repsProgression, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        LineTrendChart(
          points = exercisePoints.map { ChartPoint(it.first, it.third.first) },
          lineColor = SignalAmber,
          unitSuffix = strings.repsSuffix,
          emptyText = strings.noWorkoutsYet,
        )
      }

      item {
        Text(strings.volumeProgression, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        LineTrendChart(
          points = volumePoints,
          lineColor = MaterialTheme.colorScheme.primary,
          unitSuffix = unitStr,
          emptyText = strings.noWorkoutsYet,
        )
      }

      item {
        Text(strings.setsDynamics, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        LineTrendChart(
          points = setDynamicsPoints,
          lineColor = TelemetryCyan,
          unitSuffix = strings.setsSuffix,
          emptyText = strings.noWorkoutsYet,
        )
      }

      // Personal Records Board & Routine Stats
      if (personalRecords.isNotEmpty()) {
        item {
          ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(strings.personalRecords, style = MaterialTheme.typography.titleMedium, color = SignalAmber)
              personalRecords.values.sortedByDescending { it.estimated1RmKg }.take(8).forEach { pr ->
                val exName = exercisesById[pr.exerciseId]?.displayName(prefs.language) ?: pr.exerciseId
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(exName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text(
                    text = "${prefs.weightUnit.format(pr.maxWeightKg)} $unitStr (1RM ${prefs.weightUnit.format(pr.estimated1RmKg)})",
                    style = MonoNumericTextStyle,
                  )
                }
              }
            }
          }
        }
      }

      if (routines.isNotEmpty()) {
        item {
          ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(strings.routinesTitle, style = MaterialTheme.typography.titleMedium)
              routines.forEach { r ->
                val count = completedWorkouts.count { it.routineId == r.id }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(r.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                  Text("$count", style = MonoNumericTextStyle, color = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      }

      item { Spacer(Modifier.height(24.dp)) }
    }
  }
}

@Composable
private fun KpiBox(label: String, value: String, modifier: Modifier = Modifier) {
  ElevatedCard(
    modifier = modifier,
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
      Spacer(Modifier.height(4.dp))
      Text(value, style = MonoNumericTextStyle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
  }
}
