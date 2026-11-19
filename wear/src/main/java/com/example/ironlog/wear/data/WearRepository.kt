package com.example.ironlog.wear.data

import android.content.Context
import android.os.SystemClock
import com.example.ironlog.core.backup.BackupCodec
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.HeartRateSampleDto
import com.example.ironlog.core.model.RoutineDto
import com.example.ironlog.core.model.SetType
import com.example.ironlog.core.model.SyncEnvelope
import com.example.ironlog.core.model.WearSyncAck
import com.example.ironlog.core.model.WorkoutExerciseDto
import com.example.ironlog.core.model.WorkoutSessionDto
import com.example.ironlog.core.model.WorkoutSetDto
import com.example.ironlog.core.seed.ExerciseCatalogSeed
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
  suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { err -> cont.resumeWithException(err) }
    addOnCanceledListener { cont.cancel() }
  }

class WearRepository(
  private val context: Context,
  private val db: WearDatabase,
) {
  private val dao = db.dao()
  private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
  private val wearDeviceId = "wear-watch"

  val exercisesFlow: Flow<List<ExerciseDto>> = dao.observeExercises().map { rows ->
    rows.mapNotNull { runCatching { json.decodeFromString(ExerciseDto.serializer(), it.jsonPayload) }.getOrNull() }
  }

  val routinesFlow: Flow<List<RoutineDto>> = dao.observeRoutines().map { rows ->
    rows.mapNotNull { runCatching { json.decodeFromString(RoutineDto.serializer(), it.jsonPayload) }.getOrNull() }
  }

  val activeSessionFlow: Flow<WorkoutSessionDto?> = dao.observeActiveSession().map { row ->
    row?.let { runCatching { json.decodeFromString(WorkoutSessionDto.serializer(), it.jsonPayload) }.getOrNull() }
  }

  val completedSessionsFlow: Flow<List<WorkoutSessionDto>> = dao.observeCompletedSessions().map { rows ->
    rows.mapNotNull { runCatching { json.decodeFromString(WorkoutSessionDto.serializer(), it.jsonPayload) }.getOrNull() }
  }

  suspend fun ensureSeeded() {
    if (dao.countExercises() == 0) {
      dao.upsertExercises(
        ExerciseCatalogSeed.defaultExercises.map { ex ->
          WearExerciseEntity(ex.id, json.encodeToString(ExerciseDto.serializer(), ex), ex.updatedAt)
        }
      )
      ExerciseCatalogSeed.defaultRoutines.forEach { r ->
        dao.upsertRoutine(
          WearRoutineEntity(
            id = r.id,
            jsonPayload = json.encodeToString(RoutineDto.serializer(), r),
            sortOrder = r.sortOrder,
            updatedAt = r.updatedAt,
            isDeleted = r.isDeleted,
          )
        )
      }
    }
  }

  suspend fun startRoutineWorkout(routine: RoutineDto, history: List<WorkoutSessionDto>): WorkoutSessionDto {
    val now = System.currentTimeMillis()
    val sessionId = UUID.randomUUID().toString()
    val exercises = routine.exercises.sortedBy { it.sortOrder }.mapIndexed { idx, re ->
      val weId = UUID.randomUUID().toString()
      val prevSets = findPreviousSets(re.exerciseId, history)
      val sets = (0 until re.targetSets.coerceAtLeast(1)).map { sIdx ->
        val prev = prevSets.getOrNull(sIdx)
        WorkoutSetDto(
          id = UUID.randomUUID().toString(),
          workoutExerciseId = weId,
          setIndex = sIdx,
          setType = SetType.NORMAL,
          weightKg = prev?.weightKg ?: re.targetWeightKg,
          reps = prev?.reps ?: re.targetReps,
          isCompleted = false,
        )
      }
      WorkoutExerciseDto(
        id = weId,
        sessionId = sessionId,
        exerciseId = re.exerciseId,
        sortOrder = idx,
        supersetGroupId = re.supersetGroupId,
        restSecondsOverride = re.restSeconds,
        sets = sets,
      )
    }
    val session = WorkoutSessionDto(
      id = sessionId,
      routineId = routine.id,
      name = routine.name,
      startedAtEpochMs = now,
      isActive = true,
      sourceDevice = "wear",
      exercises = exercises,
      version = 1L,
      updatedAt = now,
    )
    saveSession(session, pendingSync = false)
    return session
  }

  suspend fun startEmptyWorkout(firstExerciseId: String): WorkoutSessionDto {
    val now = System.currentTimeMillis()
    val sessionId = UUID.randomUUID().toString()
    val weId = UUID.randomUUID().toString()
    val session = WorkoutSessionDto(
      id = sessionId,
      name = "Quick Lift",
      startedAtEpochMs = now,
      isActive = true,
      sourceDevice = "wear",
      exercises = listOf(
        WorkoutExerciseDto(
          id = weId,
          sessionId = sessionId,
          exerciseId = firstExerciseId,
          sortOrder = 0,
          restSecondsOverride = 90,
          sets = listOf(
            WorkoutSetDto(
              id = UUID.randomUUID().toString(),
              workoutExerciseId = weId,
              setIndex = 0,
              weightKg = 20.0,
              reps = 10,
            )
          ),
        )
      ),
      version = 1L,
      updatedAt = now,
    )
    saveSession(session, pendingSync = false)
    return session
  }

  suspend fun saveSession(session: WorkoutSessionDto, pendingSync: Boolean = !session.isActive) {
    val normalized = session.copy(version = session.version + 1, updatedAt = System.currentTimeMillis())
    dao.upsertSession(
      WearSessionEntity(
        id = normalized.id,
        isActive = normalized.isActive,
        startedAtEpochMs = normalized.startedAtEpochMs,
        jsonPayload = json.encodeToString(WorkoutSessionDto.serializer(), normalized),
        updatedAt = normalized.updatedAt,
        pendingSync = pendingSync,
        isDeleted = normalized.isDeleted,
      )
    )
  }

  suspend fun finishSession(session: WorkoutSessionDto, hrSamples: List<HeartRateSampleDto>): WorkoutSessionDto {
    val now = System.currentTimeMillis()
    val extraPause = if (session.isPaused && session.pauseStartedAtElapsedMs != null) {
      (SystemClock.elapsedRealtime() - session.pauseStartedAtElapsedMs!!).coerceAtLeast(0L)
    } else 0L
    val avgHr = hrSamples.takeIf { it.isNotEmpty() }?.let { list -> list.sumOf { it.bpm } / list.size }
    val maxHr = hrSamples.maxOfOrNull { it.bpm }
    val finished = session.copy(
      finishedAtEpochMs = now,
      pausedDurationMs = session.pausedDurationMs + extraPause,
      pauseStartedAtElapsedMs = null,
      isActive = false,
      isPaused = false,
      avgHeartRate = avgHr,
      maxHeartRate = maxHr,
      heartRateSamples = hrSamples,
    )
    saveSession(finished, pendingSync = true)
    pushPendingToPhone()
    return finished
  }

  fun findPreviousSets(exerciseId: String, history: List<WorkoutSessionDto>): List<WorkoutSetDto> {
    return history
      .asSequence()
      .filter { !it.isActive && !it.isDeleted }
      .sortedByDescending { it.startedAtEpochMs }
      .mapNotNull { s -> s.exercises.firstOrNull { it.exerciseId == exerciseId } }
      .firstOrNull { it.sets.any { set -> set.isCompleted } }
      ?.sets
      ?.filter { it.isCompleted }
      ?: emptyList()
  }

  suspend fun pushPendingToPhone(): Boolean {
    return runCatching {
      val availability = GoogleApiAvailability.getInstance()
      if (availability.isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
        return false
      }
      val dataClient = Wearable.getDataClient(context)
      availability.checkApiAvailability(dataClient).awaitTask()
      val pending = dao.getPendingSyncSessions()
        .mapNotNull { runCatching { json.decodeFromString(WorkoutSessionDto.serializer(), it.jsonPayload) }.getOrNull() }
      if (pending.isEmpty()) return true

      val envelope = SyncEnvelope(
        schemaVersion = 1,
        sourceDeviceId = wearDeviceId,
        sourceDeviceType = "wear",
        sessions = pending,
      )
      val payload = BackupCodec.encodeWearSync(envelope)
      val req = PutDataMapRequest.create(PATH_SYNC_WEAR_TO_PHONE).apply {
        dataMap.putString(KEY_PAYLOAD_JSON, payload)
        dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
      }.asPutDataRequest().setUrgent()
      dataClient.putDataItem(req).awaitTask()

      val msgClient = Wearable.getMessageClient(context)
      val nodes = Wearable.getNodeClient(context).connectedNodes.awaitTask()
      nodes.forEach { node ->
        runCatching {
          msgClient.sendMessage(node.id, PATH_SYNC_WEAR_TO_PHONE, payload.toByteArray(Charsets.UTF_8)).awaitTask()
        }
      }
      true
    }.getOrElse { false }
  }

  suspend fun handlePhonePayload(rawJson: String) {
    val env = BackupCodec.decodeWearSync(rawJson).getOrNull() ?: return
    if (env.sourceDeviceId == wearDeviceId) return

    val curExercises = dao.getExercisesOnce().associateBy { it.id }
    val newExercises = env.exercises.filter { inc ->
      val cur = curExercises[inc.id]
      cur == null || inc.updatedAt > cur.updatedAt
    }.map { inc ->
      WearExerciseEntity(inc.id, json.encodeToString(ExerciseDto.serializer(), inc), inc.updatedAt)
    }
    if (newExercises.isNotEmpty()) dao.upsertExercises(newExercises)

    val curRoutines = dao.getRoutinesOnce().associateBy { it.id }
    env.routines.forEach { inc ->
      val cur = curRoutines[inc.id]
      if (cur == null || inc.updatedAt > cur.updatedAt) {
        dao.upsertRoutine(
          WearRoutineEntity(
            id = inc.id,
            jsonPayload = json.encodeToString(RoutineDto.serializer(), inc),
            sortOrder = inc.sortOrder,
            updatedAt = inc.updatedAt,
            isDeleted = inc.isDeleted,
          )
        )
      }
    }

    val curSessions = dao.getAllSessionsOnce().associateBy { it.id }
    env.sessions.filter { !it.isActive }.forEach { inc ->
      val cur = curSessions[inc.id]
      if (cur == null || inc.updatedAt > cur.updatedAt) {
        dao.upsertSession(
          WearSessionEntity(
            id = inc.id,
            isActive = false,
            startedAtEpochMs = inc.startedAtEpochMs,
            jsonPayload = json.encodeToString(WorkoutSessionDto.serializer(), inc),
            updatedAt = inc.updatedAt,
            pendingSync = false,
            isDeleted = inc.isDeleted,
          )
        )
      }
    }
  }

  suspend fun handlePhoneAck(ackBytes: ByteArray) {
    val ack = runCatching {
      json.decodeFromString(WearSyncAck.serializer(), String(ackBytes, Charsets.UTF_8))
    }.getOrNull() ?: return
    if (ack.acknowledgedSessionIds.isNotEmpty()) {
      dao.markSessionsSynced(ack.acknowledgedSessionIds)
    }
  }

  companion object {
    const val PATH_SYNC_PHONE_TO_WEAR = "/ironlog/sync/phone_to_wear"
    const val PATH_SYNC_WEAR_TO_PHONE = "/ironlog/sync/wear_to_phone"
    const val PATH_SYNC_REQUEST = "/ironlog/sync/request"
    const val PATH_SYNC_ACK = "/ironlog/sync/ack"
    const val KEY_PAYLOAD_JSON = "payload_json"
    const val KEY_TIMESTAMP = "timestamp"
  }
}
