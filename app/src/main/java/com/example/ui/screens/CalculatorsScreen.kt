package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserPreferences
import com.example.ironlog.core.i18n.AppStrings
import com.example.ironlog.core.math.PersonalRecord
import com.example.ironlog.core.math.StrengthCalculators
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.MuscleGroup
import com.example.ironlog.core.model.OneRmFormula
import com.example.ironlog.core.model.WeightUnit
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ui.components.BarbellPlateVisualizer
import com.example.ui.theme.MonoNumericTextStyle
import com.example.ui.theme.SignalAmber
import kotlin.math.roundToInt

@Composable
fun CalculatorsScreen(
  strings: AppStrings,
  prefs: UserPreferences,
  exercises: List<ExerciseDto>,
  completedWorkouts: List<WorkoutSessionDto>,
  personalRecords: Map<String, PersonalRecord>,
  muscleWorkingSets: Map<MuscleGroup, Double>,
  onSetFormula: (OneRmFormula) -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val unitStr = prefs.weightUnit.symbol(prefs.language)

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 720.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          0 to strings.plateCalculator,
          1 to strings.warmupCalculator,
          2 to strings.oneRmCalculator,
          3 to strings.volumeCalculator,
        ).forEach { (idx, title) ->
          FilterChip(
            selected = selectedTab == idx,
            onClick = { selectedTab = idx },
            label = { Text(title) },
            modifier = Modifier.testTag("calc_tab_$idx"),
          )
        }
      }

      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        when (selectedTab) {
          0 -> {
            item {
              PlateCalculatorCard(strings = strings, unit = prefs.weightUnit, unitStr = unitStr)
            }
          }
          1 -> {
            item {
              WarmupCalculatorCard(strings = strings, unit = prefs.weightUnit, unitStr = unitStr)
            }
          }
          2 -> {
            item {
              OneRmCalculatorCard(
                strings = strings,
                prefs = prefs,
                unitStr = unitStr,
                exercises = exercises,
                personalRecords = personalRecords,
                onSetFormula = onSetFormula,
              )
            }
          }
          3 -> {
            item {
              VolumeCalculatorCard(
                strings = strings,
                prefs = prefs,
                unitStr = unitStr,
                exercises = exercises,
                completedWorkouts = completedWorkouts,
                muscleWorkingSets = muscleWorkingSets,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PlateCalculatorCard(
  strings: AppStrings,
  unit: WeightUnit,
  unitStr: String,
) {
  val barPresets = if (unit == WeightUnit.KG) listOf(20.0, 15.0, 10.0) else listOf(45.0, 35.0, 25.0)
  var barWeight by remember(unit) { mutableStateOf(barPresets.first()) }
  var targetInput by remember(unit) { mutableStateOf(if (unit == WeightUnit.KG) "100" else "225") }
  var availablePlates by remember(unit) {
    mutableStateOf(
      if (unit == WeightUnit.KG) StrengthCalculators.defaultKgPlates else StrengthCalculators.defaultLbsPlates
    )
  }

  val target = targetInput.replace(',', '.').toDoubleOrNull() ?: barWeight
  val result = remember(target, barWeight, availablePlates) {
    StrengthCalculators.calculatePlates(target, barWeight, availablePlates)
  }

  ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(strings.plateCalculator, style = MaterialTheme.typography.titleLarge)

      Text(strings.barWeightLabel, style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        barPresets.forEach { preset ->
          FilterChip(
            selected = barWeight == preset,
            onClick = { barWeight = preset },
            label = { Text("${preset.toInt()} $unitStr") },
          )
        }
      }

      OutlinedTextField(
        value = targetInput,
        onValueChange = { targetInput = it },
        label = { Text("${strings.targetWeightLabel} ($unitStr)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("plate_target_input"),
      )

      BarbellPlateVisualizer(result = result)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "${strings.achievedWeightLabel}: ${result.achievedWeight} $unitStr",
          style = MonoNumericTextStyle,
          color = MaterialTheme.colorScheme.primary,
        )
        if (result.remainder != 0.0) {
          Text(
            text = "${strings.remainderLabel}: ${result.remainder} $unitStr",
            style = MonoNumericTextStyle,
            color = SignalAmber,
          )
        }
      }

      Text(strings.perSidePlates, style = MaterialTheme.typography.titleSmall)
      if (result.perSidePlates.isEmpty()) {
        Text("0", style = MonoNumericTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
      } else {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          result.perSidePlates.forEach { p ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(p.colorHex)),
                )
                Text("${p.weight} $unitStr × ${p.countPerSide}", style = MonoNumericTextStyle)
              }
            }
          }
        }
      }

      Text(strings.availablePlatesLabel, style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        availablePlates.forEachIndexed { idx, plate ->
          FilterChip(
            selected = plate.enabled,
            onClick = {
              availablePlates = availablePlates.mapIndexed { i, p ->
                if (i == idx) p.copy(enabled = !p.enabled) else p
              }
            },
            label = { Text("${plate.weight}") },
          )
        }
      }
    }
  }
}

