package com.example

import android.app.Application
import com.example.data.local.IronLogDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.WorkoutRepository
import com.example.sync.WearSyncManager
import com.example.telemetry.CrashReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IronLogApplication : Application() {
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val database: IronLogDatabase by lazy { IronLogDatabase.getInstance(this) }
  val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
  val workoutRepository: WorkoutRepository by lazy {
    WorkoutRepository(database, preferencesRepository)
  }
  val wearSyncManager: WearSyncManager by lazy {
    WearSyncManager(this, workoutRepository, preferencesRepository)
  }

  override fun onCreate() {
    super.onCreate()
    CrashReporter.init(this)
    appScope.launch {
      try {
        workoutRepository.ensureSeeded()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        CrashReporter.recordNonFatal(e, area = "db_seed")
      }
    }
  }
}
