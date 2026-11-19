package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserPreferences
import com.example.ironlog.core.i18n.AppStrings
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.MeasurementType
import com.example.ironlog.core.model.SetType
import com.example.ironlog.core.model.WeightUnit
import com.example.ironlog.core.model.WorkoutExerciseDto
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ironlog.core.model.WorkoutSetDto
import com.example.service.TimerServiceState
import com.example.service.WorkoutTimerService
import com.example.ui.theme.FailureCoral
import com.example.ui.theme.MonoNumericTextStyle
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SupersetViolet
import com.example.ui.viewmodel.FinishedWorkoutSummary
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
  session: WorkoutSessionDto,
  strings: AppStrings,
  prefs: UserPreferences,
  catalogExercises: List<ExerciseDto>,
  elapsedSec: Long,
  timerState: TimerServiceState,
  findPreviousSets: (String) -> List<WorkoutSetDto>,
  onTogglePause: () -> Unit,
  onUpdateWorkoutNotes: (String) -> Unit,
  onAddExercise: (ExerciseDto) -> Unit,
  onReplaceExercise: (String, ExerciseDto) -> Unit,
  onRemoveExercise: (String) -> Unit,
  onMoveExercise: (Int, Int) -> Unit,
  onToggleSuperset: (String) -> Unit,
  onUpdateExerciseNotesAndRest: (String, String, Int?) -> Unit,
  onAddSet: (String) -> Unit,
  onRemoveSet: (String, String) -> Unit,
  onUpdateSet: (String, WorkoutSetDto, Boolean, String) -> Unit,
  onCopyPreviousSet: (String, Int, String) -> Unit,
  onStartRestTimer: (Int, String) -> Unit,
  onAdjustRestTimer: (Int) -> Unit,
  onSkipRestTimer: () -> Unit,
  onFinishWorkout: () -> Unit,
  onDiscardWorkout: () -> Unit,
) {
  val catalogMap = remember(catalogExercises) { catalogExercises.associateBy { it.id } }
  var showAddExercisePicker by remember { mutableStateOf(false) }
  var replacingWorkoutExId by remember { mutableStateOf<String?>(null) }
  var showDiscardConfirm by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 720.dp),
    ) {
      // Sticky Top Telemetry Bar
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = session.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = WorkoutTimerService.formatClock(elapsedSec),
                  style = MonoNumericTextStyle,
                  color = if (session.isPaused) SignalAmber else MaterialTheme.colorScheme.primary,
                )
                if (session.isPaused) {
                  Text(
                    text = strings.pausedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = SignalAmber,
                  )
                }
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("workout_pause_button"),
              ) {
                Icon(
                  imageVector = if (session.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                  contentDescription = if (session.isPaused) strings.resume else strings.pause,
                )
              }
              Button(
                onClick = onFinishWorkout,
                modifier = Modifier
                  .height(44.dp)
                  .testTag("finish_workout_button"),
              ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.finish, fontWeight = FontWeight.Bold)
              }
            }
          }

          // Active Rest Timer HUD
          AnimatedVisibility(visible = timerState.restRunning) {
            val total = timerState.restTotalSeconds.coerceAtLeast(1)
            val progress = (timerState.restRemainingSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                      text = "${strings.restTimer}: ${WorkoutTimerService.formatClock(timerState.restRemainingSeconds.toLong())}",
                      style = MonoNumericTextStyle,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallTimerBtn("−15s") { onAdjustRestTimer(-15) }
                    SmallTimerBtn("+15s") { onAdjustRestTimer(15) }
                    SmallTimerBtn("+30s") { onAdjustRestTimer(30) }
                    SmallTimerBtn(strings.skip) { onSkipRestTimer() }
                  }
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                  progress = { progress },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                )
              }
            }
          }

          if (!timerState.restRunning) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Filled.Timer,
                contentDescription = strings.restTimer,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              listOf(60, 90, 120, 180).forEach { sec ->
                SmallTimerBtn("${sec}s") { onStartRestTimer(sec, session.name) }
              }
            }
          }
        }
      }

      // Exercise List
      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item {
          OutlinedTextField(
            value = session.notes,
            onValueChange = onUpdateWorkoutNotes,
            label = { Text(strings.notesHint) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
          )
        }

        itemsIndexed(session.exercises, key = { _, ex -> ex.id }) { index, workoutEx ->
          val meta = catalogMap[workoutEx.exerciseId]
          val prevSets = remember(workoutEx.exerciseId) { findPreviousSets(workoutEx.exerciseId) }
          ActiveExerciseCard(
            index = index,
            totalExercises = session.exercises.size,
            workoutEx = workoutEx,
            exerciseMeta = meta,
            prevSets = prevSets,
            strings = strings,
            prefs = prefs,
            onMoveUp = { onMoveExercise(index, -1) },
            onMoveDown = { onMoveExercise(index, 1) },
            onReplace = { replacingWorkoutExId = workoutEx.id },
            onRemove = { onRemoveExercise(workoutEx.id) },
            onToggleSuperset = { onToggleSuperset(workoutEx.id) },
            onUpdateNotesAndRest = { notes, rest -> onUpdateExerciseNotesAndRest(workoutEx.id, notes, rest) },
            onAddSet = { onAddSet(workoutEx.id) },
            onRemoveSet = { setId -> onRemoveSet(workoutEx.id, setId) },
            onUpdateSet = { set, justDone ->
              onUpdateSet(workoutEx.id, set, justDone, meta?.displayName(prefs.language) ?: "")
            },
            onCopyPrev = { setIdx -> onCopyPreviousSet(workoutEx.id, setIdx, workoutEx.exerciseId) },
            onStartManualRest = { sec -> onStartRestTimer(sec, meta?.displayName(prefs.language) ?: "") },
          )
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { showAddExercisePicker = true },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("workout_add_exercise_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text(strings.addExercise)
            }
            OutlinedButton(
              onClick = { showDiscardConfirm = true },
              modifier = Modifier.height(48.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = FailureCoral),
            ) {
              Icon(Icons.Filled.Close, contentDescription = null)
              Spacer(Modifier.width(4.dp))
              Text(strings.discardWorkout)
            }
          }
        }
        item { Spacer(Modifier.height(24.dp)) }
      }
    }
  }

  if (showAddExercisePicker) {
    ExercisePickerDialog(
      exercises = catalogExercises.filter { !it.isArchived },
      strings = strings,
      language = prefs.language,
      onDismiss = { showAddExercisePicker = false },
      onSelect = { ex ->
        onAddExercise(ex)
        showAddExercisePicker = false
      },
    )
  }

  replacingWorkoutExId?.let { targetId ->
    ExercisePickerDialog(
      exercises = catalogExercises.filter { !it.isArchived },
      strings = strings,
      language = prefs.language,
      onDismiss = { replacingWorkoutExId = null },
      onSelect = { ex ->
        onReplaceExercise(targetId, ex)
        replacingWorkoutExId = null
      },
    )
  }

  if (showDiscardConfirm) {
    AlertDialog(
      onDismissRequest = { showDiscardConfirm = false },
      title = { Text(strings.discardWorkout) },
      text = { Text(strings.wipeConfirmMessage) },
      confirmButton = {
        Button(
          onClick = {
            showDiscardConfirm = false
            onDiscardWorkout()
          },
          colors = ButtonDefaults.buttonColors(containerColor = FailureCoral),
        ) { Text(strings.confirm) }
      },
      dismissButton = {
        TextButton(onClick = { showDiscardConfirm = false }) { Text(strings.cancel) }
      },
    )
  }
}

