package com.example.ironlog.wear.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.HeartRateSampleDto
import com.example.ironlog.core.model.RoutineDto
import com.example.ironlog.core.model.SetType
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ironlog.core.model.WorkoutSetDto
import com.example.ironlog.wear.WearApplication
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class WearViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as WearApplication
  private val repo = app.wearRepository
  private val hrMonitor = app.heartRateMonitor

  val exercises: StateFlow<List<ExerciseDto>> = repo.exercisesFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val routines: StateFlow<List<RoutineDto>> = repo.routinesFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val activeSession: StateFlow<WorkoutSessionDto?> = repo.activeSessionFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val completedSessions: StateFlow<List<WorkoutSessionDto>> = repo.completedSessionsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _elapsedSec = MutableStateFlow(0L)
  val elapsedSec: StateFlow<Long> = _elapsedSec.asStateFlow()

  private val _restRemainingSec = MutableStateFlow(0)
  val restRemainingSec: StateFlow<Int> = _restRemainingSec.asStateFlow()

  private val _currentHeartRate = MutableStateFlow<Int?>(null)
  val currentHeartRate: StateFlow<Int?> = _currentHeartRate.asStateFlow()

  private val _lastSummary = MutableStateFlow<WorkoutSessionDto?>(null)
  val lastSummary: StateFlow<WorkoutSessionDto?> = _lastSummary.asStateFlow()

  private val recordedHrSamples = mutableListOf<HeartRateSampleDto>()
  private var restEndElapsedMs: Long = 0L
  private var restAlerted: Boolean = true
  private var hrJob: Job? = null

  init {
    viewModelScope.launch { repo.ensureSeeded() }
    viewModelScope.launch {
      while (true) {
        val active = activeSession.value
        if (active != null) {
          val now = System.currentTimeMillis()
          val extraPause = if (active.isPaused && active.pauseStartedAtElapsedMs != null) {
            (SystemClock.elapsedRealtime() - active.pauseStartedAtElapsedMs!!).coerceAtLeast(0L)
          } else 0L
          _elapsedSec.value = ((now - active.startedAtEpochMs - active.pausedDurationMs - extraPause) / 1000L)
            .coerceAtLeast(0L)
        } else {
          _elapsedSec.value = 0L
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        if (restEndElapsedMs > nowElapsed) {
          _restRemainingSec.value = (((restEndElapsedMs - nowElapsed) + 999L) / 1000L).toInt()
        } else {
          _restRemainingSec.value = 0
          if (!restAlerted && restEndElapsedMs > 0L) {
            restAlerted = true
            vibrateWatch()
          }
        }
        delay(1000L)
      }
    }
  }

  fun startHeartRateCollection() {
    if (!hrMonitor.isAvailable || hrJob?.isActive == true) return
    hrJob = viewModelScope.launch {
      hrMonitor.heartRateFlow().collect { bpm ->
        _currentHeartRate.value = bpm
        val active = activeSession.value
        if (active != null && !active.isPaused) {
          recordedHrSamples.add(
            HeartRateSampleDto(
              id = UUID.randomUUID().toString(),
              sessionId = active.id,
              timestampEpochMs = System.currentTimeMillis(),
              bpm = bpm,
            )
          )
        }
      }
    }
  }

  fun startRoutine(routine: RoutineDto) = viewModelScope.launch {
    recordedHrSamples.clear()
    repo.startRoutineWorkout(routine, completedSessions.value)
  }

  fun startEmpty() = viewModelScope.launch {
    recordedHrSamples.clear()
    val firstExId = exercises.value.firstOrNull()?.id ?: "ex_bench_press_barbell"
    repo.startEmptyWorkout(firstExId)
  }

  fun adjustSetWeight(exerciseIndex: Int, setIndex: Int, deltaKg: Double) = viewModelScope.launch {
    val cur = activeSession.value ?: return@launch
    val updated = cur.exercises.mapIndexed { ei, ex ->
      if (ei != exerciseIndex) ex else ex.copy(
        sets = ex.sets.mapIndexed { si, s ->
          if (si != setIndex) s else s.copy(weightKg = (s.weightKg + deltaKg).coerceAtLeast(0.0))
        }
      )
    }
    repo.saveSession(cur.copy(exercises = updated), pendingSync = false)
  }

  fun adjustSetReps(exerciseIndex: Int, setIndex: Int, deltaReps: Int) = viewModelScope.launch {
    val cur = activeSession.value ?: return@launch
    val updated = cur.exercises.mapIndexed { ei, ex ->
      if (ei != exerciseIndex) ex else ex.copy(
        sets = ex.sets.mapIndexed { si, s ->
          if (si != setIndex) s else s.copy(reps = (s.reps + deltaReps).coerceAtLeast(0))
        }
      )
    }
    repo.saveSession(cur.copy(exercises = updated), pendingSync = false)
  }

  fun toggleSetComplete(exerciseIndex: Int, setIndex: Int) = viewModelScope.launch {
    val cur = activeSession.value ?: return@launch
    var completedNow = false
    var restSec = 90
    val updated = cur.exercises.mapIndexed { ei, ex ->
      if (ei != exerciseIndex) ex else {
        restSec = ex.restSecondsOverride ?: 90
        ex.copy(
          sets = ex.sets.mapIndexed { si, s ->
            if (si != setIndex) s else {
              completedNow = !s.isCompleted
              s.copy(
                isCompleted = completedNow,
                completedAtEpochMs = if (completedNow) System.currentTimeMillis() else null,
              )
            }
          }
        )
      }
    }
    repo.saveSession(cur.copy(exercises = updated), pendingSync = false)
    if (completedNow) {
      restEndElapsedMs = SystemClock.elapsedRealtime() + restSec * 1000L
      restAlerted = false
    }
  }

  fun addSet(exerciseIndex: Int) = viewModelScope.launch {
    val cur = activeSession.value ?: return@launch
    val updated = cur.exercises.mapIndexed { ei, ex ->
      if (ei != exerciseIndex) ex else {
        val last = ex.sets.lastOrNull()
        val newSet = WorkoutSetDto(
          id = UUID.randomUUID().toString(),
          workoutExerciseId = ex.id,
          setIndex = ex.sets.size,
          setType = SetType.NORMAL,
          weightKg = last?.weightKg ?: 20.0,
          reps = last?.reps ?: 8,
        )
        ex.copy(sets = ex.sets + newSet)
      }
    }
    repo.saveSession(cur.copy(exercises = updated), pendingSync = false)
  }

  fun togglePause() = viewModelScope.launch {
    val cur = activeSession.value ?: return@launch
    val nowElapsed = SystemClock.elapsedRealtime()
    val next = if (!cur.isPaused) {
      cur.copy(isPaused = true, pauseStartedAtElapsedMs = nowElapsed)
    } else {
      val delta = cur.pauseStartedAtElapsedMs?.let { (nowElapsed - it).coerceAtLeast(0L) } ?: 0L
      cur.copy(isPaused = false, pausedDurationMs = cur.pausedDurationMs + delta, pauseStartedAtElapsedMs = null)
    }
    repo.saveSession(next, pendingSync = false)
  }

  fun skipRest() {
    restEndElapsedMs = 0L
    restAlerted = true
    _restRemainingSec.value = 0
  }

  fun finishWorkout() = viewModelScope.launch {
    val cur = activeSession.value ?: return@launch
    skipRest()
    val finished = repo.finishSession(cur, recordedHrSamples.toList())
    _lastSummary.value = finished
  }

  fun dismissSummary() {
    _lastSummary.value = null
  }

  fun syncNow() = viewModelScope.launch {
    repo.pushPendingToPhone()
  }

  fun previousSetsFor(exerciseId: String): List<WorkoutSetDto> =
    repo.findPreviousSets(exerciseId, completedSessions.value)

  private fun vibrateWatch() {
    runCatching {
      val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
      vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 250), -1))
    }
  }
}
