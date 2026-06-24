package com.example.espressifmicroclimate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val DarkColorScheme = darkColorScheme(
    primary = GrayPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = GraySurfaceContainerHighDark,
    onPrimaryContainer = GrayPrimaryDark,
    secondary = GraySecondaryDark,
    onSecondary = Color.White,
    tertiary = GrayTertiaryDark,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF3D4F3F),
    onTertiaryContainer = Color(0xFFB8F5B8),
    surface = GraySurfaceDark,
    surfaceContainer = GraySurfaceContainerDark,
    surfaceContainerHigh = GraySurfaceContainerHighDark,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F0D4),
    onPrimaryContainer = Color(0xFF0E4F1A),
    secondary = GreenSecondaryLight,
    onSecondary = Color.White,
    tertiary = GreenTertiaryLight,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFB8E6C0),
    onTertiaryContainer = Color(0xFF0E4F1A),
    surface = GreenSurfaceLight,
    surfaceContainer = GreenSurfaceContainerLight,
    surfaceContainerHigh = GreenSurfaceContainerHighLight,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF8C1D18)
)

// Корневая тема приложения
// themeMode определяет какую цветовую схему использовать
@Composable
fun EspressifMicroclimateTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
