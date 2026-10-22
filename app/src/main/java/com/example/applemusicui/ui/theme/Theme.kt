package com.example.applemusicui.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val AppleRed = Color(0xFFFA2D48)

/** Pilihan tema yang bisa dipilih pengguna. */
enum class ThemeMode(val label: String) {
    System("Ikuti sistem"),
    Light("Terang"),
    Dark("Gelap"),
}

/** Palet warna aplikasi; berbeda untuk tema terang dan gelap. */
@Immutable
data class AppColors(
    val background: Color,
    val text: Color,
    val textSecondary: Color,
    val surface: Color,
    val divider: Color,
    val glass: Color,
    val glassBorder: Color,
    val selectedPill: Color,
    val control: Color,
    val segmentSelected: Color,
    val segmentSelectedText: Color,
)

private val DarkColors = AppColors(
    background = Color.Black,
    text = Color.White,
    textSecondary = Color(0xFF8E8E93),
    surface = Color(0xFF1C1C1E),
    divider = Color.White.copy(alpha = 0.12f),
    glass = Color(0xE6161618),
    glassBorder = Color.White.copy(alpha = 0.08f),
    selectedPill = Color.White.copy(alpha = 0.12f),
    control = AppleRed,
    segmentSelected = Color(0xFF3A3A3C),
    segmentSelectedText = Color.White,
)

private val LightColors = AppColors(
    background = Color.White,
    text = Color(0xFF0B0B0B),
    textSecondary = Color(0xFF6E6E73),
    surface = Color(0xFFF2F2F4),
    divider = Color.Black.copy(alpha = 0.10f),
    glass = Color(0xEBF2F2F4),
    glassBorder = Color.Black.copy(alpha = 0.06f),
    selectedPill = Color.Black.copy(alpha = 0.09f),
    control = Color(0xFF0B0B0B),
    segmentSelected = Color(0xFF0B0B0B),
    segmentSelectedText = Color.White,
)

val LocalAppColors = staticCompositionLocalOf { DarkColors }

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

@Composable
fun AppleMusicTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = AppleRed,
            background = colors.background,
            surface = colors.background,
            onBackground = colors.text,
            onSurface = colors.text,
        )
    } else {
        lightColorScheme(
            primary = AppleRed,
            background = colors.background,
            surface = colors.background,
            onBackground = colors.text,
            onSurface = colors.text,
        )
    }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Menyimpan pilihan tema di penyimpanan lokal HP. */
object ThemePrefs {
    private const val FILE = "settings"
    private const val KEY = "theme_mode"

    fun load(context: Context): ThemeMode {
        val saved = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.System
    }

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, mode.name)
            .apply()
    }
}
