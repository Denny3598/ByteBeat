package com.example.applemusicui.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppleRed = Color(0xFFFA2D48)
val SurfaceDark = Color(0xFF1C1C1E)
val TextSecondary = Color(0xFF8E8E93)
val GlassDark = Color(0xE6161618)

private val Scheme = darkColorScheme(
    primary = AppleRed,
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun AppleMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
