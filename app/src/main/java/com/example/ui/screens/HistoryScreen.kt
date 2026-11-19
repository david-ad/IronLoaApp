package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserPreferences
import com.example.ironlog.core.i18n.AppStrings
import com.example.ironlog.core.math.StrengthCalculators
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.service.WorkoutTimerService
import com.example.ui.theme.FailureCoral
import com.example.ui.theme.MonoNumericTextStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
  strings: AppStrings,
  prefs: UserPreferences,
  exercises: List<ExerciseDto>,
  completedWorkouts: List<WorkoutSessionDto>,
  onUpdateWorkout: (WorkoutSessionDto) -> Unit,
  onDeleteWorkout: (WorkoutSessionDto) -> Unit,
) {
  var showCalendar by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedDayKey by remember { mutableStateOf<String?>(null) }
  var inspectingSession by remember { mutableStateOf<WorkoutSessionDto?>(null) }
  var editingSession by remember { mutableStateOf<WorkoutSessionDto?>(null) }

  val exercisesById = remember(exercises) { exercises.associateBy { it.id } }
  val measurementMap = remember(exercises) { exercises.associate { it.id to it.measurementType } }
  val dayKeyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
  val displayDateFmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

  val filteredWorkouts = remember(completedWorkouts, searchQuery, selectedDayKey, exercisesById, prefs.language) {
    val q = searchQuery.trim().lowercase()
    completedWorkouts.filter { session ->
      val dayMatches = selectedDayKey == null || dayKeyFmt.format(Date(session.startedAtEpochMs)) == selectedDayKey
      val queryMatches = q.isEmpty() ||
        session.name.lowercase().contains(q) ||
        session.exercises.any { ex ->
          val name = exercisesById[ex.exerciseId]?.displayName(prefs.language) ?: ""
          name.lowercase().contains(q)
        }
      dayMatches && queryMatches
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 720.dp),
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(strings.historyTitle, style = MaterialTheme.typography.headlineSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = !showCalendar,
              onClick = { showCalendar = false },
              label = { Text(strings.listTab) },
              leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            FilterChip(
              selected = showCalendar,
              onClick = { showCalendar = true },
              label = { Text(strings.calendarTab) },
              leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
          }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          placeholder = { Text(strings.searchHistory) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("history_search_input"),
        )
      }

      if (showCalendar) {
        val workoutDays = remember(completedWorkouts) {
          completedWorkouts.map { dayKeyFmt.format(Date(it.startedAtEpochMs)) }.toSet()
        }
        WorkoutCalendarGrid(
          workoutDays = workoutDays,
          selectedDayKey = selectedDayKey,
          onSelectDay = { dayKey ->
            selectedDayKey = if (selectedDayKey == dayKey) null else dayKey
          },
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }

      if (filteredWorkouts.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.Filled.FitnessCenter,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text(
              text = strings.noWorkoutsYet,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filteredWorkouts, key = { it.id }) { session ->
            val end = session.finishedAtEpochMs ?: session.startedAtEpochMs
            val durSec = ((end - session.startedAtEpochMs - session.pausedDurationMs) / 1000L).coerceAtLeast(0L)
            val volKg = StrengthCalculators.sessionVolumeKg(session, measurementMap)
            val completedSets = session.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

            ElevatedCard(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { inspectingSession = session }
                .testTag("history_card_${session.id}"),
              colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
              ),
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = session.name,
                      style = MaterialTheme.typography.titleMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = displayDateFmt.format(Date(session.startedAtEpochMs)),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  if (session.sourceDevice == "wear") {
                    Icon(
                      Icons.Filled.Watch,
                      contentDescription = "Wear OS",
                      tint = MaterialTheme.colorScheme.tertiary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Text(
                    text = "⏱ ${WorkoutTimerService.formatClock(durSec)}",
                    style = MonoNumericTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "${prefs.weightUnit.format(volKg)} ${prefs.weightUnit.symbol(prefs.language)}",
                    style = MonoNumericTextStyle,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = "$completedSets ${strings.setsSuffix}",
                    style = MonoNumericTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (session.avgHeartRate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                      Icon(Icons.Filled.Favorite, contentDescription = null, tint = FailureCoral, modifier = Modifier.size(14.dp))
                      Text("${session.avgHeartRate}", style = MonoNumericTextStyle, color = FailureCoral)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  inspectingSession?.let { session ->
    val previousVolKg = remember(session.id, completedWorkouts) {
      val earlier = completedWorkouts
        .filter { it.id != session.id && it.startedAtEpochMs < session.startedAtEpochMs && (session.routineId == null || it.routineId == session.routineId) }
        .maxByOrNull { it.startedAtEpochMs }
      earlier?.let { StrengthCalculators.sessionVolumeKg(it, measurementMap) }
    }
    val curVolKg = StrengthCalculators.sessionVolumeKg(session, measurementMap)

    AlertDialog(
      onDismissRequest = { inspectingSession = null },
      title = { Text(session.name) },
      text = {
        Column(
          modifier = Modifier
            .height(380.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = displayDateFmt.format(Date(session.startedAtEpochMs)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (previousVolKg != null && previousVolKg > 0.0) {
            val deltaPct = (((curVolKg - previousVolKg) / previousVolKg) * 100.0).toInt()
            val sign = if (deltaPct >= 0) "+" else ""
            Text(
              text = "${strings.comparePrevSession}: $sign$deltaPct% (${prefs.weightUnit.format(curVolKg)} vs ${prefs.weightUnit.format(previousVolKg)} ${prefs.weightUnit.symbol(prefs.language)})",
              style = MaterialTheme.typography.labelLarge,
              color = if (deltaPct >= 0) MaterialTheme.colorScheme.primary else FailureCoral,
            )
          }
          if (session.avgHeartRate != null || session.maxHeartRate != null) {
            Text(
              text = "${strings.heartRateLabel}: avg ${session.avgHeartRate ?: "—"} / max ${session.maxHeartRate ?: "—"} bpm",
              style = MaterialTheme.typography.labelMedium,
              color = FailureCoral,
            )
          }
          if (session.notes.isNotBlank()) {
            Text(session.notes, style = MaterialTheme.typography.bodySmall)
          }

          session.exercises.forEach { ex ->
            val exName = exercisesById[ex.exerciseId]?.displayName(prefs.language) ?: ex.exerciseId
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(exName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                ex.sets.forEachIndexed { i, s ->
                  Text(
                    text = "${i + 1}. ${prefs.weightUnit.format(s.weightKg)} ${prefs.weightUnit.symbol(prefs.language)} × ${s.reps}" +
                      (if (s.durationSeconds > 0) " • ${s.durationSeconds}s" else "") +
                      (if (s.rpe != null) " • RPE ${s.rpe}" else ""),
                    style = MonoNumericTextStyle,
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(
            onClick = {
              onDeleteWorkout(session)
              inspectingSession = null
            },
          ) {
            Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = FailureCoral)
          }
          TextButton(
            onClick = {
              editingSession = session
              inspectingSession = null
            },
          ) {
            Icon(Icons.Filled.Edit, contentDescription = strings.edit)
            Spacer(Modifier.width(4.dp))
            Text(strings.edit)
          }
          Button(onClick = { inspectingSession = null }) { Text("OK") }
        }
      },
    )
  }

  editingSession?.let { draft ->
    EditCompletedWorkoutDialog(
      initial = draft,
      strings = strings,
      prefs = prefs,
      exercisesById = exercisesById,
      onDismiss = { editingSession = null },
      onSave = { updated ->
        onUpdateWorkout(updated)
        editingSession = null
      },
    )
  }
}

@Composable
private fun WorkoutCalendarGrid(
  workoutDays: Set<String>,
  selectedDayKey: String?,
  onSelectDay: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var monthOffset by remember { mutableIntStateOf(0) }
  val cal = remember(monthOffset) {
    Calendar.getInstance().apply {
      add(Calendar.MONTH, monthOffset)
      set(Calendar.DAY_OF_MONTH, 1)
    }
  }
  val monthTitle = remember(monthOffset) {
    SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(cal.time)
  }
  val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val firstDow = ((cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY) + 7) % 7
  val year = cal.get(Calendar.YEAR)
  val month = cal.get(Calendar.MONTH) + 1

  ElevatedCard(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = { monthOffset-- }) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
        }
        Text(monthTitle.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { monthOffset++ }) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
        }
      }
      val totalCells = firstDow + daysInMonth
      val rows = (totalCells + 6) / 7
      for (r in 0 until rows) {
        Row(modifier = Modifier.fillMaxWidth()) {
          for (c in 0 until 7) {
            val cellIndex = r * 7 + c
            val day = cellIndex - firstDow + 1
            if (day in 1..daysInMonth) {
              val key = "%04d-%02d-%02d".format(year, month, day)
              val hasWorkout = key in workoutDays
              val isSelected = key == selectedDayKey
              Box(
                modifier = Modifier
                  .weight(1f)
                  .aspectRatio(1.1f)
                  .padding(2.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    when {
                      isSelected -> MaterialTheme.colorScheme.primary
                      hasWorkout -> MaterialTheme.colorScheme.primaryContainer
                      else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                  )
                  .clickable { onSelectDay(key) },
                contentAlignment = Alignment.Center,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "$day",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  )
                  if (hasWorkout) {
                    Box(
                      modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary),
                    )
                  }
                }
              }
            } else {
              Spacer(Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EditCompletedWorkoutDialog(
  initial: WorkoutSessionDto,
  strings: AppStrings,
  prefs: UserPreferences,
  exercisesById: Map<String, ExerciseDto>,
  onDismiss: () -> Unit,
  onSave: (WorkoutSessionDto) -> Unit,
) {
  var name by remember { mutableStateOf(initial.name) }
  var notes by remember { mutableStateOf(initial.notes) }
  var exercises by remember { mutableStateOf(initial.exercises) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.edit) },
    text = {
      Column(
        modifier = Modifier
          .height(380.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.routineNameLabel) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(strings.notesHint) }, modifier = Modifier.fillMaxWidth())

        exercises.forEachIndexed { exIdx, ex ->
          val title = exercisesById[ex.exerciseId]?.displayName(prefs.language) ?: ex.exerciseId
          Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
          ex.sets.forEachIndexed { setIdx, set ->
            var wTxt by remember(set.id) { mutableStateOf(prefs.weightUnit.format(set.weightKg)) }
            var rTxt by remember(set.id) { mutableStateOf(set.reps.toString()) }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("#${setIdx + 1}", style = MonoNumericTextStyle, modifier = Modifier.width(28.dp))
              OutlinedTextField(
                value = wTxt,
                onValueChange = { txt ->
                  wTxt = txt
                  val kg = prefs.weightUnit.toKg(txt.replace(',', '.').toDoubleOrNull() ?: 0.0)
                  exercises = exercises.mapIndexed { ei, we ->
                    if (ei != exIdx) we else we.copy(
                      sets = we.sets.mapIndexed { si, s -> if (si == setIdx) s.copy(weightKg = kg) else s }
                    )
                  }
                },
                label = { Text(prefs.weightUnit.symbol(prefs.language)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = rTxt,
                onValueChange = { txt ->
                  rTxt = txt
                  val reps = txt.toIntOrNull() ?: 0
                  exercises = exercises.mapIndexed { ei, we ->
                    if (ei != exIdx) we else we.copy(
                      sets = we.sets.mapIndexed { si, s -> if (si == setIdx) s.copy(reps = reps) else s }
                    )
                  }
                },
                label = { Text(strings.repsCol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onSave(initial.copy(name = name, notes = notes, exercises = exercises)) }) {
        Text(strings.save)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel) }
    },
  )
}
