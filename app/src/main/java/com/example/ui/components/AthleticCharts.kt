package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ironlog.core.math.PlateCalculationResult
import com.example.ironlog.core.model.AppLanguage
import com.example.ironlog.core.model.IllustrationPose
import com.example.ironlog.core.model.MuscleGroup
import com.example.ui.theme.MonoNumericTextStyle
import kotlin.math.roundToInt

@Composable
fun ExercisePoseAnimationCanvas(
  pose: IllustrationPose,
  modifier: Modifier = Modifier,
  animate: Boolean = true,
) {
  val transition = rememberInfiniteTransition(label = "exercise_pose")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pose_phase",
  )
  val t = if (animate) phase else 0.5f

  val primary = MaterialTheme.colorScheme.primary
  val accent = MaterialTheme.colorScheme.tertiary
  val bodyColor = MaterialTheme.colorScheme.onSurface
  val surfaceBg = MaterialTheme.colorScheme.surfaceVariant

  Canvas(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(surfaceBg),
  ) {
    val w = size.width
    val h = size.height
    val stroke = (minOf(w, h) * 0.055f).coerceAtLeast(3f)

    // Subtle grid lines
    drawLine(
      color = bodyColor.copy(alpha = 0.08f),
      start = Offset(0f, h * 0.85f),
      end = Offset(w, h * 0.85f),
      strokeWidth = 2f,
    )

    when (pose) {
      IllustrationPose.BENCH_PRESS -> {
        val barY = h * (0.28f + 0.18f * t)
        // Bench
        drawRoundRect(
          color = bodyColor.copy(alpha = 0.35f),
          topLeft = Offset(w * 0.2f, h * 0.68f),
          size = Size(w * 0.6f, h * 0.08f),
          cornerRadius = CornerRadius(8f, 8f),
        )
        // Lifter torso & head
        drawCircle(bodyColor, radius = h * 0.07f, center = Offset(w * 0.28f, h * 0.58f))
        drawLine(bodyColor, Offset(w * 0.34f, h * 0.62f), Offset(w * 0.68f, h * 0.62f), stroke, StrokeCap.Round)
        // Arms pushing bar
        drawLine(bodyColor, Offset(w * 0.48f, h * 0.60f), Offset(w * 0.48f, barY), stroke, StrokeCap.Round)
        // Barbell & plates
        drawLine(primary, Offset(w * 0.22f, barY), Offset(w * 0.78f, barY), stroke * 0.9f, StrokeCap.Round)
        drawRoundRect(primary, Offset(w * 0.24f, barY - h * 0.11f), Size(w * 0.06f, h * 0.22f), CornerRadius(4f, 4f))
        drawRoundRect(primary, Offset(w * 0.70f, barY - h * 0.11f), Size(w * 0.06f, h * 0.22f), CornerRadius(4f, 4f))
      }

      IllustrationPose.SQUAT, IllustrationPose.LUNGE -> {
        val hipY = h * (0.44f + 0.18f * t)
        val kneeX = w * (0.56f + 0.06f * t)
        drawCircle(bodyColor, radius = h * 0.07f, center = Offset(w * 0.46f, hipY - h * 0.22f))
        // Torso
        drawLine(bodyColor, Offset(w * 0.46f, hipY - h * 0.14f), Offset(w * 0.42f, hipY), stroke, StrokeCap.Round)
        // Thigh + Shin
        drawLine(bodyColor, Offset(w * 0.42f, hipY), Offset(kneeX, h * 0.66f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(kneeX, h * 0.66f), Offset(w * 0.48f, h * 0.85f), stroke, StrokeCap.Round)
        // Barbell on traps
        drawLine(primary, Offset(w * 0.26f, hipY - h * 0.14f), Offset(w * 0.66f, hipY - h * 0.14f), stroke * 0.85f, StrokeCap.Round)
        drawCircle(primary, radius = h * 0.09f, center = Offset(w * 0.28f, hipY - h * 0.14f))
        drawCircle(primary, radius = h * 0.09f, center = Offset(w * 0.64f, hipY - h * 0.14f))
      }

      IllustrationPose.DEADLIFT, IllustrationPose.ROW -> {
        val barY = h * (0.72f - 0.20f * t)
        val shoulderY = h * (0.46f - 0.16f * t)
        drawCircle(bodyColor, radius = h * 0.065f, center = Offset(w * 0.60f, shoulderY - h * 0.09f))
        drawLine(bodyColor, Offset(w * 0.36f, h * 0.56f), Offset(w * 0.56f, shoulderY), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.36f, h * 0.56f), Offset(w * 0.44f, h * 0.85f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.54f, shoulderY), Offset(w * 0.54f, barY), stroke * 0.85f, StrokeCap.Round)
        // Barbell
        drawLine(primary, Offset(w * 0.36f, barY), Offset(w * 0.72f, barY), stroke * 0.85f, StrokeCap.Round)
        drawCircle(primary, radius = h * 0.10f, center = Offset(w * 0.54f, barY), style = Stroke(width = stroke * 0.75f))
      }

      IllustrationPose.OVERHEAD_PRESS -> {
        val barY = h * (0.36f - 0.20f * t)
        drawCircle(bodyColor, radius = h * 0.065f, center = Offset(w * 0.5f, h * 0.36f))
        drawLine(bodyColor, Offset(w * 0.5f, h * 0.44f), Offset(w * 0.5f, h * 0.66f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.5f, h * 0.66f), Offset(w * 0.44f, h * 0.85f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.5f, h * 0.66f), Offset(w * 0.56f, h * 0.85f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.36f, h * 0.46f), Offset(w * 0.36f, barY), stroke * 0.8f, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.64f, h * 0.46f), Offset(w * 0.64f, barY), stroke * 0.8f, StrokeCap.Round)
        drawLine(primary, Offset(w * 0.22f, barY), Offset(w * 0.78f, barY), stroke * 0.85f, StrokeCap.Round)
        drawRoundRect(primary, Offset(w * 0.22f, barY - h * 0.08f), Size(w * 0.06f, h * 0.16f), CornerRadius(3f, 3f))
        drawRoundRect(primary, Offset(w * 0.72f, barY - h * 0.08f), Size(w * 0.06f, h * 0.16f), CornerRadius(3f, 3f))
      }

      IllustrationPose.PULL_UP -> {
        val bodyLift = h * (0.14f * t)
        // Top bar
        drawLine(accent, Offset(w * 0.2f, h * 0.16f), Offset(w * 0.8f, h * 0.16f), stroke, StrokeCap.Round)
        val headY = h * 0.34f - bodyLift
        drawCircle(bodyColor, radius = h * 0.06f, center = Offset(w * 0.5f, headY))
        drawLine(bodyColor, Offset(w * 0.36f, h * 0.16f), Offset(w * 0.44f, headY + h * 0.08f), stroke * 0.8f, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.64f, h * 0.16f), Offset(w * 0.56f, headY + h * 0.08f), stroke * 0.8f, StrokeCap.Round)
        drawLine(primary, Offset(w * 0.5f, headY + h * 0.08f), Offset(w * 0.5f, headY + h * 0.30f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.5f, headY + h * 0.30f), Offset(w * 0.46f, headY + h * 0.48f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.5f, headY + h * 0.30f), Offset(w * 0.54f, headY + h * 0.48f), stroke, StrokeCap.Round)
      }

      IllustrationPose.CURL, IllustrationPose.EXTENSION -> {
        val handY = h * (0.58f - 0.22f * t)
        val handX = w * (0.64f - 0.08f * t)
        drawCircle(bodyColor, radius = h * 0.065f, center = Offset(w * 0.44f, h * 0.26f))
        drawLine(bodyColor, Offset(w * 0.44f, h * 0.34f), Offset(w * 0.44f, h * 0.64f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.44f, h * 0.64f), Offset(w * 0.44f, h * 0.85f), stroke, StrokeCap.Round)
        // Upper arm & flexing forearm
        drawLine(bodyColor, Offset(w * 0.46f, h * 0.38f), Offset(w * 0.48f, h * 0.56f), stroke, StrokeCap.Round)
        drawLine(primary, Offset(w * 0.48f, h * 0.56f), Offset(handX, handY), stroke, StrokeCap.Round)
        drawCircle(primary, radius = h * 0.065f, center = Offset(handX, handY))
      }

      IllustrationPose.PLANK -> {
        val pulse = h * 0.02f * t
        drawCircle(bodyColor, radius = h * 0.06f, center = Offset(w * 0.74f, h * 0.58f - pulse))
        drawLine(primary, Offset(w * 0.24f, h * 0.72f), Offset(w * 0.68f, h * 0.62f - pulse), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.68f, h * 0.62f - pulse), Offset(w * 0.68f, h * 0.82f), stroke * 0.85f, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.68f, h * 0.82f), Offset(w * 0.76f, h * 0.82f), stroke * 0.85f, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.24f, h * 0.72f), Offset(w * 0.22f, h * 0.84f), stroke * 0.85f, StrokeCap.Round)
      }

      IllustrationPose.CARRY, IllustrationPose.CARDIO -> {
        val stride = w * 0.08f * (t - 0.5f)
        drawCircle(bodyColor, radius = h * 0.065f, center = Offset(w * 0.5f, h * 0.26f))
        drawLine(bodyColor, Offset(w * 0.5f, h * 0.34f), Offset(w * 0.48f, h * 0.58f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.48f, h * 0.58f), Offset(w * 0.42f + stride, h * 0.84f), stroke, StrokeCap.Round)
        drawLine(bodyColor, Offset(w * 0.48f, h * 0.58f), Offset(w * 0.56f - stride, h * 0.84f), stroke, StrokeCap.Round)
        drawCircle(primary, radius = h * 0.075f, center = Offset(w * 0.36f, h * 0.62f))
        drawCircle(primary, radius = h * 0.075f, center = Offset(w * 0.62f, h * 0.62f))
      }
    }
  }
}

