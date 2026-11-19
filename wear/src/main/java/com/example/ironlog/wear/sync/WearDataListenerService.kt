package com.example.ironlog.wear.sync

import com.example.ironlog.wear.WearApplication
import com.example.ironlog.wear.data.WearRepository
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

class WearDataListenerService : WearableListenerService() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onDataChanged(dataEvents: DataEventBuffer) {
    val app = application as? WearApplication ?: return
    dataEvents.forEach { event ->
      if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearRepository.PATH_SYNC_PHONE_TO_WEAR) {
        val raw = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(WearRepository.KEY_PAYLOAD_JSON)
        if (!raw.isNullOrBlank()) {
          scope.launch { app.wearRepository.handlePhonePayload(raw) }
        }
      }
    }
  }

  override fun onMessageReceived(messageEvent: MessageEvent) {
    val app = application as? WearApplication ?: return
    when (messageEvent.path) {
      WearRepository.PATH_SYNC_REQUEST,
      WearRepository.PATH_SYNC_PHONE_TO_WEAR -> {
        val raw = String(messageEvent.data, Charsets.UTF_8)
        scope.launch { app.wearRepository.handlePhonePayload(raw) }
      }
      WearRepository.PATH_SYNC_ACK -> {
        scope.launch { app.wearRepository.handlePhoneAck(messageEvent.data) }
      }
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }
}
