package com.example.applemusicui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.AppTab
import com.example.applemusicui.ui.components.LocalOnAvatarClick
import com.example.applemusicui.ui.components.MiniPlayer
import com.example.applemusicui.ui.components.PillTabBar
import com.example.applemusicui.ui.components.ThemeDialog
import com.example.applemusicui.ui.screens.ExploreScreen
import com.example.applemusicui.ui.screens.LibraryScreen
import com.example.applemusicui.ui.screens.ListenNowScreen
import com.example.applemusicui.ui.screens.SearchScreen
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.ThemeMode

@Composable
fun AppRoot(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var current by remember { mutableStateOf(DummyData.songs.first()) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val c = AppTheme.colors
    val play: (Song) -> Unit = { song ->
        current = song
        isPlaying = true
    }

    CompositionLocalProvider(LocalOnAvatarClick provides { showThemeDialog = true }) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.background)
        ) {
            when (tab) {
                AppTab.Home -> ListenNowScreen(current, play)
                AppTab.Explore -> ExploreScreen()
                AppTab.Library -> LibraryScreen()
                AppTab.Search -> SearchScreen(current, play)
            }

            // Mini player + tab bar melayang di atas konten.
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                MiniPlayer(
                    song = current,
                    isPlaying = isPlaying,
                    onToggle = { isPlaying = !isPlaying },
                    onNext = {
                        val list = DummyData.songs
                        val index = list.indexOfFirst { it.title == current.title }
                        current = list[(index + 1) % list.size]
                        isPlaying = true
                    },
                )
                Spacer(Modifier.height(10.dp))
                PillTabBar(selected = tab, onSelect = { tab = it })
            }
        }

        if (showThemeDialog) {
            ThemeDialog(
                current = themeMode,
                onSelect = onThemeModeChange,
                onDismiss = { showThemeDialog = false },
            )
        }
    }
}
