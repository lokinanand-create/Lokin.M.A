package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = Color.White,
    primaryContainer = DeepRoyalPurple,
    onPrimaryContainer = NeonAmethyst,
    secondary = CyberCyan,
    onSecondary = ObsidianBlack,
    secondaryContainer = Color(0xFF12324B),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = AuroraMagenta,
    onTertiary = Color.White,
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = DeepVoid,
    onSurface = TextPrimaryDark,
    surfaceVariant = CosmicSurface,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = DeepVoid,
    surfaceContainer = CosmicSurface,
    surfaceContainerHigh = CosmicSurfaceElevated,
    outline = Color(0xFF3E2D63),
    outlineVariant = Color(0xFF271C42),
    error = CrimsonError,
    onError = Color.White
)

private val StudioLightColorScheme = lightColorScheme(
    primary = LightPrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = LightSecondaryIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = LightTertiaryMagenta,
    onTertiary = Color.White,
    background = LightCanvas,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = LightCanvas,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceVariant,
    outline = Color(0xFFC4B5FD),
    outlineVariant = Color(0xFFE5DEFA),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MaAiStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = StudioShapes,
        content = content
    )
}