@Composable
fun BarbellPlateVisualizer(
  result: PlateCalculationResult,
  modifier: Modifier = Modifier,
) {
  val barMetal = MaterialTheme.colorScheme.onSurfaceVariant
  val surfaceBg = MaterialTheme.colorScheme.surfaceVariant
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(130.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(surfaceBg),
  ) {
    val w = size.width
    val h = size.height
    val cy = h / 2f

    // Barbell shaft + sleeve collar
    drawRoundRect(
      color = barMetal.copy(alpha = 0.5f),
      topLeft = Offset(w * 0.06f, cy - 6.dp.toPx()),
      size = Size(w * 0.88f, 12.dp.toPx()),
      cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
    )
    drawRoundRect(
      color = barMetal,
      topLeft = Offset(w * 0.18f, cy - 16.dp.toPx()),
      size = Size(10.dp.toPx(), 32.dp.toPx()),
      cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
    )

    // Render loaded plates on sleeve
    var xCursor = w * 0.18f + 14.dp.toPx()
    result.perSidePlates.forEach { item ->
      val plateHeight = when {
        item.weight >= 20.0 -> 88.dp.toPx()
        item.weight >= 15.0 -> 76.dp.toPx()
        item.weight >= 10.0 -> 64.dp.toPx()
        item.weight >= 5.0 -> 50.dp.toPx()
        else -> 38.dp.toPx()
      }
      val plateWidth = if (item.weight >= 20.0) 14.dp.toPx() else 11.dp.toPx()
      repeat(item.countPerSide) {
        if (xCursor + plateWidth < w * 0.90f) {
          drawRoundRect(
            color = Color(item.colorHex),
            topLeft = Offset(xCursor, cy - plateHeight / 2f),
            size = Size(plateWidth, plateHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
          )
          xCursor += plateWidth + 3.dp.toPx()
        }
      }
    }
  }
}

data class ChartPoint(
  val label: String,
  val value: Double,
)

@Composable
fun LineTrendChart(
  points: List<ChartPoint>,
  lineColor: Color,
  unitSuffix: String,
  emptyText: String,
  modifier: Modifier = Modifier,
) {
  if (points.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(170.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = emptyText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  val minVal = points.minOf { it.value }
  val maxVal = points.maxOf { it.value }
  val span = (maxVal - minVal).takeIf { it > 0.001 } ?: 1.0
  val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .padding(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Max: ${(maxVal * 10).roundToInt() / 10.0} $unitSuffix",
        style = MonoNumericTextStyle,
        color = lineColor,
      )
      Text(
        text = "Last: ${(points.last().value * 10).roundToInt() / 10.0} $unitSuffix",
        style = MonoNumericTextStyle,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    Spacer(Modifier.height(10.dp))
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(128.dp),
    ) {
      val pad = 10.dp.toPx()
      val chartW = size.width - pad * 2
      val chartH = size.height - pad * 2

      // Horizontal reference grid lines
      listOf(0f, 0.5f, 1f).forEach { frac ->
        val y = pad + chartH * frac
        drawLine(gridColor, Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1.dp.toPx())
      }

      val coords = points.mapIndexed { idx, pt ->
        val x = if (points.size == 1) size.width / 2f else pad + chartW * (idx.toFloat() / (points.size - 1))
        val norm = ((pt.value - minVal) / span).toFloat().coerceIn(0f, 1f)
        val y = pad + chartH * (1f - norm)
        Offset(x, y)
      }

      if (coords.size > 1) {
        val fillPath = Path().apply {
          moveTo(coords.first().x, pad + chartH)
          coords.forEach { lineTo(it.x, it.y) }
          lineTo(coords.last().x, pad + chartH)
          close()
        }
        drawPath(
          path = fillPath,
          brush = Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
          ),
        )
        val strokePath = Path().apply {
          moveTo(coords.first().x, coords.first().y)
          coords.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
          path = strokePath,
          color = lineColor,
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
      }

      coords.forEach { pt ->
        drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = pt)
        drawCircle(color = Color(0xFF0D0F14), radius = 2.dp.toPx(), center = pt)
      }
    }
    Spacer(Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = points.first().label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = points.last().label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun MuscleHeatmapView(
  workingSetsByMuscle: Map<MuscleGroup, Double>,
  showBack: Boolean,
  language: AppLanguage,
  onSelectMuscle: (MuscleGroup) -> Unit,
  modifier: Modifier = Modifier,
) {
  val maxSets = (workingSetsByMuscle.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
  val baseBody = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
  val primary = MaterialTheme.colorScheme.primary
  val amber = MaterialTheme.colorScheme.secondary

  fun heatColor(muscle: MuscleGroup): Color {
    val sets = workingSetsByMuscle[muscle] ?: 0.0
    if (sets <= 0.0) return baseBody
    val intensity = (sets / maxSets).toFloat().coerceIn(0.2f, 1f)
    return if (intensity > 0.65f) primary.copy(alpha = 0.4f + 0.55f * intensity)
    else amber.copy(alpha = 0.35f + 0.55f * intensity)
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
      val cx = size.width / 2f
      val h = size.height

      // Head
      drawCircle(color = baseBody, radius = h * 0.065f, center = Offset(cx, h * 0.12f))

      if (!showBack) {
        // FRONT VIEW
        // Shoulders
        drawOval(heatColor(MuscleGroup.SHOULDERS), Offset(cx - 54.dp.toPx(), h * 0.20f), Size(28.dp.toPx(), 20.dp.toPx()))
        drawOval(heatColor(MuscleGroup.SHOULDERS), Offset(cx + 26.dp.toPx(), h * 0.20f), Size(28.dp.toPx(), 20.dp.toPx()))
        // Chest
        drawRoundRect(heatColor(MuscleGroup.CHEST), Offset(cx - 32.dp.toPx(), h * 0.22f), Size(30.dp.toPx(), 26.dp.toPx()), CornerRadius(8f, 8f))
        drawRoundRect(heatColor(MuscleGroup.CHEST), Offset(cx + 2.dp.toPx(), h * 0.22f), Size(30.dp.toPx(), 26.dp.toPx()), CornerRadius(8f, 8f))
        // Core
        drawRoundRect(heatColor(MuscleGroup.CORE), Offset(cx - 22.dp.toPx(), h * 0.36f), Size(44.dp.toPx(), 36.dp.toPx()), CornerRadius(10f, 10f))
        // Biceps
        drawRoundRect(heatColor(MuscleGroup.BICEPS), Offset(cx - 52.dp.toPx(), h * 0.30f), Size(16.dp.toPx(), 26.dp.toPx()), CornerRadius(8f, 8f))
        drawRoundRect(heatColor(MuscleGroup.BICEPS), Offset(cx + 36.dp.toPx(), h * 0.30f), Size(16.dp.toPx(), 26.dp.toPx()), CornerRadius(8f, 8f))
        // Forearms
        drawRoundRect(heatColor(MuscleGroup.FOREARMS), Offset(cx - 56.dp.toPx(), h * 0.44f), Size(14.dp.toPx(), 24.dp.toPx()), CornerRadius(6f, 6f))
        drawRoundRect(heatColor(MuscleGroup.FOREARMS), Offset(cx + 42.dp.toPx(), h * 0.44f), Size(14.dp.toPx(), 24.dp.toPx()), CornerRadius(6f, 6f))
        // Quads
        drawRoundRect(heatColor(MuscleGroup.QUADS), Offset(cx - 28.dp.toPx(), h * 0.56f), Size(24.dp.toPx(), 46.dp.toPx()), CornerRadius(10f, 10f))
        drawRoundRect(heatColor(MuscleGroup.QUADS), Offset(cx + 4.dp.toPx(), h * 0.56f), Size(24.dp.toPx(), 46.dp.toPx()), CornerRadius(10f, 10f))
        // Calves front outline
        drawRoundRect(heatColor(MuscleGroup.CALVES), Offset(cx - 24.dp.toPx(), h * 0.79f), Size(18.dp.toPx(), 28.dp.toPx()), CornerRadius(8f, 8f))
        drawRoundRect(heatColor(MuscleGroup.CALVES), Offset(cx + 6.dp.toPx(), h * 0.79f), Size(18.dp.toPx(), 28.dp.toPx()), CornerRadius(8f, 8f))
      } else {
        // BACK VIEW
        // Upper Back / Lats
        drawRoundRect(heatColor(MuscleGroup.BACK), Offset(cx - 34.dp.toPx(), h * 0.22f), Size(68.dp.toPx(), 48.dp.toPx()), CornerRadius(14f, 14f))
        // Rear Shoulders
        drawOval(heatColor(MuscleGroup.SHOULDERS), Offset(cx - 54.dp.toPx(), h * 0.20f), Size(24.dp.toPx(), 18.dp.toPx()))
        drawOval(heatColor(MuscleGroup.SHOULDERS), Offset(cx + 30.dp.toPx(), h * 0.20f), Size(24.dp.toPx(), 18.dp.toPx()))
        // Triceps
        drawRoundRect(heatColor(MuscleGroup.TRICEPS), Offset(cx - 52.dp.toPx(), h * 0.30f), Size(16.dp.toPx(), 26.dp.toPx()), CornerRadius(8f, 8f))
        drawRoundRect(heatColor(MuscleGroup.TRICEPS), Offset(cx + 36.dp.toPx(), h * 0.30f), Size(16.dp.toPx(), 26.dp.toPx()), CornerRadius(8f, 8f))
        // Glutes
        drawRoundRect(heatColor(MuscleGroup.GLUTES), Offset(cx - 28.dp.toPx(), h * 0.47f), Size(26.dp.toPx(), 24.dp.toPx()), CornerRadius(12f, 12f))
        drawRoundRect(heatColor(MuscleGroup.GLUTES), Offset(cx + 2.dp.toPx(), h * 0.47f), Size(26.dp.toPx(), 24.dp.toPx()), CornerRadius(12f, 12f))
        // Hamstrings
        drawRoundRect(heatColor(MuscleGroup.HAMSTRINGS), Offset(cx - 26.dp.toPx(), h * 0.60f), Size(22.dp.toPx(), 38.dp.toPx()), CornerRadius(10f, 10f))
        drawRoundRect(heatColor(MuscleGroup.HAMSTRINGS), Offset(cx + 4.dp.toPx(), h * 0.60f), Size(22.dp.toPx(), 38.dp.toPx()), CornerRadius(10f, 10f))
        // Calves
        drawRoundRect(heatColor(MuscleGroup.CALVES), Offset(cx - 24.dp.toPx(), h * 0.79f), Size(18.dp.toPx(), 28.dp.toPx()), CornerRadius(8f, 8f))
        drawRoundRect(heatColor(MuscleGroup.CALVES), Offset(cx + 6.dp.toPx(), h * 0.79f), Size(18.dp.toPx(), 28.dp.toPx()), CornerRadius(8f, 8f))
      }
    }

    Spacer(Modifier.height(10.dp))
    val relevantMuscles = MuscleGroup.entries.filter { m ->
      m != MuscleGroup.FULL_BODY && m != MuscleGroup.CARDIO && (if (showBack) m.isBack else m.isFront)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      relevantMuscles.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          pair.forEach { m ->
            val sets = workingSetsByMuscle[m] ?: 0.0
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectMuscle(m) },
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(RoundedCornerShape(3.dp))
                      .background(heatColor(m)),
                  )
                  Text(
                    text = m.label(language),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                  )
                }
                Text(
                  text = "${(sets * 10).roundToInt() / 10.0}",
                  style = MonoNumericTextStyle,
                  color = if (sets > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
      }
    }
  }
}
