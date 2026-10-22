package com.example.applemusicui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.applemusicui.ui.AppRoot
import com.example.applemusicui.ui.theme.AppThemeMode
import com.example.applemusicui.ui.theme.AppleMusicUITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }

            AppleMusicUITheme(themeMode = themeMode) {
                AppRoot(
                    currentThemeMode = themeMode,
                    onThemeModeChange = { newMode -> themeMode = newMode }
                )
            }
        }
    }
}
