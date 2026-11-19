package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserPreferences
import com.example.ironlog.core.i18n.AppStrings
import com.example.ironlog.core.math.PersonalRecord
import com.example.ironlog.core.model.EquipmentType
import com.example.ironlog.core.model.ExerciseCategory
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.IllustrationPose
import com.example.ironlog.core.model.MeasurementType
import com.example.ironlog.core.model.MuscleGroup
import com.example.ui.components.ExercisePoseAnimationCanvas
import com.example.ui.theme.FailureCoral
import com.example.ui.theme.MonoNumericTextStyle
import com.example.ui.theme.SignalAmber
import java.util.UUID

@Composable
fun CatalogScreen(
  strings: AppStrings,
  prefs: UserPreferences,
  exercises: List<ExerciseDto>,
  personalRecords: Map<String, PersonalRecord>,
  onSaveExercise: (ExerciseDto) -> Unit,
  onArchiveExercise: (ExerciseDto, Boolean) -> Unit,
  onDeleteCustomExercise: (String) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }
  var selectedEquipment by remember { mutableStateOf<EquipmentType?>(null) }
  var showArchived by remember { mutableStateOf(false) }

  var inspectingExercise by remember { mutableStateOf<ExerciseDto?>(null) }
  var editingExercise by remember { mutableStateOf<ExerciseDto?>(null) }

  val filtered = remember(exercises, searchQuery, selectedMuscle, selectedEquipment, showArchived, prefs.language) {
    val q = searchQuery.trim().lowercase()
    exercises.filter { ex ->
      (showArchived || !ex.isArchived) &&
        (selectedMuscle == null || ex.primaryMuscle == selectedMuscle || selectedMuscle in ex.secondaryMuscles) &&
        (selectedEquipment == null || ex.equipment == selectedEquipment) &&
        (q.isEmpty() || ex.nameRu.lowercase().contains(q) || ex.nameEn.lowercase().contains(q))
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingExercise = ExerciseDto(
            id = "custom_${UUID.randomUUID()}",
            nameRu = "",
            nameEn = "",
            descriptionRu = "",
            descriptionEn = "",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = EquipmentType.DUMBBELL,
            category = ExerciseCategory.COMPOUND,
            measurementType = MeasurementType.WEIGHT_REPS,
            illustrationPose = IllustrationPose.BENCH_PRESS,
            isCustom = true,
          )
        },
        modifier = Modifier.testTag("create_exercise_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = strings.createExercise)
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 720.dp),
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(strings.searchExercises) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("catalog_search_input"),
          )

          Spacer(Modifier.height(8.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = selectedMuscle == null,
              onClick = { selectedMuscle = null },
              label = { Text(strings.allMuscles) },
            )
            MuscleGroup.entries.forEach { muscle ->
              FilterChip(
                selected = selectedMuscle == muscle,
                onClick = { selectedMuscle = if (selectedMuscle == muscle) null else muscle },
                label = { Text(muscle.label(prefs.language)) },
              )
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilterChip(
              selected = selectedEquipment == null,
              onClick = { selectedEquipment = null },
              label = { Text(strings.allEquipment) },
            )
            EquipmentType.entries.forEach { eq ->
              FilterChip(
                selected = selectedEquipment == eq,
                onClick = { selectedEquipment = if (selectedEquipment == eq) null else eq },
                label = { Text(eq.label(prefs.language)) },
              )
            }
            Spacer(Modifier.width(8.dp))
            Text(strings.showArchived, style = MaterialTheme.typography.labelSmall)
            Switch(checked = showArchived, onCheckedChange = { showArchived = it })
          }
        }

        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filtered, key = { it.id }) { ex ->
            val pr = personalRecords[ex.id]
            ElevatedCard(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { inspectingExercise = ex }
                .testTag("catalog_item_${ex.id}"),
              colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
              ),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                ExercisePoseAnimationCanvas(
                  pose = ex.illustrationPose,
                  animate = false,
                  modifier = Modifier.size(60.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = ex.displayName(prefs.language),
                      style = MaterialTheme.typography.titleMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false),
                    )
                    if (ex.isCustom) {
                      Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp),
                      ) {
                        Text(
                          text = "CUSTOM",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.tertiary,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                      }
                    }
                  }
                  Text(
                    text = "${ex.primaryMuscle.label(prefs.language)} • ${ex.equipment.label(prefs.language)} • ${ex.category.label(prefs.language)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = ex.measurementType.label(prefs.language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                if (pr != null && pr.maxWeightKg > 0.0) {
                  Column(horizontalAlignment = Alignment.End) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = SignalAmber, modifier = Modifier.size(16.dp))
                    Text(
                      text = "${prefs.weightUnit.format(pr.maxWeightKg)}${prefs.weightUnit.symbol(prefs.language)}",
                      style = MonoNumericTextStyle,
                      color = SignalAmber,
                    )
                  }
                }
              }
            }
          }
          item { Spacer(Modifier.height(72.dp)) }
        }
      }
    }
  }

  inspectingExercise?.let { ex ->
    val pr = personalRecords[ex.id]
    AlertDialog(
      onDismissRequest = { inspectingExercise = null },
      title = { Text(ex.displayName(prefs.language)) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ExercisePoseAnimationCanvas(
            pose = ex.illustrationPose,
            animate = true,
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp),
          )
          Text(
            text = "${ex.primaryMuscle.label(prefs.language)} • ${ex.equipment.label(prefs.language)} • ${ex.measurementType.label(prefs.language)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(strings.techniqueTitle, style = MaterialTheme.typography.titleSmall)
          Text(ex.displayDescription(prefs.language), style = MaterialTheme.typography.bodyMedium)
          if (pr != null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(strings.personalRecords, style = MaterialTheme.typography.titleSmall, color = SignalAmber)
                Text(
                  text = "Max: ${prefs.weightUnit.format(pr.maxWeightKg)} ${prefs.weightUnit.symbol(prefs.language)}  •  1RM: ${prefs.weightUnit.format(pr.estimated1RmKg)} ${prefs.weightUnit.symbol(prefs.language)}",
                  style = MonoNumericTextStyle,
                )
              }
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (ex.isCustom) {
              IconButton(
                onClick = {
                  inspectingExercise = null
                  editingExercise = ex
                },
              ) { Icon(Icons.Filled.Edit, contentDescription = strings.editExercise) }
              IconButton(
                onClick = {
                  onDeleteCustomExercise(ex.id)
                  inspectingExercise = null
                },
              ) { Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = FailureCoral) }
            }
            IconButton(
              onClick = {
                onArchiveExercise(ex, !ex.isArchived)
                inspectingExercise = null
              },
            ) {
              Icon(
                if (ex.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                contentDescription = if (ex.isArchived) strings.unarchive else strings.archive,
              )
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { inspectingExercise = null }) { Text("OK") }
      },
    )
  }

  editingExercise?.let { draft ->
    ExerciseEditorDialog(
      initial = draft,
      strings = strings,
      prefs = prefs,
      onDismiss = { editingExercise = null },
      onSave = { updated ->
        onSaveExercise(updated)
        editingExercise = null
      },
    )
  }
}

