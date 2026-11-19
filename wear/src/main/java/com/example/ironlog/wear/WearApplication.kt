package com.example.ironlog.wear

import android.app.Application
import com.example.ironlog.wear.data.WearDatabase
import com.example.ironlog.wear.data.WearRepository
import com.example.ironlog.wear.sensor.HeartRateSensorMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearApplication : Application() {
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val database: WearDatabase by lazy { WearDatabase.getInstance(this) }
  val wearRepository: WearRepository by lazy { WearRepository(this, database) }
  val heartRateMonitor: HeartRateSensorMonitor by lazy { HeartRateSensorMonitor(this) }

  override fun onCreate() {
    super.onCreate()
    appScope.launch {
      wearRepository.ensureSeeded()
    }
  }
}
