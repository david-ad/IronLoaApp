package com.example.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.IronLogApplication
import com.example.data.preferences.UserPreferences
import com.example.ironlog.core.backup.BackupCodec
import com.example.ironlog.core.i18n.AppStrings
import com.example.ironlog.core.i18n.StringsProvider
import com.example.ironlog.core.math.PersonalRecord
import com.example.ironlog.core.math.StrengthCalculators
import com.example.ironlog.core.model.AppLanguage
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.MeasurementType
import com.example.ironlog.core.model.MuscleGroup
import com.example.ironlog.core.model.OneRmFormula
import com.example.ironlog.core.model.RoutineDto
import com.example.ironlog.core.model.RoutineFolderDto
import com.example.ironlog.core.model.SetType
import com.example.ironlog.core.model.ThemeMode
import com.example.ironlog.core.model.WeightUnit
import com.example.ironlog.core.model.WorkoutExerciseDto
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ironlog.core.model.WorkoutSetDto
import com.example.service.TimerServiceState
import com.example.service.WorkoutTimerService
import com.example.sync.WearSyncStatus
import com.example.telemetry.CrashReporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class FinishedWorkoutSummary(
  val session: WorkoutSessionDto,
  val durationSeconds: Long,
  val totalVolumeKg: Double,
  val completedSets: Int,
  val newPrExerciseIds: List<String>,
)

class IronLogViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as IronLogApplication
  private val repo = app.workoutRepository
  private val prefs = app.preferencesRepository
  private val wearSync = app.wearSyncManager

  val preferences: StateFlow<UserPreferences> = prefs.preferencesFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

  val strings: StateFlow<AppStrings> = preferences
    .map { StringsProvider.get(it.language) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StringsProvider.ru)

  val exercises: StateFlow<List<ExerciseDto>> = repo.exercisesFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val folders: StateFlow<List<RoutineFolderDto>> = repo.foldersFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val routines: StateFlow<List<RoutineDto>> = repo.routinesFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _startingSessionFallback = MutableStateFlow<WorkoutSessionDto?>(null)
  private val _isClosingActiveWorkout = MutableStateFlow(false)

  val activeWorkout: StateFlow<WorkoutSessionDto?> =
    combine(repo.activeWorkoutFlow, _startingSessionFallback, _isClosingActiveWorkout) { roomActive, fallback, closing ->
      if (closing) {
        if (roomActive == null) {
          _isClosingActiveWorkout.value = false
        }
        null
      } else {
        if (roomActive != null && fallback != null) {
          _startingSessionFallback.value = null
        }
        roomActive ?: fallback
      }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val completedWorkouts: StateFlow<List<WorkoutSessionDto>> = repo.completedWorkoutsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val timerState: StateFlow<TimerServiceState> = WorkoutTimerService.state
  val wearSyncStatus: StateFlow<WearSyncStatus> = wearSync.status

  val personalRecords: StateFlow<Map<String, PersonalRecord>> =
    combine(completedWorkouts, preferences) { sessions, pref ->
      StrengthCalculators.computePersonalRecords(sessions, pref.preferred1RmFormula)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  val muscleWorkingSets: StateFlow<Map<MuscleGroup, Double>> =
    combine(completedWorkouts, exercises) { sessions, exList ->
      val primary = exList.associate { it.id to it.primaryMuscle }
      val secondary = exList.associate { it.id to it.secondaryMuscles }
      StrengthCalculators.computeMuscleWorkingSets(sessions, primary, secondary)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  private val _workoutElapsedTickSec = MutableStateFlow(0L)
  val workoutElapsedTickSec: StateFlow<Long> = _workoutElapsedTickSec.asStateFlow()

  private val _finishedSummary = MutableStateFlow<FinishedWorkoutSummary?>(null)
  val finishedSummary: StateFlow<FinishedWorkoutSummary?> = _finishedSummary.asStateFlow()

  private val _statusBanner = MutableStateFlow<String?>(null)
  val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

  init {
    viewModelScope.launch {
      prefs.preferencesFlow.collect { pref ->
        CrashReporter.updatePreferences(pref)
      }
    }
    viewModelScope.launch {
      repo.ensureSeeded()
    }
    // Keep elapsed workout clock accurate locally even before/without FGS binding
    viewModelScope.launch {
      while (true) {
        val active = activeWorkout.value
        if (active != null) {
          val nowMs = System.currentTimeMillis()
          val extraPaused = if (active.isPaused && active.pauseStartedAtElapsedMs != null) {
            (SystemClock.elapsedRealtime() - active.pauseStartedAtElapsedMs!!).coerceAtLeast(0L)
          } else 0L
          val elapsed = ((nowMs - active.startedAtEpochMs - active.pausedDurationMs - extraPaused) / 1000L)
            .coerceAtLeast(0L)
          _workoutElapsedTickSec.value = elapsed
          // Ensure foreground service stays synced after app restart recovery
          if (!timerState.value.workoutRunning) {
            val pref = preferences.value
            WorkoutTimerService.startWorkoutService(
              context = app,
              title = active.name,
              elapsedSeconds = elapsed,
              isPaused = active.isPaused,
              soundEnabled = pref.timerSoundEnabled,
              vibrateEnabled = pref.timerVibrateEnabled,
            )
          }
        } else {
          _workoutElapsedTickSec.value = 0L
        }
        delay(1000L)
      }
    }
  }

  fun clearStatusBanner() {
    _statusBanner.value = null
  }

  fun dismissSummary() {
    _finishedSummary.value = null
  }

  // Preferences
  fun setWeightUnit(unit: WeightUnit) = viewModelScope.launch { prefs.setWeightUnit(unit) }
  fun setLanguage(language: AppLanguage) = viewModelScope.launch { prefs.setLanguage(language) }
  fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }
  fun setTimerSound(enabled: Boolean) = viewModelScope.launch { prefs.setTimerSound(enabled) }
  fun setTimerVibrate(enabled: Boolean) = viewModelScope.launch { prefs.setTimerVibrate(enabled) }
  fun setAutoRestTimer(enabled: Boolean) = viewModelScope.launch { prefs.setAutoRestTimer(enabled) }
  fun setOneRmFormula(formula: OneRmFormula) = viewModelScope.launch { prefs.setOneRmFormula(formula) }

  // Catalog
  fun saveExercise(exercise: ExerciseDto) = viewModelScope.launch { repo.saveExercise(exercise) }
  fun archiveExercise(exercise: ExerciseDto, archived: Boolean) = viewModelScope.launch {
    repo.archiveExercise(exercise, archived)
  }
  fun deleteCustomExercise(exerciseId: String) = viewModelScope.launch {
    repo.deleteCustomExercise(exerciseId)
  }

  // Folders & Routines
  fun saveFolder(folder: RoutineFolderDto) = viewModelScope.launch { repo.saveFolder(folder) }
  fun deleteFolder(folder: RoutineFolderDto) = viewModelScope.launch { repo.deleteFolder(folder) }
  fun saveRoutine(routine: RoutineDto) = viewModelScope.launch { repo.saveRoutine(routine) }
  fun duplicateRoutine(routine: RoutineDto) = viewModelScope.launch {
    val suffix = if (preferences.value.language == AppLanguage.RU) "копия" else "copy"
    repo.duplicateRoutine(routine, suffix)
  }
  fun deleteRoutine(routine: RoutineDto) = viewModelScope.launch { repo.deleteRoutine(routine) }

  // Workout Execution
  fun startWorkoutFromRoutine(routine: RoutineDto, onStarted: () -> Unit) = viewModelScope.launch {
    CrashReporter.log("workout_start:routine")
    _isClosingActiveWorkout.value = false
    val session = repo.startWorkoutFromRoutine(routine, completedWorkouts.value)
    _startingSessionFallback.value = session
    val pref = preferences.value
    WorkoutTimerService.startWorkoutService(
      context = app,
      title = session.name,
      elapsedSeconds = 0L,
      isPaused = false,
      soundEnabled = pref.timerSoundEnabled,
      vibrateEnabled = pref.timerVibrateEnabled,
    )
    onStarted()
  }

  fun startEmptyWorkout(onStarted: () -> Unit) = viewModelScope.launch {
    CrashReporter.log("workout_start:empty")
    _isClosingActiveWorkout.value = false
    val title = if (preferences.value.language == AppLanguage.RU) "Свободная тренировка" else "Freestyle Workout"
    val session = repo.startEmptyWorkout(title)
    _startingSessionFallback.value = session
    val pref = preferences.value
    WorkoutTimerService.startWorkoutService(
      context = app,
      title = session.name,
      elapsedSeconds = 0L,
      isPaused = false,
      soundEnabled = pref.timerSoundEnabled,
      vibrateEnabled = pref.timerVibrateEnabled,
    )
    onStarted()
  }

  fun togglePauseActiveWorkout() = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    val updated = repo.togglePauseWorkout(cur)
    val pref = preferences.value
    WorkoutTimerService.startWorkoutService(
      context = app,
      title = updated.name,
      elapsedSeconds = _workoutElapsedTickSec.value,
      isPaused = updated.isPaused,
      soundEnabled = pref.timerSoundEnabled,
      vibrateEnabled = pref.timerVibrateEnabled,
    )
  }

  fun updateWorkoutNotes(notes: String) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    repo.persistWorkoutSession(cur.copy(notes = notes), markSyncPending = false)
  }

  fun updateWorkoutName(name: String) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    repo.persistWorkoutSession(cur.copy(name = name), markSyncPending = false)
  }

  fun addExerciseToActiveWorkout(exercise: ExerciseDto) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    val prevSets = repo.findPreviousExerciseSets(exercise.id, completedWorkouts.value)
    val weId = UUID.randomUUID().toString()
    val initialSet = WorkoutSetDto(
      id = UUID.randomUUID().toString(),
      workoutExerciseId = weId,
      setIndex = 0,
      setType = SetType.NORMAL,
      weightKg = prevSets.firstOrNull()?.weightKg ?: 0.0,
      reps = prevSets.firstOrNull()?.reps ?: 8,
      durationSeconds = prevSets.firstOrNull()?.durationSeconds ?: if (exercise.measurementType == MeasurementType.DURATION) 60 else 0,
      distanceMeters = prevSets.firstOrNull()?.distanceMeters ?: 0.0,
      isCompleted = false,
    )
    val newEx = WorkoutExerciseDto(
      id = weId,
      sessionId = cur.id,
      exerciseId = exercise.id,
      sortOrder = cur.exercises.size,
      restSecondsOverride = 90,
      sets = listOf(initialSet),
    )
    repo.persistWorkoutSession(cur.copy(exercises = cur.exercises + newEx), markSyncPending = false)
  }

  fun replaceExerciseInActiveWorkout(workoutExerciseId: String, replacement: ExerciseDto) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    val updated = cur.exercises.map { ex ->
      if (ex.id == workoutExerciseId) ex.copy(exerciseId = replacement.id) else ex
    }
    repo.persistWorkoutSession(cur.copy(exercises = updated), markSyncPending = false)
  }

  fun removeExerciseFromActiveWorkout(workoutExerciseId: String) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    repo.persistWorkoutSession(
      cur.copy(exercises = cur.exercises.filterNot { it.id == workoutExerciseId }),
      markSyncPending = false,
    )
  }

  fun moveExerciseInActiveWorkout(index: Int, direction: Int) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    val target = index + direction
    if (index !in cur.exercises.indices || target !in cur.exercises.indices) return@launch
    val mutable = cur.exercises.toMutableList()
    val item = mutable.removeAt(index)
    mutable.add(target, item)
    repo.persistWorkoutSession(cur.copy(exercises = mutable), markSyncPending = false)
  }

  fun toggleSupersetWithNext(workoutExerciseId: String) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    val idx = cur.exercises.indexOfFirst { it.id == workoutExerciseId }
    if (idx < 0 || idx + 1 >= cur.exercises.size) return@launch
    val first = cur.exercises[idx]
    val second = cur.exercises[idx + 1]
    val newGroup = if (first.supersetGroupId != null && first.supersetGroupId == second.supersetGroupId) {
      null
    } else {
      first.supersetGroupId ?: ((cur.exercises.mapNotNull { it.supersetGroupId }.maxOrNull() ?: 0) + 1)
    }
    val updated = cur.exercises.mapIndexed { i, ex ->
      when (i) {
        idx, idx + 1 -> ex.copy(supersetGroupId = newGroup)
        else -> ex
      }
    }
    repo.persistWorkoutSession(cur.copy(exercises = updated), markSyncPending = false)
  }

  fun updateExerciseNotesAndRest(workoutExerciseId: String, notes: String, restSec: Int?) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    val updated = cur.exercises.map { ex ->
      if (ex.id == workoutExerciseId) ex.copy(notes = notes, restSecondsOverride = restSec) else ex
    }
    repo.persistWorkoutSession(cur.copy(exercises = updated), markSyncPending = false)
  }

  fun addSetToActiveExercise(workoutExerciseId: String) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    val updated = cur.exercises.map { ex ->
      if (ex.id != workoutExerciseId) ex else {
        val last = ex.sets.lastOrNull()
        val newSet = WorkoutSetDto(
          id = UUID.randomUUID().toString(),
          workoutExerciseId = ex.id,
          setIndex = ex.sets.size,
          setType = SetType.NORMAL,
          weightKg = last?.weightKg ?: 0.0,
          reps = last?.reps ?: 8,
          durationSeconds = last?.durationSeconds ?: 0,
          distanceMeters = last?.distanceMeters ?: 0.0,
          isCompleted = false,
        )
        ex.copy(sets = ex.sets + newSet)
      }
    }
    repo.persistWorkoutSession(cur.copy(exercises = updated), markSyncPending = false)
  }

  fun removeSetFromActiveExercise(workoutExerciseId: String, setId: String) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    val updated = cur.exercises.map { ex ->
      if (ex.id != workoutExerciseId) ex else ex.copy(sets = ex.sets.filterNot { it.id == setId })
    }
    repo.persistWorkoutSession(cur.copy(exercises = updated), markSyncPending = false)
  }

  fun updateActiveSet(
    workoutExerciseId: String,
    updatedSet: WorkoutSetDto,
    triggerRestIfJustCompleted: Boolean = false,
    exerciseTitleForTimer: String = "",
  ) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    var restDuration = 90
    val updatedExercises = cur.exercises.map { ex ->
      if (ex.id != workoutExerciseId) ex else {
        restDuration = ex.restSecondsOverride ?: 90
        ex.copy(
          sets = ex.sets.map { s ->
            if (s.id == updatedSet.id) updatedSet else s
          }
        )
      }
    }
    repo.persistWorkoutSession(cur.copy(exercises = updatedExercises), markSyncPending = false)

    if (triggerRestIfJustCompleted && updatedSet.isCompleted && preferences.value.autoStartRestTimer) {
      startRestTimer(restDuration, exerciseTitleForTimer)
    }
  }

  fun copyPreviousSetValues(workoutExerciseId: String, setIndex: Int, exerciseId: String) = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    val prevSets = repo.findPreviousExerciseSets(exerciseId, completedWorkouts.value)
    val prev = prevSets.getOrNull(setIndex) ?: prevSets.lastOrNull() ?: return@launch
    val updated = cur.exercises.map { ex ->
      if (ex.id != workoutExerciseId) ex else {
        ex.copy(
          sets = ex.sets.mapIndexed { idx, s ->
            if (idx == setIndex) {
              s.copy(
                weightKg = prev.weightKg,
                reps = prev.reps,
                durationSeconds = prev.durationSeconds,
                distanceMeters = prev.distanceMeters,
              )
            } else s
          }
        )
      }
    }
    repo.persistWorkoutSession(cur.copy(exercises = updated), markSyncPending = false)
  }

  fun startRestTimer(seconds: Int, exerciseTitle: String = "") {
    val pref = preferences.value
    WorkoutTimerService.startRestTimer(
      context = app,
      seconds = seconds,
      exerciseTitle = exerciseTitle,
      soundEnabled = pref.timerSoundEnabled,
      vibrateEnabled = pref.timerVibrateEnabled,
    )
  }

  fun adjustRestTimer(deltaSeconds: Int) {
    WorkoutTimerService.adjustRestTimer(app, deltaSeconds)
  }

  fun skipRestTimer() {
    WorkoutTimerService.stopRestTimer(app)
  }

  fun finishActiveWorkout() = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    CrashReporter.log("workout_finish")
    _isClosingActiveWorkout.value = true
    _startingSessionFallback.value = null
    val oldPrs = personalRecords.value
    val finished = repo.finishWorkout(cur)
    WorkoutTimerService.stopWorkoutService(app)

    val measurementMap = exercises.value.associate { it.id to it.measurementType }
    val volume = StrengthCalculators.sessionVolumeKg(finished, measurementMap)
    val completedSets = finished.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    val newPrs = mutableListOf<String>()
    val formula = preferences.value.preferred1RmFormula
    for (ex in finished.exercises) {
      val prevBest = oldPrs[ex.exerciseId]
      val sets = ex.sets.filter { it.isCompleted && it.setType != SetType.WARMUP }
      val beat = sets.any { s ->
        val est1Rm = StrengthCalculators.estimate1Rm(s.weightKg, s.reps, formula)
        prevBest == null || s.weightKg > prevBest.maxWeightKg || est1Rm > prevBest.estimated1RmKg
      }
      if (beat && sets.isNotEmpty()) newPrs.add(ex.exerciseId)
    }

    _finishedSummary.value = FinishedWorkoutSummary(
      session = finished,
      durationSeconds = _workoutElapsedTickSec.value,
      totalVolumeKg = volume,
      completedSets = completedSets,
      newPrExerciseIds = newPrs,
    )
  }

  fun discardActiveWorkout() = viewModelScope.launch {
    val cur = activeWorkout.value ?: return@launch
    CrashReporter.log("workout_discard")
    _isClosingActiveWorkout.value = true
    _startingSessionFallback.value = null
    repo.discardActiveWorkout(cur)
    WorkoutTimerService.stopWorkoutService(app)
  }

  // History Editing & Deletion
  fun updateCompletedWorkout(session: WorkoutSessionDto) = viewModelScope.launch {
    repo.persistWorkoutSession(session.copy(isActive = false), markSyncPending = true)
  }

  fun deleteCompletedWorkout(session: WorkoutSessionDto) = viewModelScope.launch {
    repo.deleteCompletedWorkout(session)
  }

  fun findPreviousSets(exerciseId: String): List<WorkoutSetDto> =
    repo.findPreviousExerciseSets(exerciseId, completedWorkouts.value)

  // Wear OS Local Sync
  fun refreshWearNodes() = viewModelScope.launch {
    wearSync.refreshConnectedNodes()
  }

  fun syncWithWatch() = viewModelScope.launch {
    val res = wearSync.syncWithWatch()
    val lang = preferences.value.language
    _statusBanner.value = res.fold(
      onSuccess = { (rCount, sCount) ->
        if (lang == AppLanguage.RU) "Синхронизация отправлена: $rCount программ, $sCount тренировок"
        else "Local sync sent: $rCount routines, $sCount workouts"
      },
      onFailure = { err ->
        if (lang == AppLanguage.RU) "Нет связи с часами: ${err.message}"
        else "Watch unavailable: ${err.message}"
      },
    )
  }

  // Backup Export & Import (P2)
  suspend fun buildJsonBackup(): String = repo.exportJsonBackup()
  suspend fun buildCsvHistory(): String = repo.exportCsvHistory(preferences.value.language)

  fun importJsonBackup(rawJson: String) = viewModelScope.launch {
    val lang = preferences.value.language
    val decoded = BackupCodec.decodeAndVerifyBackup(rawJson)
    if (decoded.isFailure) {
      decoded.exceptionOrNull()?.let { err ->
        CrashReporter.recordNonFatal(err, area = "backup_import")
      }
      _statusBanner.value = if (lang == AppLanguage.RU) {
        "Ошибка проверки файла: ${decoded.exceptionOrNull()?.message}"
      } else {
        "Invalid backup file: ${decoded.exceptionOrNull()?.message}"
      }
      return@launch
    }
    CrashReporter.log("backup_import_success")
    val (rCount, sCount) = repo.mergeIncomingEnvelope(decoded.getOrThrow())
    _statusBanner.value = if (lang == AppLanguage.RU) {
      "Восстановлено: $rCount программ, $sCount тренировок"
    } else {
      "Restored: $rCount routines, $sCount workouts"
    }
  }

  fun wipeAllData() = viewModelScope.launch {
    CrashReporter.log("wipe_all_data")
    WorkoutTimerService.stopWorkoutService(app)
    repo.wipeAllData()
    val lang = preferences.value.language
    _statusBanner.value = if (lang == AppLanguage.RU) "Все локальные данные удалены" else "All local data wiped"
  }
}
