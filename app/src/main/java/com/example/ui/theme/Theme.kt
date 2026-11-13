package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PahadDarkColorScheme =
  darkColorScheme(
    primary = PineLightPrimary,
    onPrimary = PineOnLightPrimary,
    primaryContainer = PineDarkContainer,
    onPrimaryContainer = PineMist,
    secondary = SageLightSecondary,
    onSecondary = PineDeep,
    secondaryContainer = PineNightElevated,
    onSecondaryContainer = CreamInk,
    tertiary = SaffronLightTertiary,
    onTertiary = SaffronDarkOnContainer,
    tertiaryContainer = Color(0xFF5D2A13),
    onTertiaryContainer = SaffronContainer,
    background = PineNight,
    onBackground = CreamInk,
    surface = PineNightSurface,
    onSurface = CreamInk,
    surfaceVariant = PineNightElevated,
    onSurfaceVariant = MistMuted,
    outline = Color(0xFF46554B),
    outlineVariant = Color(0xFF2B3A31),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
  )

private val PahadLightColorScheme =
  lightColorScheme(
    primary = PineForest,
    onPrimary = Color.White,
    primaryContainer = PineMist,
    onPrimaryContainer = PineDarkOnContainer,
    secondary = SageMoss,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = SageDarkOnContainer,
    tertiary = HimalayanSaffron,
    onTertiary = Color.White,
    tertiaryContainer = SaffronContainer,
    onTertiaryContainer = SaffronDarkOnContainer,
    background = ParchmentCream,
    onBackground = ForestInk,
    surface = ParchmentCream,
    onSurface = ForestInk,
    surfaceVariant = LinenSurface,
    onSurfaceVariant = MossMuted,
    surfaceContainerLow = LinenSurface,
    surfaceContainer = LinenElevated,
    outline = StoneOutline,
    outlineVariant = StoneOutlineVariant,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorSoft,
  )

val PahadShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun PahadOrganicTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) PahadDarkColorScheme else PahadLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = PahadTypography,
    shapes = PahadShapes,
    content = content,
  )
}
