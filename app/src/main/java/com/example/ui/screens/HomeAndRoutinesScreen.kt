package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserPreferences
import com.example.ironlog.core.i18n.AppStrings
import com.example.ironlog.core.math.StrengthCalculators
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.RoutineDto
import com.example.ironlog.core.model.RoutineExerciseDto
import com.example.ironlog.core.model.RoutineFolderDto
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.service.WorkoutTimerService
import com.example.ui.theme.MonoNumericTextStyle
import java.util.UUID

@Composable
fun HomeAndRoutinesScreen(
  strings: AppStrings,
  prefs: UserPreferences,
  exercises: List<ExerciseDto>,
  folders: List<RoutineFolderDto>,
  routines: List<RoutineDto>,
  activeWorkout: WorkoutSessionDto?,
  activeElapsedSec: Long,
  completedWorkouts: List<WorkoutSessionDto>,
  onStartEmptyWorkout: () -> Unit,
  onResumeActiveWorkout: () -> Unit,
  onStartRoutine: (RoutineDto) -> Unit,
  onSaveRoutine: (RoutineDto) -> Unit,
  onDuplicateRoutine: (RoutineDto) -> Unit,
  onDeleteRoutine: (RoutineDto) -> Unit,
  onSaveFolder: (RoutineFolderDto) -> Unit,
  onOpenSettings: () -> Unit,
) {
  var selectedFolderId by remember { mutableStateOf<String?>(null) }
  var editingRoutine by remember { mutableStateOf<RoutineDto?>(null) }
  var showNewFolderDialog by remember { mutableStateOf(false) }

  val exercisesById = remember(exercises) { exercises.associateBy { it.id } }
  val measurementMap = remember(exercises) { exercises.associate { it.id to it.measurementType } }

  val filteredRoutines = remember(routines, selectedFolderId) {
    if (selectedFolderId == null) routines else routines.filter { it.folderId == selectedFolderId }
  }

  val sevenDaysAgo = remember { System.currentTimeMillis() - 7L * 24L * 3600L * 1000L }
  val weeklySessions = remember(completedWorkouts, sevenDaysAgo) {
    completedWorkouts.filter { it.startedAtEpochMs >= sevenDaysAgo }
  }
  val weeklyVolumeKg = remember(weeklySessions, measurementMap) {
    weeklySessions.sumOf { StrengthCalculators.sessionVolumeKg(it, measurementMap) }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 720.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Hero Header Banner
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
              Brush.linearGradient(
                listOf(
                  MaterialTheme.colorScheme.surfaceContainerHigh,
                  MaterialTheme.colorScheme.surface,
                )
              )
            ),
        ) {
          val accent = MaterialTheme.colorScheme.primary
          Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.9f, size.height * 0.15f),
                radius = size.width * 0.55f,
              ),
              center = Offset(size.width * 0.9f, size.height * 0.15f),
              radius = size.width * 0.55f,
            )
          }

          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.FitnessCenter,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = strings.appName,
                  style = MaterialTheme.typography.headlineMedium,
                )
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(99.dp),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.OfflineBolt,
                      contentDescription = null,
                      modifier = Modifier.size(12.dp),
                      tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      text = strings.offlineReadyBadge,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }

              IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("settings_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Settings,
                  contentDescription = strings.settingsTitle,
                )
              }
            }

            Spacer(Modifier.height(14.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              MetricMiniPill(
                label = strings.workoutsCount,
                value = "${completedWorkouts.size}",
                modifier = Modifier.weight(1f),
              )
              MetricMiniPill(
                label = strings.weeklyVolumeLabel,
                value = "${prefs.weightUnit.format(weeklyVolumeKg)} ${prefs.weightUnit.symbol(prefs.language)}",
                modifier = Modifier.weight(1f),
              )
            }
            Spacer(Modifier.height(14.dp))

            if (activeWorkout != null) {
              Button(
                onClick = onResumeActiveWorkout,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("resume_workout_button"),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondary,
                  contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
              ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "${strings.resumeWorkout} • ${WorkoutTimerService.formatClock(activeElapsedSec)}",
                  fontWeight = FontWeight.Bold,
                )
              }
            } else {
              Button(
                onClick = onStartEmptyWorkout,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("start_empty_workout_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.startEmptyWorkout, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Routines Header + Actions
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(strings.routinesTitle, style = MaterialTheme.typography.titleLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
              onClick = { showNewFolderDialog = true },
              modifier = Modifier.testTag("new_folder_button"),
            ) {
              Icon(Icons.Filled.CreateNewFolder, contentDescription = strings.newFolder)
            }
            OutlinedButton(
              onClick = {
                editingRoutine = RoutineDto(
                  id = UUID.randomUUID().toString(),
                  folderId = selectedFolderId,
                  name = "",
                  description = "",
                )
              },
              modifier = Modifier.testTag("new_routine_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text(strings.newRoutine)
            }
          }
        }
      }

      // Folder Filter Row
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedFolderId == null,
            onClick = { selectedFolderId = null },
            label = { Text(strings.allFolders) },
            leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
          )
          folders.forEach { folder ->
            FilterChip(
              selected = selectedFolderId == folder.id,
              onClick = { selectedFolderId = if (selectedFolderId == folder.id) null else folder.id },
              label = { Text(folder.name) },
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(folder.colorHex)),
                )
              },
            )
          }
        }
      }

      // Routines List
      items(filteredRoutines, key = { it.id }) { routine ->
        RoutineCard(
          routine = routine,
          strings = strings,
          language = prefs.language,
          exercisesById = exercisesById,
          folderName = folders.firstOrNull { it.id == routine.folderId }?.name,
          onStart = { onStartRoutine(routine) },
          onEdit = { editingRoutine = routine },
          onDuplicate = { onDuplicateRoutine(routine) },
          onDelete = { onDeleteRoutine(routine) },
        )
      }

      item { Spacer(Modifier.height(24.dp)) }
    }
  }

  if (showNewFolderDialog) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showNewFolderDialog = false },
      title = { Text(strings.newFolder) },
      text = {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(strings.folderName) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.isNotBlank()) {
              onSaveFolder(
                RoutineFolderDto(
                  id = UUID.randomUUID().toString(),
                  name = name.trim(),
                  colorHex = 0xFFC8FF2E,
                  sortOrder = folders.size,
                )
              )
              showNewFolderDialog = false
            }
          }
        ) { Text(strings.save) }
      },
      dismissButton = {
        TextButton(onClick = { showNewFolderDialog = false }) { Text(strings.cancel) }
      },
    )
  }

  editingRoutine?.let { routineDraft ->
    RoutineEditorDialog(
      initialRoutine = routineDraft,
      strings = strings,
      prefs = prefs,
      folders = folders,
      catalogExercises = exercises.filter { !it.isArchived },
      onDismiss = { editingRoutine = null },
      onSave = { updated ->
        onSaveRoutine(updated)
        editingRoutine = null
      },
    )
  }
}

