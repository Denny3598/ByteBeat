package com.example.applemusicui.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.components.SongRow
import com.example.applemusicui.ui.theme.SurfaceDark
import com.example.applemusicui.ui.theme.TextSecondary

@Composable
fun SearchScreen(current: Song, onPlay: (Song) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = DummyData.songs.filter {
        it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 200.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader("Search", horizontalPadding = 0.dp)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Artis, lagu, album", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )
        }

        if (query.isBlank()) {
            // Belum mengetik: tampilkan kategori.
            items(DummyData.moods) { MoodCard(it) }
        } else {
            // Hasil pencarian (dummy, difilter dari daftar lagu lokal).
            items(results, span = { GridItemSpan(maxLineSpan) }) { song ->
                SongRow(song, highlighted = song.title == current.title) { onPlay(song) }
            }
        }
    }
}