@Composable
private fun SmallTimerBtn(text: String, onClick: () -> Unit) {
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(8.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
    )
  }
}

@Composable
private fun ActiveExerciseCard(
  index: Int,
  totalExercises: Int,
  workoutEx: WorkoutExerciseDto,
  exerciseMeta: ExerciseDto?,
  prevSets: List<WorkoutSetDto>,
  strings: AppStrings,
  prefs: UserPreferences,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onReplace: () -> Unit,
  onRemove: () -> Unit,
  onToggleSuperset: () -> Unit,
  onUpdateNotesAndRest: (String, Int?) -> Unit,
  onAddSet: () -> Unit,
  onRemoveSet: (String) -> Unit,
  onUpdateSet: (WorkoutSetDto, Boolean) -> Unit,
  onCopyPrev: (Int) -> Unit,
  onStartManualRest: (Int) -> Unit,
) {
  val measurementType = exerciseMeta?.measurementType ?: MeasurementType.WEIGHT_REPS
  val exerciseTitle = exerciseMeta?.displayName(prefs.language) ?: workoutEx.exerciseId
  var menuOpen by remember { mutableStateOf(false) }
  var showNotes by remember { mutableStateOf(workoutEx.notes.isNotBlank()) }

  // Inline exercise execution stopwatch for timed / isometric / cardio exercises
  var exStopwatchRunning by remember { mutableStateOf(false) }
  var exStopwatchSeconds by remember { mutableIntStateOf(0) }
  LaunchedEffect(exStopwatchRunning) {
    while (exStopwatchRunning) {
      delay(1000L)
      exStopwatchSeconds++
    }
  }

  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (workoutEx.supersetGroupId != null) {
              Surface(
                color = SupersetViolet.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
              ) {
                Text(
                  text = "${strings.supersetBadge} ${workoutEx.supersetGroupId}",
                  style = MaterialTheme.typography.labelSmall,
                  color = SupersetViolet,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
            Text(
              text = exerciseTitle,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = "${strings.restTimer}: ${workoutEx.restSecondsOverride ?: 90}s • ${measurementType.label(prefs.language)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Box {
          IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = strings.edit)
          }
          DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
              text = { Text("${strings.restTimer}: ${workoutEx.restSecondsOverride ?: 90}s (+30s)") },
              leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) },
              onClick = {
                menuOpen = false
                val next = ((workoutEx.restSecondsOverride ?: 90) + 30).let { if (it > 240) 30 else it }
                onUpdateNotesAndRest(workoutEx.notes, next)
              },
            )
            DropdownMenuItem(
              text = { Text("${strings.restTimer}: Start now") },
              onClick = {
                menuOpen = false
                onStartManualRest(workoutEx.restSecondsOverride ?: 90)
              },
            )
            DropdownMenuItem(
              text = { Text(strings.notesHint) },
              onClick = {
                menuOpen = false
                showNotes = !showNotes
              },
            )
            if (index + 1 < totalExercises) {
              DropdownMenuItem(
                text = { Text(strings.linkSuperset) },
                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                onClick = {
                  menuOpen = false
                  onToggleSuperset()
                },
              )
            }
            if (index > 0) {
              DropdownMenuItem(
                text = { Text("↑") },
                leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                onClick = {
                  menuOpen = false
                  onMoveUp()
                },
              )
            }
            if (index + 1 < totalExercises) {
              DropdownMenuItem(
                text = { Text("↓") },
                leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                onClick = {
                  menuOpen = false
                  onMoveDown()
                },
              )
            }
            DropdownMenuItem(
              text = { Text(strings.replaceExercise) },
              leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
              onClick = {
                menuOpen = false
                onReplace()
              },
            )
            DropdownMenuItem(
              text = { Text(strings.delete) },
              leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
              onClick = {
                menuOpen = false
                onRemove()
              },
            )
          }
        }
      }

      if (measurementType == MeasurementType.DURATION ||
        measurementType == MeasurementType.WEIGHT_DURATION ||
        measurementType == MeasurementType.DISTANCE_DURATION
      ) {
        Spacer(Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "⏱ ${WorkoutTimerService.formatClock(exStopwatchSeconds.toLong())}",
              style = MonoNumericTextStyle,
              color = MaterialTheme.colorScheme.tertiary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              SmallTimerBtn(if (exStopwatchRunning) strings.pause else strings.startNow) {
                exStopwatchRunning = !exStopwatchRunning
              }
              SmallTimerBtn("00:00") {
                exStopwatchRunning = false
                exStopwatchSeconds = 0
              }
            }
          }
        }
      }

      if (showNotes) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
          value = workoutEx.notes,
          onValueChange = { onUpdateNotesAndRest(it, workoutEx.restSecondsOverride) },
          label = { Text(strings.notesHint) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      Spacer(Modifier.height(10.dp))

      // Header row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(strings.setCol, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
        Text(strings.prevCol, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        if (measurementType == MeasurementType.WEIGHT_REPS || measurementType == MeasurementType.WEIGHT_DURATION) {
          Text(
            "${strings.weightCol} (${prefs.weightUnit.symbol(prefs.language)})",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.Center,
          )
        }
        if (measurementType == MeasurementType.WEIGHT_REPS || measurementType == MeasurementType.REPS_ONLY) {
          Text(strings.repsCol, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
        }
        if (measurementType == MeasurementType.DURATION ||
          measurementType == MeasurementType.WEIGHT_DURATION ||
          measurementType == MeasurementType.DISTANCE_DURATION
        ) {
          Text(strings.timeCol, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
        }
        if (measurementType == MeasurementType.DISTANCE_DURATION) {
          Text(strings.distCol, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
        }
        Text(strings.rpeCol, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
        Text(strings.doneCol, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
      }

      Spacer(Modifier.height(6.dp))

      workoutEx.sets.forEachIndexed { setIdx, set ->
        val prev = prevSets.getOrNull(setIdx)
        ActiveSetRow(
          setIndex = setIdx,
          set = set,
          prevSet = prev,
          measurementType = measurementType,
          unit = prefs.weightUnit,
          strings = strings,
          onUpdate = onUpdateSet,
          onCopyPrev = { onCopyPrev(setIdx) },
          onRemove = { onRemoveSet(set.id) },
        )
        Spacer(Modifier.height(6.dp))
      }

      OutlinedButton(
        onClick = onAddSet,
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp),
      ) {
        Text(strings.addSet)
      }
    }
  }
}

@Composable
private fun ActiveSetRow(
  setIndex: Int,
  set: WorkoutSetDto,
  prevSet: WorkoutSetDto?,
  measurementType: MeasurementType,
  unit: WeightUnit,
  strings: AppStrings,
  onUpdate: (WorkoutSetDto, Boolean) -> Unit,
  onCopyPrev: () -> Unit,
  onRemove: () -> Unit,
) {
  var weightText by remember(set.id, set.weightKg, unit) {
    mutableStateOf(if (set.weightKg == 0.0) "" else unit.format(set.weightKg))
  }
  var repsText by remember(set.id, set.reps) {
    mutableStateOf(if (set.reps == 0) "" else set.reps.toString())
  }
  var durText by remember(set.id, set.durationSeconds) {
    mutableStateOf(if (set.durationSeconds == 0) "" else set.durationSeconds.toString())
  }
  var distText by remember(set.id, set.distanceMeters) {
    mutableStateOf(if (set.distanceMeters == 0.0) "" else set.distanceMeters.toInt().toString())
  }
  var typeMenuOpen by remember { mutableStateOf(false) }
  var rpeMenuOpen by remember { mutableStateOf(false) }

  val rowBg = if (set.isCompleted) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
  }

  val prevLabel = when {
    prevSet == null -> "—"
    measurementType == MeasurementType.WEIGHT_REPS -> "${unit.format(prevSet.weightKg)}×${prevSet.reps}"
    measurementType == MeasurementType.REPS_ONLY -> "${prevSet.reps}"
    measurementType == MeasurementType.DURATION -> "${prevSet.durationSeconds}s"
    measurementType == MeasurementType.WEIGHT_DURATION -> "${unit.format(prevSet.weightKg)}/${prevSet.durationSeconds}s"
    else -> "${prevSet.distanceMeters.toInt()}m"
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(rowBg)
      .padding(horizontal = 4.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Set Type & Number badge
    Box {
      Surface(
        modifier = Modifier
          .width(34.dp)
          .height(40.dp)
          .clickable { typeMenuOpen = true },
        shape = RoundedCornerShape(8.dp),
        color = when (set.setType) {
          SetType.NORMAL -> MaterialTheme.colorScheme.surface
          SetType.WARMUP -> SignalAmber.copy(alpha = 0.25f)
          SetType.FAILURE -> FailureCoral.copy(alpha = 0.25f)
          SetType.DROP -> SupersetViolet.copy(alpha = 0.25f)
        },
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = if (set.setType == SetType.NORMAL) "${setIndex + 1}" else set.setType.shortCode,
            style = MonoNumericTextStyle,
          )
        }
      }
      DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
        SetType.entries.forEach { st ->
          DropdownMenuItem(
            text = { Text("${st.shortCode} • ${st.nameRu} / ${st.nameEn}") },
            onClick = {
              typeMenuOpen = false
              onUpdate(set.copy(setType = st), false)
            },
          )
        }
        DropdownMenuItem(
          text = { Text(strings.delete, color = FailureCoral) },
          onClick = {
            typeMenuOpen = false
            onRemove()
          },
        )
      }
    }

    // Previous value (tap to copy!)
    Box(
      modifier = Modifier
        .weight(1f)
        .height(40.dp)
        .clip(RoundedCornerShape(6.dp))
        .clickable(enabled = prevSet != null) { onCopyPrev() },
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = prevLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        if (prevSet != null) {
          Icon(
            Icons.Filled.ContentCopy,
            contentDescription = strings.copyPrevious,
            modifier = Modifier.size(10.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    if (measurementType == MeasurementType.WEIGHT_REPS || measurementType == MeasurementType.WEIGHT_DURATION) {
      CompactNumberField(
        value = weightText,
        onValueChange = { txt ->
          weightText = txt
          val parsed = txt.replace(',', '.').toDoubleOrNull() ?: 0.0
          onUpdate(set.copy(weightKg = unit.toKg(parsed)), false)
        },
        placeholder = "0",
        modifier = Modifier.weight(1.1f),
      )
    }

    if (measurementType == MeasurementType.WEIGHT_REPS || measurementType == MeasurementType.REPS_ONLY) {
      CompactNumberField(
        value = repsText,
        onValueChange = { txt ->
          repsText = txt
          val parsed = txt.toIntOrNull() ?: 0
          onUpdate(set.copy(reps = parsed), false)
        },
        placeholder = "0",
        isDecimal = false,
        modifier = Modifier.weight(0.9f),
      )
    }

    if (measurementType == MeasurementType.DURATION ||
      measurementType == MeasurementType.WEIGHT_DURATION ||
      measurementType == MeasurementType.DISTANCE_DURATION
    ) {
      CompactNumberField(
        value = durText,
        onValueChange = { txt ->
          durText = txt
          val parsed = txt.toIntOrNull() ?: 0
          onUpdate(set.copy(durationSeconds = parsed), false)
        },
        placeholder = "60",
        isDecimal = false,
        modifier = Modifier.weight(0.9f),
      )
    }

    if (measurementType == MeasurementType.DISTANCE_DURATION) {
      CompactNumberField(
        value = distText,
        onValueChange = { txt ->
          distText = txt
          val parsed = txt.replace(',', '.').toDoubleOrNull() ?: 0.0
          onUpdate(set.copy(distanceMeters = parsed), false)
        },
        placeholder = "500",
        modifier = Modifier.weight(0.9f),
      )
    }

    // RPE selector
    Box {
      Surface(
        modifier = Modifier
          .width(42.dp)
          .height(40.dp)
          .clickable { rpeMenuOpen = true },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = set.rpe?.toString() ?: "—",
            style = MaterialTheme.typography.labelMedium,
            color = if (set.rpe != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      DropdownMenu(expanded = rpeMenuOpen, onDismissRequest = { rpeMenuOpen = false }) {
        listOf(6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0).forEach { r ->
          DropdownMenuItem(
            text = { Text("RPE $r") },
            onClick = {
              rpeMenuOpen = false
              onUpdate(set.copy(rpe = r), false)
            },
          )
        }
        DropdownMenuItem(
          text = { Text("Clear RPE") },
          onClick = {
            rpeMenuOpen = false
            onUpdate(set.copy(rpe = null), false)
          },
        )
      }
    }

    // Complete Set Toggle (48dp touch target)
    Surface(
      modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable {
          val nowDone = !set.isCompleted
          onUpdate(
            set.copy(
              isCompleted = nowDone,
              completedAtEpochMs = if (nowDone) System.currentTimeMillis() else null,
            ),
            nowDone,
          )
        }
        .testTag("complete_set_${set.id}"),
      shape = RoundedCornerShape(10.dp),
      color = if (set.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = strings.doneCol,
          tint = if (set.isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun CompactNumberField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  isDecimal: Boolean = true,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    placeholder = { Text(placeholder, style = MaterialTheme.typography.labelMedium) },
    singleLine = true,
    textStyle = MonoNumericTextStyle.copy(textAlign = TextAlign.Center),
    keyboardOptions = KeyboardOptions(
      keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number,
    ),
    modifier = modifier.height(48.dp),
  )
}

@Composable
fun WorkoutSummaryDialog(
  summary: FinishedWorkoutSummary,
  strings: AppStrings,
  prefs: UserPreferences,
  exercisesById: Map<String, ExerciseDto>,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        Icons.Filled.EmojiEvents,
        contentDescription = null,
        tint = SignalAmber,
        modifier = Modifier.size(36.dp),
      )
    },
    title = { Text(strings.workoutSummary) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(summary.session.name, style = MaterialTheme.typography.titleMedium)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(strings.duration)
          Text(WorkoutTimerService.formatClock(summary.durationSeconds), style = MonoNumericTextStyle)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(strings.totalVolume)
          Text(
            "${prefs.weightUnit.format(summary.totalVolumeKg)} ${prefs.weightUnit.symbol(prefs.language)}",
            style = MonoNumericTextStyle,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(strings.totalSets)
          Text("${summary.completedSets}", style = MonoNumericTextStyle)
        }
        if (summary.newPrExerciseIds.isNotEmpty()) {
          Spacer(Modifier.height(4.dp))
          Text(strings.personalRecords, style = MaterialTheme.typography.titleSmall, color = SignalAmber)
          summary.newPrExerciseIds.forEach { exId ->
            val name = exercisesById[exId]?.displayName(prefs.language) ?: exId
            Text("🏆 $name (${strings.newPrBadge})", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("OK") }
    },
  )
}
