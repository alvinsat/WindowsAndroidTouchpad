package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Authentic Sony PlayStation 1 (PS1) Industrial Light Gray Palette
val PS1Background = Color(0xFFD4D5DC)        // Classic PS1 chassis matte light gray
val PS1Surface = Color(0xFFE2E3E8)           // Elevated console top lid & button surface
val PS1SurfaceElevated = Color(0xFFC7C8D1)   // Recessed drive tray / controller bay gray
val PS1SurfaceBorder = Color(0xFFA4A6B0)     // Molded seam grooves & casing perimeter
val PS1SurfaceHighlight = Color(0xFFEFF0F5)  // Subtle bevel edge highlight

// Signature PS1 Controller Symbol & Sony Accents
val PS1Blue = Color(0xFF3858D6)              // Iconic PS1 Cross (X) / Sony Blue
val PS1Green = Color(0xFF00966C)             // PS1 Triangle Green
val PS1Red = Color(0xFFD32F2F)               // PS1 Circle Red
val PS1Pink = Color(0xFFD83A8C)              // PS1 Square Pink

// Status Indicators calibrated for high contrast on light gray
val StatusGreen = Color(0xFF00875A)          // Active LED Green
val StatusAmber = Color(0xFFD97706)          // Standby / Discovery Amber
val StatusRed = Color(0xFFD32F2F)            // Error / Warning Red

// PS1 Silk-Screened Industrial Typography
val TextPrimary = Color(0xFF1E2026)          // Deep graphite black (high contrast, ultra legible)
val TextSecondary = Color(0xFF4E515E)        // Muted graphite for secondary labels
val TextMuted = Color(0xFF787B8A)            // Muted hint / placeholder text

// Backward compatibility mappings for existing component references
val DarkBackground = PS1Background
val DarkSurface = PS1Surface
val DarkSurfaceElevated = PS1SurfaceElevated
val DarkSurfaceBorder = PS1SurfaceBorder

val CyanPrimary = PS1Blue
val CyanSecondary = PS1Green
val CyanTertiary = PS1Pink

val Purple80 = PS1Blue
val PurpleGrey80 = PS1Green
val Pink80 = PS1Pink

val Purple40 = Color(0xFF263EA3)
val PurpleGrey40 = Color(0xFF006B4D)
val Pink40 = Color(0xFFA8266C)


