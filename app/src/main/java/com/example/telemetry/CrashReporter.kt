package com.example.telemetry

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import com.example.data.preferences.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import kotlinx.coroutines.CancellationException

/**
 * Lightweight, privacy-respecting wrapper around Firebase Crashlytics.
 * Caches crash reports offline on-device and uploads automatically when network returns.
 */
object CrashReporter {
  @Volatile
  private var initialized: Boolean = false

  private fun isRobolectric(): Boolean =
    Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

  fun init(context: Context) {
    if (initialized) return
    runCatching {
      val appContext = context.applicationContext ?: context
      if (FirebaseApp.getApps(appContext).isEmpty()) {
        FirebaseApp.initializeApp(appContext)
      }
      val crashlytics = Firebase.crashlytics
      crashlytics.isCrashlyticsCollectionEnabled = !isRobolectric()
      crashlytics.setCustomKeys {
        key("build_type", BuildConfig.BUILD_TYPE)
        key("app_version", BuildConfig.VERSION_NAME)
        key("version_code", BuildConfig.VERSION_CODE)
      }
      initialized = true
    }
  }

  fun updatePreferences(prefs: UserPreferences) {
    runCatching {
      if (!initialized) return
      Firebase.crashlytics.setCustomKeys {
        key("weight_unit", prefs.weightUnit.name)
        key("app_language", prefs.language.name)
        key("theme_mode", prefs.themeMode.name)
        key("rm_formula", prefs.preferred1RmFormula.name)
      }
    }
  }

  fun log(message: String) {
    if (message.isBlank()) return
    runCatching {
      if (!initialized) return
      Firebase.crashlytics.log(message)
    }
  }

  fun recordNonFatal(
    throwable: Throwable,
    area: String,
    extras: Map<String, String> = emptyMap(),
  ) {
    if (throwable is CancellationException) throw throwable
    runCatching {
      if (!initialized) return
      val keys = CustomKeysAndValues.Builder().apply {
        putString("area", area)
        extras.forEach { (k, v) -> putString(k, v) }
      }.build()
      Firebase.crashlytics.recordException(throwable, keys)
    }
  }
}