@Composable
private fun MetricMiniPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = value,
        style = MonoNumericTextStyle,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun RoutineCard(
  routine: RoutineDto,
  strings: AppStrings,
  language: com.example.ironlog.core.model.AppLanguage,
  exercisesById: Map<String, ExerciseDto>,
  folderName: String?,
  onStart: () -> Unit,
  onEdit: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }

  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("routine_card_${routine.id}"),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          if (!folderName.isNullOrBlank()) {
            Text(
              text = folderName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.tertiary,
            )
          }
          Text(
            text = routine.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (routine.description.isNotBlank()) {
            Text(
              text = routine.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Box {
          IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = strings.edit)
          }
          DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
              text = { Text(strings.edit) },
              leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
              onClick = {
                menuOpen = false
                onEdit()
              },
            )
            DropdownMenuItem(
              text = { Text(strings.duplicate) },
              leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
              onClick = {
                menuOpen = false
                onDuplicate()
              },
            )
            DropdownMenuItem(
              text = { Text(strings.delete) },
              leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
              onClick = {
                menuOpen = false
                onDelete()
              },
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))
      routine.exercises.take(4).forEach { rEx ->
        val exName = exercisesById[rEx.exerciseId]?.displayName(language) ?: rEx.exerciseId
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "• $exName" + if (rEx.supersetGroupId != null) " [${strings.supersetBadge} ${rEx.supersetGroupId}]" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${rEx.targetSets}×${rEx.targetReps}",
            style = MonoNumericTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (routine.exercises.size > 4) {
        Text(
          text = "+${routine.exercises.size - 4}...",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(Modifier.height(12.dp))
      Button(
        onClick = onStart,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("start_routine_${routine.id}"),
      ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(strings.startNow, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun RoutineEditorDialog(
  initialRoutine: RoutineDto,
  strings: AppStrings,
  prefs: UserPreferences,
  folders: List<RoutineFolderDto>,
  catalogExercises: List<ExerciseDto>,
  onDismiss: () -> Unit,
  onSave: (RoutineDto) -> Unit,
) {
  var name by remember { mutableStateOf(initialRoutine.name) }
  var description by remember { mutableStateOf(initialRoutine.description) }
  var folderId by remember { mutableStateOf(initialRoutine.folderId) }
  var items by remember { mutableStateOf(initialRoutine.exercises) }
  var showPicker by remember { mutableStateOf(false) }

  val catalogMap = remember(catalogExercises) { catalogExercises.associateBy { it.id } }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initialRoutine.name.isBlank()) strings.newRoutine else strings.edit) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(420.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(strings.routineNameLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text(strings.routineDescLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (folders.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = folderId == null,
              onClick = { folderId = null },
              label = { Text(strings.allFolders) },
            )
            folders.forEach { f ->
              FilterChip(
                selected = folderId == f.id,
                onClick = { folderId = f.id },
                label = { Text(f.name) },
              )
            }
          }
        }

        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(items.size) { idx ->
            val item = items[idx]
            val exName = catalogMap[item.exerciseId]?.displayName(prefs.language) ?: item.exerciseId
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = exName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row {
                    if (idx > 0) {
                      IconButton(
                        onClick = {
                          val m = items.toMutableList()
                          val cur = m.removeAt(idx)
                          m.add(idx - 1, cur)
                          items = m
                        },
                        modifier = Modifier.size(32.dp),
                      ) { Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    }
                    if (idx + 1 < items.size) {
                      IconButton(
                        onClick = {
                          val m = items.toMutableList()
                          val cur = m.removeAt(idx)
                          m.add(idx + 1, cur)
                          items = m
                        },
                        modifier = Modifier.size(32.dp),
                      ) { Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                      IconButton(
                        onClick = {
                          val grp = if (item.supersetGroupId != null && item.supersetGroupId == items[idx + 1].supersetGroupId) {
                            null
                          } else {
                            item.supersetGroupId ?: ((items.mapNotNull { it.supersetGroupId }.maxOrNull() ?: 0) + 1)
                          }
                          items = items.mapIndexed { i, re ->
                            if (i == idx || i == idx + 1) re.copy(supersetGroupId = grp) else re
                          }
                        },
                        modifier = Modifier.size(32.dp),
                      ) {
                        Icon(
                          Icons.Filled.Link,
                          contentDescription = strings.linkSuperset,
                          tint = if (item.supersetGroupId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    }
                    IconButton(
                      onClick = { items = items.filterIndexed { i, _ -> i != idx } },
                      modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Filled.Delete, contentDescription = strings.delete, modifier = Modifier.size(16.dp)) }
                  }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  CompactStepper(
                    label = strings.targetSetsLabel,
                    value = "${item.targetSets}",
                    onMinus = {
                      items = items.mapIndexed { i, re ->
                        if (i == idx) re.copy(targetSets = (re.targetSets - 1).coerceAtLeast(1)) else re
                      }
                    },
                    onPlus = {
                      items = items.mapIndexed { i, re ->
                        if (i == idx) re.copy(targetSets = re.targetSets + 1) else re
                      }
                    },
                    modifier = Modifier.weight(1f),
                  )
                  CompactStepper(
                    label = strings.targetRepsLabel,
                    value = "${item.targetReps}",
                    onMinus = {
                      items = items.mapIndexed { i, re ->
                        if (i == idx) re.copy(targetReps = (re.targetReps - 1).coerceAtLeast(1)) else re
                      }
                    },
                    onPlus = {
                      items = items.mapIndexed { i, re ->
                        if (i == idx) re.copy(targetReps = re.targetReps + 1) else re
                      }
                    },
                    modifier = Modifier.weight(1f),
                  )
                  CompactStepper(
                    label = strings.restTimer,
                    value = "${item.restSeconds}s",
                    onMinus = {
                      items = items.mapIndexed { i, re ->
                        if (i == idx) re.copy(restSeconds = (re.restSeconds - 15).coerceAtLeast(15)) else re
                      }
                    },
                    onPlus = {
                      items = items.mapIndexed { i, re ->
                        if (i == idx) re.copy(restSeconds = re.restSeconds + 15) else re
                      }
                    },
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }
          }
        }

        OutlinedButton(
          onClick = { showPicker = true },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(strings.addExercise)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onSave(
              initialRoutine.copy(
                name = name.trim(),
                description = description.trim(),
                folderId = folderId,
                exercises = items,
              )
            )
          }
        }
      ) { Text(strings.save) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel) }
    },
  )

  if (showPicker) {
    ExercisePickerDialog(
      exercises = catalogExercises,
      strings = strings,
      language = prefs.language,
      onDismiss = { showPicker = false },
      onSelect = { ex ->
        items = items + RoutineExerciseDto(
          id = UUID.randomUUID().toString(),
          routineId = initialRoutine.id,
          exerciseId = ex.id,
          sortOrder = items.size,
          targetSets = 3,
          targetReps = 10,
          targetWeightKg = 0.0,
          restSeconds = 90,
        )
        showPicker = false
      },
    )
  }
}

@Composable
private fun CompactStepper(
  label: String,
  value: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "−",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
          .clickable(onClick = onMinus)
          .padding(horizontal = 6.dp, vertical = 2.dp),
      )
      Text(value, style = MonoNumericTextStyle)
      Text(
        text = "+",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
          .clickable(onClick = onPlus)
          .padding(horizontal = 6.dp, vertical = 2.dp),
      )
    }
  }
}

@Composable
fun ExercisePickerDialog(
  exercises: List<ExerciseDto>,
  strings: AppStrings,
  language: com.example.ironlog.core.model.AppLanguage,
  onDismiss: () -> Unit,
  onSelect: (ExerciseDto) -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val filtered = remember(exercises, query, language) {
    val q = query.trim().lowercase()
    if (q.isEmpty()) exercises
    else exercises.filter {
      it.nameRu.lowercase().contains(q) ||
        it.nameEn.lowercase().contains(q) ||
        it.primaryMuscle.label(language).lowercase().contains(q)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addExercise) },
    text = {
      Column(modifier = Modifier.height(380.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text(strings.searchExercises) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items(filtered, key = { it.id }) { ex ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(ex) },
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(ex.displayName(language), style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "${ex.primaryMuscle.label(language)} • ${ex.equipment.label(language)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel) }
    },
  )
}
