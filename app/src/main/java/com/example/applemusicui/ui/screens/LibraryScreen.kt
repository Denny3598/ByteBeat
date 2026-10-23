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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.ui.components.AlbumCard
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.components.SectionHeader
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed

@Composable
fun LibraryScreen() {
    val c = AppTheme.colors
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 200.dp),
    ) {
        item { ScreenHeader("Perpustakaan") }

        // Kartu statistik
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    StatCard(
                        value = "511",
                        label = "Lagu diputar",
                        footer = "Anggota sejak 09/26",
                        width = screenWidth * 0.8f,
                    )
                }
                item {
                    StatCard(
                        value = "Top Song",
                        label = "Lagu paling sering",
                        footer = "Anggota sejak 09/26",
                        width = screenWidth * 0.8f,
                    )
                }
            }
        }

        // Unduhan & musik lokal
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                LibraryRow(Icons.Filled.Download, "Unduhan")
                HorizontalDivider(color = c.divider)
                LibraryRow(Icons.Filled.MusicNote, "Musik lokal")
            }
        }

        item { SectionHeader("Playlist", action = "Lihat semua", actionColor = AppleRed) }
        items(playlistTiles) { tile ->
            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                PlaylistTile(tile)
            }
        }

        item { SectionHeader("Album") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(DummyData.albums) { album -> AlbumCard(album, size = 130.dp) }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, footer: String, width: androidx.compose.ui.unit.Dp) {
    val c = AppTheme.colors
    Column(
        Modifier
            .width(width)
            .height(170.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(value, color = c.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(label, color = c.textSecondary, fontSize = 15.sp)
        }
        Text(footer, color = c.textSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun LibraryRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AppleRed, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            color = c.text,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textSecondary)
    }
}

private data class PlaylistTileData(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val colors: List<Color>,
)

private val playlistTiles = listOf(
    PlaylistTileData("Playlist baru", "Buat playlist", Icons.Filled.Add, listOf(Color(0xFF3A3A3C), Color(0xFF636366))),
    PlaylistTileData("Impor dari Spotify", "Pindahkan playlist", Icons.Filled.MusicNote, listOf(Color(0xFF1DB954), Color(0xFF0B5D2A))),
    PlaylistTileData("Musik yang disukai", "Playlist otomatis", Icons.Filled.Favorite, listOf(Color(0xFFFA2D48), Color(0xFFFF6A88))),
)

@Composable
private fun PlaylistTile(tile: PlaylistTileData) {
    val c = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(tile.colors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tile.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tile.title,
                color = c.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(tile.subtitle, color = c.textSecondary, fontSize = 14.sp)
        }
    }
}
