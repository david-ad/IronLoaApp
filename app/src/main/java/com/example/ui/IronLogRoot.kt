package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ActiveWorkoutScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalculatorsScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeAndRoutinesScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.WorkoutSummaryDialog
import com.example.ui.theme.IronLogTheme
import com.example.ui.viewmodel.IronLogViewModel
import kotlinx.serialization.Serializable

@Serializable object RouteTrain
@Serializable object RouteCatalog
@Serializable object RouteHistory
@Serializable object RouteAnalytics
@Serializable object RouteCalculators
@Serializable object RouteActiveWorkout

private data class TopNavItem(
  val route: Any,
  val label: String,
  val icon: ImageVector,
  val tag: String,
)

@Composable
fun IronLogRoot(viewModel: IronLogViewModel) {
  val prefs by viewModel.preferences.collectAsStateWithLifecycle()
  val strings by viewModel.strings.collectAsStateWithLifecycle()
  val exercises by viewModel.exercises.collectAsStateWithLifecycle()
  val folders by viewModel.folders.collectAsStateWithLifecycle()
  val routines by viewModel.routines.collectAsStateWithLifecycle()
  val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
  val completedWorkouts by viewModel.completedWorkouts.collectAsStateWithLifecycle()
  val personalRecords by viewModel.personalRecords.collectAsStateWithLifecycle()
  val muscleWorkingSets by viewModel.muscleWorkingSets.collectAsStateWithLifecycle()
  val timerState by viewModel.timerState.collectAsStateWithLifecycle()
  val elapsedSec by viewModel.workoutElapsedTickSec.collectAsStateWithLifecycle()
  val wearSyncStatus by viewModel.wearSyncStatus.collectAsStateWithLifecycle()
  val finishedSummary by viewModel.finishedSummary.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val notificationPermLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { /* Timer works with or without notification permission */ }

  fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  IronLogTheme(themeMode = prefs.themeMode) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(statusBanner) {
      statusBanner?.let { msg ->
        snackbarHostState.showSnackbar(msg)
        viewModel.clearStatusBanner()
      }
    }

    val isInActiveWorkoutScreen = backStackEntry?.destination?.hasRoute<RouteActiveWorkout>() == true

    val navItems = listOf(
      TopNavItem(RouteTrain, strings.navWorkout, Icons.Filled.FitnessCenter, "nav_train"),
      TopNavItem(RouteCatalog, strings.navCatalog, Icons.AutoMirrored.Filled.MenuBook, "nav_catalog"),
      TopNavItem(RouteHistory, strings.navHistory, Icons.Filled.History, "nav_history"),
      TopNavItem(RouteAnalytics, strings.navAnalytics, Icons.Filled.Analytics, "nav_analytics"),
      TopNavItem(RouteCalculators, strings.navTools, Icons.Filled.Calculate, "nav_calc"),
    )

    fun navigateToTopTab(route: Any) {
      if (route is RouteTrain) {
        val poppedToTrain = navController.popBackStack<RouteTrain>(inclusive = false)
        if (!poppedToTrain) {
          navController.navigate(RouteTrain) {
            popUpTo(navController.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
          }
        }
      } else {
        navController.navigate(route) {
          popUpTo<RouteTrain> { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
      }
    }

    fun openActiveWorkout() {
      navController.navigate(RouteActiveWorkout) {
        launchSingleTop = true
      }
    }

    fun exitActiveWorkoutToTrain() {
      val popped = navController.popBackStack<RouteActiveWorkout>(inclusive = true)
      if (!popped && navController.currentBackStackEntry == null) {
        navController.navigate(RouteTrain) {
          launchSingleTop = true
        }
      }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
          if (!useNavRail && !isInActiveWorkoutScreen) {
            NavigationBar {
              navItems.forEach { item ->
                val selected = when (item.route) {
                  is RouteTrain -> backStackEntry?.destination?.hasRoute<RouteTrain>() == true
                  is RouteCatalog -> backStackEntry?.destination?.hasRoute<RouteCatalog>() == true
                  is RouteHistory -> backStackEntry?.destination?.hasRoute<RouteHistory>() == true
                  is RouteAnalytics -> backStackEntry?.destination?.hasRoute<RouteAnalytics>() == true
                  is RouteCalculators -> backStackEntry?.destination?.hasRoute<RouteCalculators>() == true
                  else -> false
                }
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTopTab(item.route) },
                  icon = { Icon(item.icon, contentDescription = item.label) },
                  label = { Text(item.label, maxLines = 1) },
                  modifier = Modifier.testTag(item.tag),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        ) {
          if (useNavRail && !isInActiveWorkoutScreen) {
            NavigationRail {
              navItems.forEach { item ->
                val selected = when (item.route) {
                  is RouteTrain -> backStackEntry?.destination?.hasRoute<RouteTrain>() == true
                  is RouteCatalog -> backStackEntry?.destination?.hasRoute<RouteCatalog>() == true
                  is RouteHistory -> backStackEntry?.destination?.hasRoute<RouteHistory>() == true
                  is RouteAnalytics -> backStackEntry?.destination?.hasRoute<RouteAnalytics>() == true
                  is RouteCalculators -> backStackEntry?.destination?.hasRoute<RouteCalculators>() == true
                  else -> false
                }
                NavigationRailItem(
                  selected = selected,
                  onClick = { navigateToTopTab(item.route) },
                  icon = { Icon(item.icon, contentDescription = item.label) },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag(item.tag),
                )
              }
            }
          }

          NavHost(
            navController = navController,
            startDestination = RouteTrain,
            modifier = Modifier.weight(1f),
          ) {
            composable<RouteTrain> {
              HomeAndRoutinesScreen(
                strings = strings,
                prefs = prefs,
                exercises = exercises,
                folders = folders,
                routines = routines,
                activeWorkout = activeWorkout,
                activeElapsedSec = elapsedSec,
                completedWorkouts = completedWorkouts,
                onStartEmptyWorkout = {
                  requestNotificationPermissionIfNeeded()
                  viewModel.startEmptyWorkout {
                    openActiveWorkout()
                  }
                },
                onResumeActiveWorkout = {
                  requestNotificationPermissionIfNeeded()
                  openActiveWorkout()
                },
                onStartRoutine = { routine ->
                  requestNotificationPermissionIfNeeded()
                  viewModel.startWorkoutFromRoutine(routine) {
                    openActiveWorkout()
                  }
                },
                onSaveRoutine = viewModel::saveRoutine,
                onDuplicateRoutine = viewModel::duplicateRoutine,
                onDeleteRoutine = viewModel::deleteRoutine,
                onSaveFolder = viewModel::saveFolder,
                onOpenSettings = { showSettings = true },
              )
            }

            composable<RouteActiveWorkout> {
              var lastKnownSession by remember { mutableStateOf(activeWorkout) }
              if (activeWorkout != null && activeWorkout != lastKnownSession) {
                lastKnownSession = activeWorkout
              }
              val sessionToRender = activeWorkout ?: lastKnownSession
              if (sessionToRender == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Button(onClick = { exitActiveWorkoutToTrain() }) {
                    Text(strings.navWorkout)
                  }
                }
              } else {
                ActiveWorkoutScreen(
                  session = sessionToRender,
                  strings = strings,
                  prefs = prefs,
                  catalogExercises = exercises,
                  elapsedSec = elapsedSec,
                  timerState = timerState,
                  findPreviousSets = viewModel::findPreviousSets,
                  onTogglePause = viewModel::togglePauseActiveWorkout,
                  onUpdateWorkoutNotes = viewModel::updateWorkoutNotes,
                  onAddExercise = viewModel::addExerciseToActiveWorkout,
                  onReplaceExercise = viewModel::replaceExerciseInActiveWorkout,
                  onRemoveExercise = viewModel::removeExerciseFromActiveWorkout,
                  onMoveExercise = viewModel::moveExerciseInActiveWorkout,
                  onToggleSuperset = viewModel::toggleSupersetWithNext,
                  onUpdateExerciseNotesAndRest = viewModel::updateExerciseNotesAndRest,
                  onAddSet = viewModel::addSetToActiveExercise,
                  onRemoveSet = viewModel::removeSetFromActiveExercise,
                  onUpdateSet = viewModel::updateActiveSet,
                  onCopyPreviousSet = viewModel::copyPreviousSetValues,
                  onStartRestTimer = viewModel::startRestTimer,
                  onAdjustRestTimer = viewModel::adjustRestTimer,
                  onSkipRestTimer = viewModel::skipRestTimer,
                  onFinishWorkout = {
                    viewModel.finishActiveWorkout()
                    exitActiveWorkoutToTrain()
                  },
                  onDiscardWorkout = {
                    viewModel.discardActiveWorkout()
                    exitActiveWorkoutToTrain()
                  },
                )
              }
            }

            composable<RouteCatalog> {
              CatalogScreen(
                strings = strings,
                prefs = prefs,
                exercises = exercises,
                personalRecords = personalRecords,
                onSaveExercise = viewModel::saveExercise,
                onArchiveExercise = viewModel::archiveExercise,
                onDeleteCustomExercise = viewModel::deleteCustomExercise,
              )
            }

            composable<RouteHistory> {
              HistoryScreen(
                strings = strings,
                prefs = prefs,
                exercises = exercises,
                completedWorkouts = completedWorkouts,
                onUpdateWorkout = viewModel::updateCompletedWorkout,
                onDeleteWorkout = viewModel::deleteCompletedWorkout,
              )
            }

            composable<RouteAnalytics> {
              AnalyticsScreen(
                strings = strings,
                prefs = prefs,
                exercises = exercises,
                routines = routines,
                completedWorkouts = completedWorkouts,
                personalRecords = personalRecords,
                muscleWorkingSets = muscleWorkingSets,
              )
            }

            composable<RouteCalculators> {
              CalculatorsScreen(
                strings = strings,
                prefs = prefs,
                exercises = exercises,
                completedWorkouts = completedWorkouts,
                personalRecords = personalRecords,
                muscleWorkingSets = muscleWorkingSets,
                onSetFormula = viewModel::setOneRmFormula,
              )
            }
          }
        }
      }

      if (showSettings) {
        SettingsDialog(
          strings = strings,
          prefs = prefs,
          wearSyncStatus = wearSyncStatus,
          onSetUnit = viewModel::setWeightUnit,
          onSetLanguage = viewModel::setLanguage,
          onSetTheme = viewModel::setThemeMode,
          onSetTimerSound = viewModel::setTimerSound,
          onSetTimerVibrate = viewModel::setTimerVibrate,
          onSetAutoRest = viewModel::setAutoRestTimer,
          onRefreshWearNodes = viewModel::refreshWearNodes,
          onSyncWithWatch = viewModel::syncWithWatch,
          onBuildJsonBackup = viewModel::buildJsonBackup,
          onBuildCsvHistory = viewModel::buildCsvHistory,
          onImportJsonBackup = viewModel::importJsonBackup,
          onWipeAllData = viewModel::wipeAllData,
          onDismiss = { showSettings = false },
        )
      }

      finishedSummary?.let { summary ->
        WorkoutSummaryDialog(
          summary = summary,
          strings = strings,
          prefs = prefs,
          exercisesById = exercises.associateBy { it.id },
          onDismiss = viewModel::dismissSummary,
        )
      }
    }
  }
}
