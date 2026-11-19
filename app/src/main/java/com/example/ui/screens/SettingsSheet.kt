package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserPreferences
import com.example.ironlog.core.i18n.AppStrings
import com.example.ironlog.core.model.AppLanguage
import com.example.ironlog.core.model.ThemeMode
import com.example.ironlog.core.model.WeightUnit
import com.example.sync.WearSyncStatus
import com.example.ui.theme.FailureCoral
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
  strings: AppStrings,
  prefs: UserPreferences,
  wearSyncStatus: WearSyncStatus,
  onSetUnit: (WeightUnit) -> Unit,
  onSetLanguage: (AppLanguage) -> Unit,
  onSetTheme: (ThemeMode) -> Unit,
  onSetTimerSound: (Boolean) -> Unit,
  onSetTimerVibrate: (Boolean) -> Unit,
  onSetAutoRest: (Boolean) -> Unit,
  onRefreshWearNodes: () -> Unit,
  onSyncWithWatch: () -> Unit,
  onBuildJsonBackup: suspend () -> String,
  onBuildCsvHistory: suspend () -> String,
  onImportJsonBackup: (String) -> Unit,
  onWipeAllData: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showWipeConfirm by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { onRefreshWearNodes() }

  val exportJsonLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val payload = onBuildJsonBackup()
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(payload.toByteArray(Charsets.UTF_8))
        }
      }
    }
  }

  val exportCsvLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("text/csv")
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val payload = onBuildCsvHistory()
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(payload.toByteArray(Charsets.UTF_8))
        }
      }
    }
  }

  val importJsonLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      val raw = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
      if (!raw.isNullOrBlank()) {
        onImportJsonBackup(raw)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.settingsTitle) },
    text = {
      Column(
        modifier = Modifier
          .height(450.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Units
        Text(strings.unitsLabel, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          WeightUnit.entries.forEach { u ->
            FilterChip(
              selected = prefs.weightUnit == u,
              onClick = { onSetUnit(u) },
              label = { Text(u.symbol(prefs.language).uppercase()) },
              modifier = Modifier.testTag("unit_chip_${u.name}"),
            )
          }
        }

        // Language
        Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = prefs.language == AppLanguage.RU,
            onClick = { onSetLanguage(AppLanguage.RU) },
            label = { Text("Русский") },
            modifier = Modifier.testTag("lang_chip_ru"),
          )
          FilterChip(
            selected = prefs.language == AppLanguage.EN,
            onClick = { onSetLanguage(AppLanguage.EN) },
            label = { Text("English") },
            modifier = Modifier.testTag("lang_chip_en"),
          )
        }

        // Theme
        Text(strings.themeLabel, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FilterChip(selected = prefs.themeMode == ThemeMode.DARK, onClick = { onSetTheme(ThemeMode.DARK) }, label = { Text(strings.themeDark) })
          FilterChip(selected = prefs.themeMode == ThemeMode.LIGHT, onClick = { onSetTheme(ThemeMode.LIGHT) }, label = { Text(strings.themeLight) })
          FilterChip(selected = prefs.themeMode == ThemeMode.SYSTEM, onClick = { onSetTheme(ThemeMode.SYSTEM) }, label = { Text(strings.themeSystem) })
        }

        HorizontalDivider()

        // Timers
        SettingToggleRow(strings.timerSoundLabel, prefs.timerSoundEnabled, onSetTimerSound)
        SettingToggleRow(strings.timerVibrateLabel, prefs.timerVibrateEnabled, onSetTimerVibrate)
        SettingToggleRow(strings.autoRestTimerLabel, prefs.autoStartRestTimer, onSetAutoRest)

        HorizontalDivider()

        // Wear OS Local Sync
        Text(strings.wearSyncTitle, style = MaterialTheme.typography.titleSmall)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
              val statusTxt = when (wearSyncStatus) {
                WearSyncStatus.Idle -> strings.syncIdle
                WearSyncStatus.Syncing -> "..."
                is WearSyncStatus.Connected -> "${strings.syncReady}: ${wearSyncStatus.nodeNames.ifEmpty { listOf("0") }.joinToString()}"
                is WearSyncStatus.Unavailable -> wearSyncStatus.reason
              }
              Text(statusTxt, style = MaterialTheme.typography.bodySmall)
            }
            Button(
              onClick = onSyncWithWatch,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("wear_sync_button"),
            ) {
              Icon(Icons.Filled.Sync, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text(strings.syncNow)
            }
          }
        }

        HorizontalDivider()

        // Local Backup & Export (SAF zero storage permission)
        Text(strings.backupTitle, style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
          onClick = { exportJsonLauncher.launch("ironlog_backup.json") },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.Upload, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(strings.exportJson)
        }
        OutlinedButton(
          onClick = { exportCsvLauncher.launch("ironlog_history.csv") },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.Upload, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(strings.exportCsv)
        }
        OutlinedButton(
          onClick = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.Download, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(strings.importJson)
        }

        HorizontalDivider()

        // Wipe Local Data (NFR-12)
        OutlinedButton(
          onClick = { showWipeConfirm = true },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = FailureCoral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.DeleteForever, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(strings.wipeAllData)
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("OK") }
    },
  )

  if (showWipeConfirm) {
    AlertDialog(
      onDismissRequest = { showWipeConfirm = false },
      title = { Text(strings.wipeConfirmTitle) },
      text = { Text(strings.wipeConfirmMessage) },
      confirmButton = {
        Button(
          onClick = {
            showWipeConfirm = false
            onWipeAllData()
          },
          colors = ButtonDefaults.buttonColors(containerColor = FailureCoral),
        ) { Text(strings.confirm) }
      },
      dismissButton = {
        TextButton(onClick = { showWipeConfirm = false }) { Text(strings.cancel) }
      },
    )
  }
}

@Composable
private fun SettingToggleRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
