package com.example.applemusicui.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.AlbumCard
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.components.SectionHeader
import com.example.applemusicui.ui.components.SongRow

@Composable
fun ListenNowScreen(current: Song, onPlay: (Song, List<Song>) -> Unit) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 200.dp),
    ) {
        item { ScreenHeader("Listen Now") }

        item { SectionHeader("Recents") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(DummyData.recentPages) { page ->
                    Column(Modifier.width(screenWidth * 0.88f)) {
                        page.forEach { song ->
                            SongRow(song, highlighted = song.title == current.title) {
                                onPlay(song, DummyData.songs)
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Album & single") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(DummyData.albums) { AlbumCard(it) }
            }
        }

        item { SectionHeader("Pilihan cepat") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(DummyData.albums.reversed()) { AlbumCard(it) }
            }
        }
    }
}
