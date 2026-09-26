package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PS1ColorScheme =
  lightColorScheme(
    primary = PS1Blue,
    onPrimary = Color.White,
    primaryContainer = PS1Surface,
    onPrimaryContainer = PS1Blue,
    secondary = PS1Green,
    onSecondary = Color.White,
    tertiary = PS1Pink,
    onTertiary = Color.White,
    background = PS1Background,
    onBackground = TextPrimary,
    surface = PS1Surface,
    onSurface = TextPrimary,
    surfaceVariant = PS1SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = PS1SurfaceBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = PS1ColorScheme, typography = Typography, content = content)
}

