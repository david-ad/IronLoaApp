package com.example.ironlog.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.ironlog.core.model.AppLanguage
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.RoutineDto
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ironlog.wear.ui.WearViewModel
import java.util.Locale

class WearMainActivity : ComponentActivity() {
  private val wearViewModel: WearViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      WearIronLogApp(viewModel = wearViewModel)
    }
  }

  // Hardware stem button support: STEM_1 toggles pause/resume during active workout
  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_STEM_1 && wearViewModel.activeSession.value != null) {
      wearViewModel.togglePause()
      return true
    }
    return super.onKeyDown(keyCode, event)
  }
}

private val WearAthleticColorScheme = ColorScheme(
  primary = Color(0xFFC8FF2E),
  onPrimary = Color(0xFF0B1002),
  primaryContainer = Color(0xFF2B3B0A),
  onPrimaryContainer = Color(0xFFC8FF2E),
  secondary = Color(0xFFFFB020),
  onSecondary = Color(0xFF1F1200),
  tertiary = Color(0xFF38BDF8),
  onTertiary = Color(0xFF001C29),
  background = Color(0xFF0D0F14),
  onBackground = Color(0xFFF3F5F8),
  surfaceContainer = Color(0xFF1E2430),
  onSurface = Color(0xFFF3F5F8),
  onSurfaceVariant = Color(0xFF9AA4B8),
  error = Color(0xFFFF5C5C),
)

@Composable
fun WearIronLogApp(viewModel: WearViewModel) {
  val exercises by viewModel.exercises.collectAsStateWithLifecycle()
  val routines by viewModel.routines.collectAsStateWithLifecycle()
  val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
  val elapsedSec by viewModel.elapsedSec.collectAsStateWithLifecycle()
  val restRemaining by viewModel.restRemainingSec.collectAsStateWithLifecycle()
  val heartRate by viewModel.currentHeartRate.collectAsStateWithLifecycle()
  val lastSummary by viewModel.lastSummary.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val hrPermission = if (Build.VERSION.SDK_INT >= 36) {
    "android.permission.health.READ_HEART_RATE"
  } else {
    Manifest.permission.BODY_SENSORS
  }
  val hrLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) viewModel.startHeartRateCollection()
  }

  LaunchedEffect(activeSession?.id) {
    if (activeSession != null) {
      val granted = ContextCompat.checkSelfPermission(context, hrPermission) == PackageManager.PERMISSION_GRANTED
      if (granted) {
        viewModel.startHeartRateCollection()
      } else {
        hrLauncher.launch(hrPermission)
      }
    }
  }

  val lang = remember {
    if (Locale.getDefault().language.startsWith("ru")) AppLanguage.RU else AppLanguage.EN
  }

  MaterialTheme(colorScheme = WearAthleticColorScheme) {
    AppScaffold(timeText = { TimeText() }) {
      val summary = lastSummary
      val active = activeSession
      when {
        summary != null -> {
          WearSummaryScreen(
            session = summary,
            lang = lang,
            onDone = viewModel::dismissSummary,
          )
        }
        active != null -> {
          WearActiveWorkoutPager(
            session = active,
            exercises = exercises,
            elapsedSec = elapsedSec,
            restRemaining = restRemaining,
            heartRate = heartRate,
            lang = lang,
            viewModel = viewModel,
          )
        }
        else -> {
          WearHomeScreen(
            routines = routines,
            lang = lang,
            onStartEmpty = viewModel::startEmpty,
            onStartRoutine = viewModel::startRoutine,
            onSyncNow = viewModel::syncNow,
          )
        }
      }
    }
  }
}

