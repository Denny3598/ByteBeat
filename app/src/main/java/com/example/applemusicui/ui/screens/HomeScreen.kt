package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SongItem(val id: String, val title: String, val artist: String)
data class AlbumItem(val id: String, val title: String, val typeAndArtist: String)

@Composable
fun HomeScreen() {
    val recentSongs = listOf(
        SongItem("1", "It's You", "Sezairi"),
        SongItem("2", "Kiss the Sky (EP Version)", "Cash Cash"),
        SongItem("3", "Shower", "Becky G"),
        SongItem("4", "Thunderclouds (feat. Sia, Diplo & ...", "LSD")
    )

    val albums = listOf(
        AlbumItem("1", "Siapa Di Hatimu (f...", "Single • Maulana Ar..."),
        AlbumItem("2", "Suka Sama Kamu ...", "Single • Maulana Ar..."),
        AlbumItem("3", "Disco ...", "Album")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Listen Now", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
        }

        item {
            Column {
                SectionHeader("Recents")
                Spacer(modifier = Modifier.height(12.dp))
                recentSongs.forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                            Text(song.artist, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }
        }

        item {
            Column {
                SectionHeader("Album & single")
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(albums) { album ->
                        Column(modifier = Modifier.width(140.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(album.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Text(album.typeAndArtist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Pilihan cepat")
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Lihat Semua", fontSize = 14.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Medium)
    }
}
