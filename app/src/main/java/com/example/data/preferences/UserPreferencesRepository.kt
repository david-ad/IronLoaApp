package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ironlog.core.model.AppLanguage
import com.example.ironlog.core.model.OneRmFormula
import com.example.ironlog.core.model.ThemeMode
import com.example.ironlog.core.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.ironLogDataStore: DataStore<Preferences> by preferencesDataStore(name = "ironlog_prefs")

data class UserPreferences(
  val weightUnit: WeightUnit = WeightUnit.KG,
  val language: AppLanguage = AppLanguage.RU,
  val themeMode: ThemeMode = ThemeMode.DARK,
  val timerSoundEnabled: Boolean = true,
  val timerVibrateEnabled: Boolean = true,
  val autoStartRestTimer: Boolean = true,
  val preferred1RmFormula: OneRmFormula = OneRmFormula.EPLEY,
  val lastSyncEpochMs: Long = 0L,
  val restEndElapsedMs: Long = 0L,
  val restTotalSeconds: Int = 90,
  val restExerciseTitle: String = "",
)

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    val LANGUAGE = stringPreferencesKey("app_language")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val TIMER_SOUND = booleanPreferencesKey("timer_sound")
    val TIMER_VIBRATE = booleanPreferencesKey("timer_vibrate")
    val AUTO_REST = booleanPreferencesKey("auto_rest_timer")
    val ONE_RM_FORMULA = stringPreferencesKey("one_rm_formula")
    val DEVICE_ID = stringPreferencesKey("device_id")
    val LAST_SYNC = longPreferencesKey("last_sync_ms")
    val REST_END_ELAPSED = longPreferencesKey("rest_end_elapsed_ms")
    val REST_TOTAL_SEC = intPreferencesKey("rest_total_sec")
    val REST_EXERCISE = stringPreferencesKey("rest_exercise")
  }

  val preferencesFlow: Flow<UserPreferences> = context.ironLogDataStore.data.map { prefs ->
    UserPreferences(
      weightUnit = prefs[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
        ?: WeightUnit.KG,
      language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
        ?: AppLanguage.RU,
      themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
        ?: ThemeMode.DARK,
      timerSoundEnabled = prefs[Keys.TIMER_SOUND] ?: true,
      timerVibrateEnabled = prefs[Keys.TIMER_VIBRATE] ?: true,
      autoStartRestTimer = prefs[Keys.AUTO_REST] ?: true,
      preferred1RmFormula = prefs[Keys.ONE_RM_FORMULA]?.let { runCatching { OneRmFormula.valueOf(it) }.getOrNull() }
        ?: OneRmFormula.EPLEY,
      lastSyncEpochMs = prefs[Keys.LAST_SYNC] ?: 0L,
      restEndElapsedMs = prefs[Keys.REST_END_ELAPSED] ?: 0L,
      restTotalSeconds = prefs[Keys.REST_TOTAL_SEC] ?: 90,
      restExerciseTitle = prefs[Keys.REST_EXERCISE] ?: "",
    )
  }

  suspend fun getOrCreateDeviceId(): String {
    val existing = context.ironLogDataStore.data.first()[Keys.DEVICE_ID]
    if (!existing.isNullOrBlank()) return existing
    val created = "phone-${UUID.randomUUID()}"
    context.ironLogDataStore.edit { it[Keys.DEVICE_ID] = created }
    return created
  }

  suspend fun setWeightUnit(unit: WeightUnit) {
    context.ironLogDataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.ironLogDataStore.edit { it[Keys.LANGUAGE] = language.name }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.ironLogDataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setTimerSound(enabled: Boolean) {
    context.ironLogDataStore.edit { it[Keys.TIMER_SOUND] = enabled }
  }

  suspend fun setTimerVibrate(enabled: Boolean) {
    context.ironLogDataStore.edit { it[Keys.TIMER_VIBRATE] = enabled }
  }

  suspend fun setAutoRestTimer(enabled: Boolean) {
    context.ironLogDataStore.edit { it[Keys.AUTO_REST] = enabled }
  }

  suspend fun setOneRmFormula(formula: OneRmFormula) {
    context.ironLogDataStore.edit { it[Keys.ONE_RM_FORMULA] = formula.name }
  }

  suspend fun setLastSyncEpochMs(ts: Long) {
    context.ironLogDataStore.edit { it[Keys.LAST_SYNC] = ts }
  }

  suspend fun saveRestTimer(endElapsedMs: Long, totalSeconds: Int, exerciseTitle: String) {
    context.ironLogDataStore.edit {
      it[Keys.REST_END_ELAPSED] = endElapsedMs
      it[Keys.REST_TOTAL_SEC] = totalSeconds
      it[Keys.REST_EXERCISE] = exerciseTitle
    }
  }

  suspend fun clearRestTimer() {
    context.ironLogDataStore.edit {
      it[Keys.REST_END_ELAPSED] = 0L
      it[Keys.REST_EXERCISE] = ""
    }
  }
}
