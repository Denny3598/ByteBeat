package com.example.applemusicui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.applemusicui.ui.AppRoot
import com.example.applemusicui.ui.theme.AppleMusicTheme
import com.example.applemusicui.ui.theme.ThemeMode
import com.example.applemusicui.ui.theme.ThemePrefs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var mode by remember { mutableStateOf(ThemePrefs.load(this@MainActivity)) }
            val dark = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            // Warna ikon status bar & navigation bar mengikuti tema.
            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (dark) {
                    SystemBarStyle.dark(transparent)
                } else {
                    SystemBarStyle.light(transparent, transparent)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            AppleMusicTheme(darkTheme = dark) {
                AppRoot(
                    themeMode = mode,
                    onThemeModeChange = {
                        mode = it
                        ThemePrefs.save(this@MainActivity, it)
                    },
                )
            }
        }
    }
}