@Composable
private fun WearHomeScreen(
  routines: List<RoutineDto>,
  lang: AppLanguage,
  onStartEmpty: () -> Unit,
  onStartRoutine: (RoutineDto) -> Unit,
  onSyncNow: () -> Unit,
) {
  val columnState = rememberTransformingLazyColumnState()
  val transformationSpec = rememberTransformationSpec()

  ScreenScaffold(scrollState = columnState) { contentPadding ->
    TransformingLazyColumn(
      state = columnState,
      contentPadding = contentPadding,
    ) {
      item {
        ListHeader(
          modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec)
            .padding(vertical = 4.dp),
          transformation = SurfaceTransformation(transformationSpec),
        ) {
          Text("IronLog")
        }
      }

      item {
        Button(
          modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec)
            .padding(vertical = 2.dp)
            .testTag("wear_start_empty"),
          transformation = SurfaceTransformation(transformationSpec),
          onClick = onStartEmpty,
          icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        ) {
          Text(
            text = if (lang == AppLanguage.RU) "Пустая тренировка" else "Empty Workout",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      items(routines, key = { it.id }) { routine ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec)
            .padding(vertical = 2.dp)
            .testTag("wear_routine_${routine.id}"),
          transformation = SurfaceTransformation(transformationSpec),
          onClick = { onStartRoutine(routine) },
        ) {
          Text(routine.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(
            text = "${routine.exercises.size} ${if (lang == AppLanguage.RU) "упр." else "ex."}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item {
        OutlinedButton(
          modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec)
            .padding(vertical = 2.dp),
          transformation = SurfaceTransformation(transformationSpec),
          onClick = onSyncNow,
          icon = { Icon(Icons.Filled.Sync, contentDescription = null) },
        ) {
          Text(if (lang == AppLanguage.RU) "Синхронизация" else "Sync Phone", maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun WearActiveWorkoutPager(
  session: WorkoutSessionDto,
  exercises: List<ExerciseDto>,
  elapsedSec: Long,
  restRemaining: Int,
  heartRate: Int?,
  lang: AppLanguage,
  viewModel: WearViewModel,
) {
  val exercisesById = remember(exercises) { exercises.associateBy { it.id } }
  val pageCount = session.exercises.size + 1
  val pagerState = rememberPagerState(pageCount = { pageCount })

  HorizontalPagerScaffold(pagerState = pagerState) {
    HorizontalPager(state = pagerState) { page ->
      AnimatedPage(pageIndex = page, pagerState = pagerState) {
        if (page < session.exercises.size) {
          val wEx = session.exercises[page]
          val meta = exercisesById[wEx.exerciseId]
          val exName = meta?.displayName(lang) ?: wEx.exerciseId
          val prevSets = remember(wEx.exerciseId) { viewModel.previousSetsFor(wEx.exerciseId) }
          var selectedSetIdx by remember(wEx.id, wEx.sets.size) {
            mutableIntStateOf(
              wEx.sets.indexOfFirst { !it.isCompleted }.let { if (it >= 0) it else 0 }
            )
          }
          val activeSet = wEx.sets.getOrNull(selectedSetIdx)

          val columnState = rememberTransformingLazyColumnState()
          val spec = rememberTransformationSpec()

          ScreenScaffold(scrollState = columnState) { contentPadding ->
            TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
              item {
                ListHeader(
                  modifier = Modifier
                    .fillMaxWidth()
                    .transformedHeight(this, spec)
                    .padding(vertical = 4.dp),
                  transformation = SurfaceTransformation(spec),
                ) {
                  Text(exName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }

              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = formatWearClock(elapsedSec),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  if (heartRate != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("$heartRate", style = MaterialTheme.typography.labelMedium)
                  }
                }
              }

              if (restRemaining > 0) {
                item {
                  Button(
                    modifier = Modifier
                      .fillMaxWidth()
                      .transformedHeight(this, spec)
                      .padding(vertical = 2.dp),
                    transformation = SurfaceTransformation(spec),
                    onClick = viewModel::skipRest,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                  ) {
                    Text(
                      text = "${if (lang == AppLanguage.RU) "Отдых" else "Rest"} ${formatWearClock(restRemaining.toLong())} (Skip)",
                      maxLines = 1,
                    )
                  }
                }
              }

              if (activeSet != null) {
                item {
                  val prev = prevSets.getOrNull(selectedSetIdx)
                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                    Text(
                      text = "Set ${selectedSetIdx + 1}/${wEx.sets.size}" +
                        if (prev != null) " • Prev ${prev.weightKg.toInt()}×${prev.reps}" else "",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      IconButton(onClick = { viewModel.adjustSetWeight(page, selectedSetIdx, -2.5) }) {
                        Text("−", fontWeight = FontWeight.Bold)
                      }
                      Text("${activeSet.weightKg} kg", style = MaterialTheme.typography.titleMedium)
                      IconButton(onClick = { viewModel.adjustSetWeight(page, selectedSetIdx, 2.5) }) {
                        Text("+", fontWeight = FontWeight.Bold)
                      }
                    }
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      IconButton(onClick = { viewModel.adjustSetReps(page, selectedSetIdx, -1) }) {
                        Text("−", fontWeight = FontWeight.Bold)
                      }
                      Text("${activeSet.reps} reps", style = MaterialTheme.typography.titleMedium)
                      IconButton(onClick = { viewModel.adjustSetReps(page, selectedSetIdx, 1) }) {
                        Text("+", fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }

                item {
                  Button(
                    modifier = Modifier
                      .fillMaxWidth()
                      .transformedHeight(this, spec)
                      .padding(vertical = 2.dp)
                      .testTag("wear_complete_set"),
                    transformation = SurfaceTransformation(spec),
                    onClick = { viewModel.toggleSetComplete(page, selectedSetIdx) },
                    icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                  ) {
                    Text(
                      text = if (activeSet.isCompleted) {
                        if (lang == AppLanguage.RU) "Выполнено" else "Done"
                      } else {
                        if (lang == AppLanguage.RU) "Засчитать сет" else "Complete Set"
                      },
                      maxLines = 1,
                    )
                  }
                }
              }

              item {
                OutlinedButton(
                  modifier = Modifier
                    .fillMaxWidth()
                    .transformedHeight(this, spec)
                    .padding(vertical = 2.dp),
                  transformation = SurfaceTransformation(spec),
                  onClick = { viewModel.addSet(page) },
                ) {
                  Text(if (lang == AppLanguage.RU) "+ Подход" else "+ Add Set", maxLines = 1)
                }
              }
            }
          }
        } else {
          ScreenScaffold {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Text(session.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Text(formatWearClock(elapsedSec), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = viewModel::togglePause) {
                  Icon(
                    imageVector = if (session.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    if (session.isPaused) (if (lang == AppLanguage.RU) "Продолжить" else "Resume")
                    else (if (lang == AppLanguage.RU) "Пауза" else "Pause")
                  )
                }
              }
              EdgeButton(
                onClick = viewModel::finishWorkout,
                modifier = Modifier.testTag("wear_finish_workout"),
              ) {
                Text(if (lang == AppLanguage.RU) "Завершить" else "Finish")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WearSummaryScreen(
  session: WorkoutSessionDto,
  lang: AppLanguage,
  onDone: () -> Unit,
) {
  val setsCount = session.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
  ScreenScaffold {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(if (lang == AppLanguage.RU) "Сохранено!" else "Saved!", style = MaterialTheme.typography.titleMedium)
        Text("$setsCount ${if (lang == AppLanguage.RU) "подх." else "sets"}", style = MaterialTheme.typography.bodySmall)
        if (session.avgHeartRate != null) {
          Text("♥ ${session.avgHeartRate} bpm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
      }
      EdgeButton(onClick = onDone) { Text("OK") }
    }
  }
}

private fun formatWearClock(totalSec: Long): String {
  val s = totalSec.coerceAtLeast(0L)
  val m = s / 60
  val sec = s % 60
  return "%02d:%02d".format(m, sec)
}
