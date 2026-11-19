package com.example.ironlog.wear.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class HeartRateSensorMonitor(context: Context) {
  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val hrSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)

  val isAvailable: Boolean get() = hrSensor != null

  fun heartRateFlow(): Flow<Int> = callbackFlow {
    val sensor = hrSensor
    if (sensorManager == null || sensor == null) {
      close()
      return@callbackFlow
    }
    val listener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent?) {
        val raw = event?.values?.firstOrNull() ?: return
        val bpm = raw.toInt()
        if (bpm in 30..240) {
          trySend(bpm)
        }
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    awaitClose {
      sensorManager.unregisterListener(listener)
    }
  }
}
