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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.ui.components.AppTab
import com.example.applemusicui.ui.components.MiniPlayer
import com.example.applemusicui.ui.components.PillTabBar
import com.example.applemusicui.ui.screens.ExploreScreen
import com.example.applemusicui.ui.screens.LibraryScreen
import com.example.applemusicui.ui.screens.ListenNowScreen
import com.example.applemusicui.ui.screens.SearchScreen

@Composable
fun AppRoot() {
    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }

    val songs = DummyData.songs
    val current = songs[currentIndex]
    val play: (com.example.applemusicui.data.Song) -> Unit = { song ->
        currentIndex = songs.indexOf(song).coerceAtLeast(0)
        isPlaying = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                    currentIndex = (currentIndex + 1) % songs.size
                    isPlaying = true
                },
            )
            Spacer(Modifier.height(10.dp))
            PillTabBar(selected = tab, onSelect = { tab = it })
        }
    }
}