@Composable
private fun WarmupCalculatorCard(
  strings: AppStrings,
  unit: WeightUnit,
  unitStr: String,
) {
  val barWeight = if (unit == WeightUnit.KG) 20.0 else 45.0
  var workingInput by remember(unit) { mutableStateOf(if (unit == WeightUnit.KG) "100" else "225") }
  var warmupCount by remember { mutableIntStateOf(3) }

  val working = workingInput.replace(',', '.').toDoubleOrNull() ?: barWeight
  val sets = remember(working, barWeight, warmupCount, unit) {
    StrengthCalculators.calculateWarmupSets(working, barWeight, warmupCount, unit)
  }

  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(strings.warmupCalculator, style = MaterialTheme.typography.titleLarge)
      OutlinedTextField(
        value = workingInput,
        onValueChange = { workingInput = it },
        label = { Text("${strings.workingWeightLabel} ($unitStr)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Text(strings.warmupCountLabel, style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..5).forEach { count ->
          FilterChip(
            selected = warmupCount == count,
            onClick = { warmupCount = count },
            label = { Text("$count") },
          )
        }
      }

      sets.forEach { rec ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("#${rec.setNumber} (${rec.percentage}%)", style = MaterialTheme.typography.bodyMedium)
            Text(
              text = "${rec.weightDisplay} $unitStr × ${rec.reps} ${strings.repsSuffix}",
              style = MonoNumericTextStyle,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OneRmCalculatorCard(
  strings: AppStrings,
  prefs: UserPreferences,
  unitStr: String,
  exercises: List<ExerciseDto>,
  personalRecords: Map<String, PersonalRecord>,
  onSetFormula: (OneRmFormula) -> Unit,
) {
  var weightInput by remember(prefs.weightUnit) {
    mutableStateOf(if (prefs.weightUnit == WeightUnit.KG) "80" else "185")
  }
  var repsInput by remember { mutableStateOf("5") }
  val weightDisplay = weightInput.replace(',', '.').toDoubleOrNull() ?: 0.0
  val reps = repsInput.toIntOrNull() ?: 1
  val estDisplay = remember(weightDisplay, reps, prefs.preferred1RmFormula) {
    StrengthCalculators.estimate1Rm(weightDisplay, reps, prefs.preferred1RmFormula)
  }
  val exercisesById = remember(exercises) { exercises.associateBy { it.id } }

  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(strings.oneRmCalculator, style = MaterialTheme.typography.titleLarge)

      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        OneRmFormula.entries.forEach { f ->
          FilterChip(
            selected = prefs.preferred1RmFormula == f,
            onClick = { onSetFormula(f) },
            label = { Text(f.label(prefs.language)) },
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = weightInput,
          onValueChange = { weightInput = it },
          label = { Text("${strings.weightCol} ($unitStr)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = repsInput,
          onValueChange = { repsInput = it },
          label = { Text(strings.repsCol) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Text(strings.estimated1RmLabel, style = MaterialTheme.typography.labelLarge)
          Text(
            text = "${(estDisplay * 10).roundToInt() / 10.0} $unitStr",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      // Percentage table
      listOf(95, 90, 85, 80, 75, 70, 60).chunked(2).forEach { pair ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          pair.forEach { pct ->
            val v = ((estDisplay * pct / 100.0) * 10).roundToInt() / 10.0
            Surface(
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("$pct%", style = MaterialTheme.typography.labelSmall)
                Text("$v $unitStr", style = MonoNumericTextStyle)
              }
            }
          }
          if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
      }

      if (personalRecords.isNotEmpty()) {
        Text(strings.personalRecords, style = MaterialTheme.typography.titleSmall, color = SignalAmber)
        personalRecords.values.sortedByDescending { it.estimated1RmKg }.take(6).forEach { pr ->
          val name = exercisesById[pr.exerciseId]?.displayName(prefs.language) ?: pr.exerciseId
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text("${prefs.weightUnit.format(pr.estimated1RmKg)} $unitStr", style = MonoNumericTextStyle)
          }
        }
      }
    }
  }
}

@Composable
private fun VolumeCalculatorCard(
  strings: AppStrings,
  prefs: UserPreferences,
  unitStr: String,
  exercises: List<ExerciseDto>,
  completedWorkouts: List<WorkoutSessionDto>,
  muscleWorkingSets: Map<MuscleGroup, Double>,
) {
  val measurementMap = remember(exercises) { exercises.associate { it.id to it.measurementType } }
  val exercisesById = remember(exercises) { exercises.associateBy { it.id } }
  val sevenDaysAgo = remember { System.currentTimeMillis() - 7L * 24L * 3600L * 1000L }
  val weekVolKg = remember(completedWorkouts, measurementMap) {
    completedWorkouts.filter { it.startedAtEpochMs >= sevenDaysAgo }
      .sumOf { StrengthCalculators.sessionVolumeKg(it, measurementMap) }
  }
  val allVolKg = remember(completedWorkouts, measurementMap) {
    completedWorkouts.sumOf { StrengthCalculators.sessionVolumeKg(it, measurementMap) }
  }

  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(strings.volumeCalculator, style = MaterialTheme.typography.titleLarge)

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(strings.weeklyVolumeLabel, style = MaterialTheme.typography.bodyMedium)
        Text(
          "${prefs.weightUnit.format(weekVolKg)} $unitStr",
          style = MonoNumericTextStyle,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(strings.totalVolume, style = MaterialTheme.typography.bodyMedium)
        Text("${prefs.weightUnit.format(allVolKg)} $unitStr", style = MonoNumericTextStyle)
      }

      val lastSession = completedWorkouts.firstOrNull()
      if (lastSession != null) {
        Text("${lastSession.name}:", style = MaterialTheme.typography.titleSmall)
        lastSession.exercises.forEach { ex ->
          val type = measurementMap[ex.exerciseId] ?: com.example.ironlog.core.model.MeasurementType.WEIGHT_REPS
          val exVol = ex.sets.filter { it.isCompleted }.sumOf { StrengthCalculators.setVolumeKg(it, type) }
          val exName = exercisesById[ex.exerciseId]?.displayName(prefs.language) ?: ex.exerciseId
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(exName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text("${prefs.weightUnit.format(exVol)} $unitStr", style = MonoNumericTextStyle)
          }
        }
      }

      if (muscleWorkingSets.isNotEmpty()) {
        Text(strings.muscleHeatmapTitle, style = MaterialTheme.typography.titleSmall)
        muscleWorkingSets.entries.sortedByDescending { it.value }.forEach { (m, sets) ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(m.label(prefs.language), style = MaterialTheme.typography.bodySmall)
            Text("${(sets * 10).roundToInt() / 10.0} ${strings.setsSuffix}", style = MonoNumericTextStyle)
          }
        }
      }
    }
  }
}
