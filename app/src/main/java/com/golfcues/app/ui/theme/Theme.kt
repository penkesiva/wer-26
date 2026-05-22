package com.golfcues.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GolfGreen,
    onPrimary = Color.White,
    primaryContainer = GolfGreenLight,
    secondary = AccentOrange,
    background = SurfaceLight,
    surface = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val DarkColors = darkColorScheme(
    primary = GolfGreenLight,
    onPrimary = Color.Black,
    primaryContainer = GolfGreen,
    secondary = AccentOrange,
    background = GolfGreenDark,
    surface = Color(0xFF1E2A1F),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun GolfCuesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
