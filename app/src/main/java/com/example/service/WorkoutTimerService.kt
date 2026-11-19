package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimerServiceState(
  val workoutRunning: Boolean = false,
  val workoutPaused: Boolean = false,
  val workoutTitle: String = "",
  val workoutElapsedSeconds: Long = 0L,
  val restRemainingSeconds: Int = 0,
  val restTotalSeconds: Int = 0,
  val restRunning: Boolean = false,
  val restExerciseTitle: String = "",
)

class WorkoutTimerService : Service() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var tickerJob: Job? = null

  private var workoutStartElapsedMs: Long = 0L
  private var accumulatedPausedMs: Long = 0L
  private var isPaused: Boolean = false
  private var pauseStartElapsedMs: Long = 0L
  private var workoutTitle: String = "IronLog"

  private var restEndElapsedMs: Long = 0L
  private var restTotalSeconds: Int = 0
  private var restExerciseTitle: String = ""
  private var restAlertFired: Boolean = true
  private var soundEnabled: Boolean = true
  private var vibrateEnabled: Boolean = true

  override fun onCreate() {
    super.onCreate()
    createNotificationChannels()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START_OR_UPDATE_WORKOUT -> {
        workoutTitle = intent.getStringExtra(EXTRA_WORKOUT_TITLE) ?: "IronLog"
        val elapsedSeconds = intent.getLongExtra(EXTRA_WORKOUT_ELAPSED_SEC, 0L)
        isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)
        soundEnabled = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        vibrateEnabled = intent.getBooleanExtra(EXTRA_VIBRATE_ENABLED, true)
        val nowElapsed = SystemClock.elapsedRealtime()
        workoutStartElapsedMs = nowElapsed - (elapsedSeconds * 1000L)
        accumulatedPausedMs = 0L
        pauseStartElapsedMs = if (isPaused) nowElapsed else 0L
        startForegroundSafe()
        ensureTickerRunning()
      }
      ACTION_START_REST -> {
        val seconds = intent.getIntExtra(EXTRA_REST_SECONDS, 90).coerceAtLeast(1)
        restExerciseTitle = intent.getStringExtra(EXTRA_REST_EXERCISE) ?: ""
        soundEnabled = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        vibrateEnabled = intent.getBooleanExtra(EXTRA_VIBRATE_ENABLED, true)
        restTotalSeconds = seconds
        restEndElapsedMs = SystemClock.elapsedRealtime() + seconds * 1000L
        restAlertFired = false
        startForegroundSafe()
        ensureTickerRunning()
      }
      ACTION_ADJUST_REST -> {
        val deltaSec = intent.getIntExtra(EXTRA_REST_DELTA_SEC, 0)
        if (!restAlertFired && restEndElapsedMs > 0L) {
          val now = SystemClock.elapsedRealtime()
          val newEnd = (restEndElapsedMs + deltaSec * 1000L).coerceAtLeast(now + 1000L)
          restEndElapsedMs = newEnd
          restTotalSeconds = maxOf(restTotalSeconds, ((newEnd - now) / 1000L).toInt())
          emitSnapshot()
        }
      }
      ACTION_STOP_REST -> {
        restEndElapsedMs = 0L
        restTotalSeconds = 0
        restAlertFired = true
        emitSnapshot()
      }
      ACTION_STOP_WORKOUT -> {
        tickerJob?.cancel()
        restEndElapsedMs = 0L
        restAlertFired = true
        _state.value = TimerServiceState()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
    }
    return START_STICKY
  }

  private fun startForegroundSafe() {
    val notification = buildOngoingNotification(emitSnapshot())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ServiceCompat.startForeground(
        this,
        NOTIFICATION_ID_ONGOING,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(NOTIFICATION_ID_ONGOING, notification)
    }
  }

  private fun ensureTickerRunning() {
    if (tickerJob?.isActive == true) return
    tickerJob = scope.launch {
      while (true) {
        val snap = emitSnapshot()
        if (!snap.restRunning && !restAlertFired && restEndElapsedMs > 0L) {
          restAlertFired = true
          triggerRestFinishedAlert()
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_ONGOING, buildOngoingNotification(snap))
        delay(1000L)
      }
    }
  }

  private fun emitSnapshot(): TimerServiceState {
    val now = SystemClock.elapsedRealtime()
    val workoutSec = if (workoutStartElapsedMs == 0L) 0L else {
      val effectiveNow = if (isPaused && pauseStartElapsedMs > 0L) pauseStartElapsedMs else now
      ((effectiveNow - workoutStartElapsedMs - accumulatedPausedMs) / 1000L).coerceAtLeast(0L)
    }
    val restRem = if (restEndElapsedMs > now) {
      (((restEndElapsedMs - now) + 999L) / 1000L).toInt()
    } else {
      0
    }
    val snap = TimerServiceState(
      workoutRunning = workoutStartElapsedMs > 0L,
      workoutPaused = isPaused,
      workoutTitle = workoutTitle,
      workoutElapsedSeconds = workoutSec,
      restRemainingSeconds = restRem,
      restTotalSeconds = restTotalSeconds,
      restRunning = restRem > 0,
      restExerciseTitle = restExerciseTitle,
    )
    _state.value = snap
    return snap
  }

  private fun triggerRestFinishedAlert() {
    if (soundEnabled) {
      runCatching {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
        scope.launch {
          delay(600)
          tg.release()
        }
      }
    }
    if (vibrateEnabled) {
      runCatching {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 220, 120, 280), -1)
          )
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(longArrayOf(0, 220, 120, 280), -1)
        }
      }
    }
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pi = PendingIntent.getActivity(
      this,
      2,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val alert = NotificationCompat.Builder(this, CHANNEL_REST_ALERT)
      .setSmallIcon(R.drawable.ic_stat_ironlog)
      .setContentTitle("IronLog • ${restExerciseTitle.ifBlank { workoutTitle }}")
      .setContentText("Rest finished — ready for next set!")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pi)
      .build()
    nm.notify(NOTIFICATION_ID_REST_ALERT, alert)
  }

  private fun buildOngoingNotification(snap: TimerServiceState): Notification {
    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pi = PendingIntent.getActivity(
      this,
      1,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val workoutClock = formatClock(snap.workoutElapsedSeconds)
    val contentText = if (snap.restRunning) {
      "⏱ $workoutClock  •  Rest: ${formatClock(snap.restRemainingSeconds.toLong())}"
    } else if (snap.workoutPaused) {
      "⏸ $workoutClock (Paused)"
    } else {
      "⏱ $workoutClock"
    }
    return NotificationCompat.Builder(this, CHANNEL_WORKOUT_ONGOING)
      .setSmallIcon(R.drawable.ic_stat_ironlog)
      .setContentTitle(snap.workoutTitle.ifBlank { "IronLog" })
      .setContentText(contentText)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(pi)
      .build()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      nm.createNotificationChannel(
        NotificationChannel(
          CHANNEL_WORKOUT_ONGOING,
          "Active Workout Timer",
          NotificationManager.IMPORTANCE_LOW,
        )
      )
      nm.createNotificationChannel(
        NotificationChannel(
          CHANNEL_REST_ALERT,
          "Rest Timer Alerts",
          NotificationManager.IMPORTANCE_HIGH,
        )
      )
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  companion object {
    private const val CHANNEL_WORKOUT_ONGOING = "ironlog_workout_ongoing"
    private const val CHANNEL_REST_ALERT = "ironlog_rest_alert"
    private const val NOTIFICATION_ID_ONGOING = 4001
    private const val NOTIFICATION_ID_REST_ALERT = 4002

    const val ACTION_START_OR_UPDATE_WORKOUT = "com.example.ironlog.ACTION_START_WORKOUT"
    const val ACTION_STOP_WORKOUT = "com.example.ironlog.ACTION_STOP_WORKOUT"
    const val ACTION_START_REST = "com.example.ironlog.ACTION_START_REST"
    const val ACTION_ADJUST_REST = "com.example.ironlog.ACTION_ADJUST_REST"
    const val ACTION_STOP_REST = "com.example.ironlog.ACTION_STOP_REST"

    const val EXTRA_WORKOUT_TITLE = "extra_title"
    const val EXTRA_WORKOUT_ELAPSED_SEC = "extra_elapsed_sec"
    const val EXTRA_IS_PAUSED = "extra_is_paused"
    const val EXTRA_REST_SECONDS = "extra_rest_sec"
    const val EXTRA_REST_DELTA_SEC = "extra_rest_delta_sec"
    const val EXTRA_REST_EXERCISE = "extra_rest_exercise"
    const val EXTRA_SOUND_ENABLED = "extra_sound"
    const val EXTRA_VIBRATE_ENABLED = "extra_vibrate"

    private val _state = MutableStateFlow(TimerServiceState())
    val state: StateFlow<TimerServiceState> = _state.asStateFlow()

    fun formatClock(totalSeconds: Long): String {
      val s = totalSeconds.coerceAtLeast(0L)
      val h = s / 3600
      val m = (s % 3600) / 60
      val sec = s % 60
      return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }

    fun startWorkoutService(
      context: Context,
      title: String,
      elapsedSeconds: Long,
      isPaused: Boolean,
      soundEnabled: Boolean,
      vibrateEnabled: Boolean,
    ) {
      val intent = Intent(context, WorkoutTimerService::class.java).apply {
        action = ACTION_START_OR_UPDATE_WORKOUT
        putExtra(EXTRA_WORKOUT_TITLE, title)
        putExtra(EXTRA_WORKOUT_ELAPSED_SEC, elapsedSeconds)
        putExtra(EXTRA_IS_PAUSED, isPaused)
        putExtra(EXTRA_SOUND_ENABLED, soundEnabled)
        putExtra(EXTRA_VIBRATE_ENABLED, vibrateEnabled)
      }
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      }
    }

    fun startRestTimer(
      context: Context,
      seconds: Int,
      exerciseTitle: String,
      soundEnabled: Boolean,
      vibrateEnabled: Boolean,
    ) {
      val intent = Intent(context, WorkoutTimerService::class.java).apply {
        action = ACTION_START_REST
        putExtra(EXTRA_REST_SECONDS, seconds)
        putExtra(EXTRA_REST_EXERCISE, exerciseTitle)
        putExtra(EXTRA_SOUND_ENABLED, soundEnabled)
        putExtra(EXTRA_VIBRATE_ENABLED, vibrateEnabled)
      }
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      }
    }

    fun adjustRestTimer(context: Context, deltaSeconds: Int) {
      val intent = Intent(context, WorkoutTimerService::class.java).apply {
        action = ACTION_ADJUST_REST
        putExtra(EXTRA_REST_DELTA_SEC, deltaSeconds)
      }
      runCatching { context.startService(intent) }
    }

    fun stopRestTimer(context: Context) {
      val intent = Intent(context, WorkoutTimerService::class.java).apply {
        action = ACTION_STOP_REST
      }
      runCatching { context.startService(intent) }
    }

    fun stopWorkoutService(context: Context) {
      val intent = Intent(context, WorkoutTimerService::class.java).apply {
        action = ACTION_STOP_WORKOUT
      }
      runCatching { context.startService(intent) }
    }
  }
}
