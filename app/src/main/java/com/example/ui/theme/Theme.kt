package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.ironlog.core.model.ThemeMode

private val DarkAthleticScheme = darkColorScheme(
  primary = VoltLime,
  onPrimary = OnVoltLime,
  primaryContainer = VoltLimeContainer,
  onPrimaryContainer = VoltLime,
  secondary = SignalAmber,
  onSecondary = OnSignalAmber,
  secondaryContainer = SignalAmberContainer,
  onSecondaryContainer = SignalAmber,
  tertiary = TelemetryCyan,
  onTertiary = OnTelemetryCyan,
  tertiaryContainer = TelemetryCyanContainer,
  onTertiaryContainer = TelemetryCyan,
  background = ObsidianBg,
  onBackground = TextPrimaryDark,
  surface = SlateSurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = CardContainer,
  onSurfaceVariant = TextSecondaryDark,
  surfaceContainer = CardContainer,
  surfaceContainerHigh = ElevatedContainer,
  outline = OutlineSubtle,
  error = FailureCoral,
)

private val LightAthleticScheme = lightColorScheme(
  primary = ForestVolt,
  onPrimary = OnForestVolt,
  primaryContainer = ForestVoltContainer,
  onPrimaryContainer = ForestVolt,
  secondary = AmberDark,
  onSecondary = PureSurface,
  tertiary = CyanDark,
  onTertiary = PureSurface,
  background = ChalkBg,
  onBackground = TextPrimaryLight,
  surface = PureSurface,
  onSurface = TextPrimaryLight,
  surfaceVariant = SoftContainer,
  onSurfaceVariant = TextSecondaryLight,
  surfaceContainer = PureSurface,
  surfaceContainerHigh = SoftContainer,
  outline = OutlineLight,
  error = FailureCoral,
)

val IronLogShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
)

@Composable
fun IronLogTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val useDark = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }
  val scheme = if (useDark) DarkAthleticScheme else LightAthleticScheme

  MaterialTheme(
    colorScheme = scheme,
    typography = IronLogTypography,
    shapes = IronLogShapes,
    content = content,
  )
}
