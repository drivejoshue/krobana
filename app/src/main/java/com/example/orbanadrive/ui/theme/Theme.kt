package com.example.orbanadrive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ====== Esquemas de color ======
private val DarkColors = darkColorScheme(
    primary         = Blue600,
    onPrimary       = White,
    secondary       = Teal400,
    onSecondary     = Color.Black,
    tertiary        = CyanA400,          // ojo: con C mayúscula
    background      = Gray900,
    onBackground    = Color(0xFFE9EDF3),
    surface         = Gray800,
    onSurface       = Color(0xFFE9EDF3),
    surfaceVariant  = Gray700,
    error           = Color(0xFFFF6B6B)
)

private val LightColors = lightColorScheme(
    primary         = Blue600,
    onPrimary       = White,
    secondary       = Teal400,
    onSecondary     = White,
    tertiary        = CyanA400,
    background      = Color(0xFFF7FAFF),
    onBackground    = Color(0xFF101623),
    surface         = Color(0xFFFFFFFF),
    onSurface       = Color(0xFF0F1115),
    surfaceVariant  = Color(0xFFE6EAF2),
    error           = Color(0xFFB3261E)
)

// ====== Theme wrapper ======
@Composable
fun OrbanaDriveTheme(
    darkTheme: Boolean = true, // por defecto dark; luego lo atamos a Settings
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
