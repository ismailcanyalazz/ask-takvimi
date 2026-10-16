package com.asktakvimi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.asktakvimi.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = RoseDeep,
    onPrimary = SurfaceLight,
    primaryContainer = RoseSoft,
    onPrimaryContainer = InkDark,
    secondary = Lilac,
    onSecondary = SurfaceLight,
    secondaryContainer = LilacSoft,
    onSecondaryContainer = InkDark,
    tertiary = Rose,
    onTertiary = SurfaceLight,
    tertiaryContainer = CreamDeep,
    onTertiaryContainer = InkDark,
    background = BackgroundLight,
    onBackground = InkDark,
    surface = SurfaceLight,
    onSurface = InkDark,
    surfaceVariant = Cream,
    onSurfaceVariant = InkSoft,
    outline = OutlineLight,
    outlineVariant = OutlineLight
)

private val DarkColors = darkColorScheme(
    primary = RoseNight,
    onPrimary = InkOnBright,
    primaryContainer = RoseNightContainer,
    onPrimaryContainer = InkLight,
    secondary = LilacNight,
    onSecondary = InkOnBright,
    secondaryContainer = LilacNightContainer,
    onSecondaryContainer = InkLight,
    tertiary = RoseNight,
    onTertiary = InkOnBright,
    tertiaryContainer = RoseNightContainer,
    onTertiaryContainer = InkLight,
    background = BackgroundDark,
    onBackground = InkLight,
    surface = SurfaceDark,
    onSurface = InkLight,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = InkLightSoft,
    outline = OutlineDark,
    outlineVariant = OutlineDark
)

@Composable
fun AskTakvimiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
