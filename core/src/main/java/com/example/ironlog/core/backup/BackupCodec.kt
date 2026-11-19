package com.example.ironlog.core.backup

import com.example.ironlog.core.model.AppLanguage
import com.example.ironlog.core.model.ExerciseDto
import com.example.ironlog.core.model.SyncEnvelope
import com.example.ironlog.core.model.WorkoutSessionDto
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupCodec {

  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val compactJson = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun encodeBackupJson(envelope: SyncEnvelope): String {
    val unsigned = envelope.copy(sha256Checksum = "")
    val canonicalPayload = compactJson.encodeToString(SyncEnvelope.serializer(), unsigned)
    val signed = unsigned.copy(sha256Checksum = sha256(canonicalPayload))
    return json.encodeToString(SyncEnvelope.serializer(), signed)
  }

  fun decodeAndVerifyBackup(rawJson: String): Result<SyncEnvelope> {
    return runCatching {
      val parsed = json.decodeFromString(SyncEnvelope.serializer(), rawJson)
      require(parsed.schemaVersion in 1..1) { "Unsupported schema version: ${parsed.schemaVersion}" }
      if (parsed.sha256Checksum.isNotBlank()) {
        val unsigned = parsed.copy(sha256Checksum = "")
        val expected = sha256(compactJson.encodeToString(SyncEnvelope.serializer(), unsigned))
        require(expected == parsed.sha256Checksum) { "Backup integrity checksum mismatch" }
      }
      parsed
    }
  }

  fun encodeWearSync(envelope: SyncEnvelope): String =
    compactJson.encodeToString(SyncEnvelope.serializer(), envelope)

  fun decodeWearSync(rawJson: String): Result<SyncEnvelope> =
    runCatching { compactJson.decodeFromString(SyncEnvelope.serializer(), rawJson) }

  fun encodeWorkoutHistoryCsv(
    sessions: List<WorkoutSessionDto>,
    exercisesById: Map<String, ExerciseDto>,
    language: AppLanguage,
  ): String {
    val sb = StringBuilder()
    sb.appendLine("session_id,date,workout_name,duration_min,source_device,avg_hr,exercise_name,muscle_group,set_index,set_type,weight_kg,reps,duration_sec,distance_m,rpe,completed")
    val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    for (session in sessions.filter { !it.isActive && !it.isDeleted }.sortedByDescending { it.startedAtEpochMs }) {
      val end = session.finishedAtEpochMs ?: session.startedAtEpochMs
      val durMin = ((end - session.startedAtEpochMs - session.pausedDurationMs).coerceAtLeast(0L) / 60000L)
      val dateStr = dateFmt.format(Date(session.startedAtEpochMs))

      for (ex in session.exercises) {
        val meta = exercisesById[ex.exerciseId]
        val exName = meta?.displayName(language) ?: ex.exerciseId
        val muscle = meta?.primaryMuscle?.label(language) ?: ""
        for (set in ex.sets) {
          sb.append(csvEscape(session.id)).append(',')
          sb.append(csvEscape(dateStr)).append(',')
          sb.append(csvEscape(session.name)).append(',')
          sb.append(durMin).append(',')
          sb.append(csvEscape(session.sourceDevice)).append(',')
          sb.append(session.avgHeartRate?.toString() ?: "").append(',')
          sb.append(csvEscape(exName)).append(',')
          sb.append(csvEscape(muscle)).append(',')
          sb.append(set.setIndex + 1).append(',')
          sb.append(set.setType.name).append(',')
          sb.append(set.weightKg).append(',')
          sb.append(set.reps).append(',')
          sb.append(set.durationSeconds).append(',')
          sb.append(set.distanceMeters).append(',')
          sb.append(set.rpe?.toString() ?: "").append(',')
          sb.append(set.isCompleted)
          sb.appendLine()
        }
      }
    }
    return sb.toString()
  }

  private fun csvEscape(value: String): String {
    val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
    return if (needsQuotes) "\"${value.replace("\"", "\"\"")}\"" else value
  }
}
