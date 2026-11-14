package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = HealthPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = HealthBlueContainerLight,
    onPrimaryContainer = Color(0xFF0B3A78),
    secondary = PastelPalette.MintAccent,
    onSecondary = Color.White,
    secondaryContainer = PastelPalette.MintCardLight,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = PastelPalette.PurpleAccent,
    onTertiary = Color.White,
    tertiaryContainer = PastelPalette.PurpleCardLight,
    onTertiaryContainer = Color(0xFF4C1D95),
    background = HealthBackgroundLight,
    onBackground = HealthTextPrimaryLight,
    surface = HealthSurfaceLight,
    onSurface = HealthTextPrimaryLight,
    surfaceVariant = HealthSurfaceVariantLight,
    onSurfaceVariant = HealthTextSecondaryLight,
    outline = HealthOutlineLight,
    outlineVariant = Color(0xFFE7EDF5),
    error = PastelPalette.RoseAccent,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = HealthPrimaryBlueDark,
    onPrimary = Color(0xFF0A254D),
    primaryContainer = HealthBlueContainerDark,
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = PastelPalette.MintAccentDark,
    onSecondary = Color(0xFF042F21),
    secondaryContainer = PastelPalette.MintCardDark,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = PastelPalette.PurpleAccentDark,
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = PastelPalette.PurpleCardDark,
    onTertiaryContainer = Color(0xFFDDD6FE),
    background = HealthBackgroundDark,
    onBackground = HealthTextPrimaryDark,
    surface = HealthSurfaceDark,
    onSurface = HealthTextPrimaryDark,
    surfaceVariant = HealthSurfaceVariantDark,
    onSurfaceVariant = HealthTextSecondaryDark,
    outline = HealthOutlineDark,
    outlineVariant = Color(0xFF232E3D),
    error = PastelPalette.RoseAccentDark,
    onError = Color(0xFF4C0519)
)

val HealthShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MyHealthTheme(
    darkThemeMode: String = "System",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (darkThemeMode) {
        "Dark" -> true
        "Light" -> false
        else -> systemDark
    }
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides useDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = HealthShapes,
            content = content
        )
    }
}