@Composable
private fun ExerciseEditorDialog(
  initial: ExerciseDto,
  strings: AppStrings,
  prefs: UserPreferences,
  onDismiss: () -> Unit,
  onSave: (ExerciseDto) -> Unit,
) {
  var nameRu by remember { mutableStateOf(initial.nameRu) }
  var nameEn by remember { mutableStateOf(initial.nameEn) }
  var descRu by remember { mutableStateOf(initial.descriptionRu) }
  var descEn by remember { mutableStateOf(initial.descriptionEn) }
  var muscle by remember { mutableStateOf(initial.primaryMuscle) }
  var equipment by remember { mutableStateOf(initial.equipment) }
  var category by remember { mutableStateOf(initial.category) }
  var measurement by remember { mutableStateOf(initial.measurementType) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial.nameRu.isBlank() && initial.nameEn.isBlank()) strings.createExercise else strings.editExercise) },
    text = {
      Column(
        modifier = Modifier
          .height(400.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(value = nameRu, onValueChange = { nameRu = it }, label = { Text(strings.nameRuLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text(strings.nameEnLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = descRu, onValueChange = { descRu = it }, label = { Text(strings.descRuLabel) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = descEn, onValueChange = { descEn = it }, label = { Text(strings.descEnLabel) }, modifier = Modifier.fillMaxWidth())

        Text(strings.primaryMuscleLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          MuscleGroup.entries.forEach { m ->
            FilterChip(selected = muscle == m, onClick = { muscle = m }, label = { Text(m.label(prefs.language)) })
          }
        }

        Text(strings.equipmentLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          EquipmentType.entries.forEach { eq ->
            FilterChip(selected = equipment == eq, onClick = { equipment = eq }, label = { Text(eq.label(prefs.language)) })
          }
        }

        Text(strings.categoryLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ExerciseCategory.entries.forEach { cat ->
            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label(prefs.language)) })
          }
        }

        Text(strings.measurementTypeLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          MeasurementType.entries.forEach { mt ->
            FilterChip(selected = measurement == mt, onClick = { measurement = mt }, label = { Text(mt.label(prefs.language)) })
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (nameRu.isNotBlank() || nameEn.isNotBlank()) {
            onSave(
              initial.copy(
                nameRu = nameRu.ifBlank { nameEn }.trim(),
                nameEn = nameEn.ifBlank { nameRu }.trim(),
                descriptionRu = descRu.ifBlank { descEn }.trim(),
                descriptionEn = descEn.ifBlank { descRu }.trim(),
                primaryMuscle = muscle,
                equipment = equipment,
                category = category,
                measurementType = measurement,
                isCustom = true,
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
}
