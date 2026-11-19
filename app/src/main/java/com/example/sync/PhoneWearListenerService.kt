package com.example.sync

import com.example.IronLogApplication
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PhoneWearListenerService : WearableListenerService() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onDataChanged(dataEvents: DataEventBuffer) {
    val app = application as? IronLogApplication ?: return
    dataEvents.forEach { event ->
      if (event.type == DataEvent.TYPE_CHANGED) {
        val uri = event.dataItem.uri
        if (uri.path == WearSyncManager.PATH_SYNC_WEAR_TO_PHONE) {
          val raw = DataMapItem.fromDataItem(event.dataItem)
            .dataMap
            .getString(WearSyncManager.KEY_PAYLOAD_JSON)
          if (!raw.isNullOrBlank()) {
            scope.launch {
              app.wearSyncManager.handleIncomingPayload(raw, uri.host)
            }
          }
        }
      }
    }
  }

  override fun onMessageReceived(messageEvent: MessageEvent) {
    val app = application as? IronLogApplication ?: return
    when (messageEvent.path) {
      WearSyncManager.PATH_SYNC_WEAR_TO_PHONE,
      WearSyncManager.PATH_SYNC_REQUEST -> {
        val raw = String(messageEvent.data, Charsets.UTF_8)
        scope.launch {
          app.wearSyncManager.handleIncomingPayload(raw, messageEvent.sourceNodeId)
        }
      }
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }
}
