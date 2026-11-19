package com.example.sync

import android.content.Context
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.WorkoutRepository
import com.example.ironlog.core.backup.BackupCodec
import com.example.ironlog.core.model.WearSyncAck
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
  suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { err -> cont.resumeWithException(err) }
    addOnCanceledListener { cont.cancel() }
  }

sealed class WearSyncStatus {
  data object Idle : WearSyncStatus()
  data object Syncing : WearSyncStatus()
  data class Connected(val nodeNames: List<String>, val lastSyncedMs: Long) : WearSyncStatus()
  data class Unavailable(val reason: String) : WearSyncStatus()
}

class WearSyncManager(
  private val context: Context,
  private val repository: WorkoutRepository,
  private val prefs: UserPreferencesRepository,
) {
  private val _status = MutableStateFlow<WearSyncStatus>(WearSyncStatus.Idle)
  val status: StateFlow<WearSyncStatus> = _status.asStateFlow()

  private val json = Json { ignoreUnknownKeys = true }

  suspend fun refreshConnectedNodes(): List<String> {
    return runCatching {
      val availability = GoogleApiAvailability.getInstance()
      if (availability.isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
        _status.value = WearSyncStatus.Unavailable("Wearable Data Layer unavailable")
        return emptyList()
      }
      val nodeClient = Wearable.getNodeClient(context)
      availability.checkApiAvailability(nodeClient).awaitTask()
      val nodes = nodeClient.connectedNodes.awaitTask()
      val names = nodes.map { it.displayName }
      _status.value = WearSyncStatus.Connected(names, System.currentTimeMillis())
      names
    }.getOrElse {
      _status.value = WearSyncStatus.Unavailable("Wearable Data Layer unavailable")
      emptyList()
    }
  }

  suspend fun syncWithWatch(): Result<Pair<Int, Int>> {
    _status.value = WearSyncStatus.Syncing
    return runCatching {
      val availability = GoogleApiAvailability.getInstance()
      check(availability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
        "Google Play Services unavailable"
      }
      val dataClient = Wearable.getDataClient(context)
      val nodeClient = Wearable.getNodeClient(context)
      availability.checkApiAvailability(dataClient).awaitTask()

      val nodes = nodeClient.connectedNodes.awaitTask()
      val envelope = repository.buildFullEnvelope(onlyPendingSync = false)
      val payloadJson = BackupCodec.encodeWearSync(envelope)

      val request = PutDataMapRequest.create(PATH_SYNC_PHONE_TO_WEAR).apply {
        dataMap.putString(KEY_PAYLOAD_JSON, payloadJson)
        dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
      }.asPutDataRequest().setUrgent()

      dataClient.putDataItem(request).awaitTask()

      val msgClient = Wearable.getMessageClient(context)
      nodes.forEach { node ->
        runCatching {
          msgClient.sendMessage(node.id, PATH_SYNC_REQUEST, payloadJson.toByteArray(Charsets.UTF_8)).awaitTask()
        }
      }

      val syncedIds = envelope.routines.map { it.id } + envelope.sessions.map { it.id }
      repository.markSynced(syncedIds)
      _status.value = WearSyncStatus.Connected(nodes.map { it.displayName }, System.currentTimeMillis())
      envelope.routines.size to envelope.sessions.size
    }.onFailure { err ->
      _status.value = WearSyncStatus.Unavailable(err.message ?: "Sync failed")
    }
  }

  suspend fun handleIncomingPayload(rawJson: String, sourceNodeId: String?) {
    val parsed = BackupCodec.decodeWearSync(rawJson).getOrNull() ?: return
    val (routinesImported, sessionsImported) = repository.mergeIncomingEnvelope(parsed)
    if (routinesImported > 0 || sessionsImported > 0) {
      prefs.setLastSyncEpochMs(System.currentTimeMillis())
    }
    if (!sourceNodeId.isNullOrBlank()) {
      runCatching {
        val ack = WearSyncAck(
          receiverDeviceId = prefs.getOrCreateDeviceId(),
          acknowledgedSessionIds = parsed.sessions.map { it.id },
          acknowledgedRoutineIds = parsed.routines.map { it.id },
        )
        val bytes = json.encodeToString(WearSyncAck.serializer(), ack).toByteArray(Charsets.UTF_8)
        Wearable.getMessageClient(context).sendMessage(sourceNodeId, PATH_SYNC_ACK, bytes).awaitTask()
      }
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
