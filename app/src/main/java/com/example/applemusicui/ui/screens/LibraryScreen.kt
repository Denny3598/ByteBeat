package com.example.applemusicui.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.AppPrefs
import com.example.applemusicui.data.LibraryItem
import com.example.applemusicui.data.LibraryRepository
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.components.SectionHeader
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed
import kotlinx.coroutines.launch

private const val LIKED_ID = "FEmusic_liked_videos"

@Composable
fun LibraryScreen(onPlay: (Song, List<Song>) -> Unit = { _, _ -> }) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    val loggedIn = AppPrefs.isLoggedIn()

    var playlists by remember { mutableStateOf<List<LibraryItem>?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loggedIn) {
        playlists = if (loggedIn) {
            runCatching { LibraryRepository.playlists() }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }

    fun open(item: LibraryItem) {
        if (busyId != null) return
        busyId = item.browseId
        message = null
        scope.launch {
            val songs = runCatching { LibraryRepository.tracks(item.browseId) }
                .getOrDefault(emptyList())
            busyId = null
            val first = songs.firstOrNull()
            if (first == null) {
                message = "\"${item.title}\" tidak bisa dibuka sekarang."
            } else {
                onPlay(first, songs)
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Pustaka")

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 200.dp),
        ) {
            message?.let { msg ->
                item {
                    Text(
                        msg,
                        color = AppleRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            item {
                LinkRow(
                    title = "Unduhan",
                    subtitle = "Musik yang disimpan untuk offline",
                    icon = Icons.Filled.Download,
                )
            }
            item {
                LinkRow(
                    title = "Musik lokal",
                    subtitle = "Dari perangkat ini",
                    icon = Icons.Filled.MusicNote,
                )
            }

            if (loggedIn) {
                item {
                    LinkRow(
                        title = "Lagu yang disukai",
                        subtitle = "Dari akun YouTube Music",
                        icon = Icons.Filled.Favorite,
                        busy = busyId == LIKED_ID,
                        onClick = {
                            open(LibraryItem("Lagu yang disukai", "", null, LIKED_ID))
                        },
                    )
                }

                item {
                    SectionHeader(title = "Playlist", action = null)
                }

                val list = playlists
                when {
                    list == null -> item {
                        Text(
                            "Memuat playlist…",
                            color = c.textSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }

                    list.isEmpty() -> item {
                        Text(
                            "Belum ada playlist di akunmu.",
                            color = c.textSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }

                    else -> item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(list, key = { it.browseId }) { playlist ->
                                PlaylistCard(
                                    item = playlist,
                                    busy = busyId == playlist.browseId,
                                    onClick = { open(playlist) },
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Masuk ke YouTube Music lewat Pengaturan untuk melihat pustaka akunmu.",
                        color = c.textSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

/** Baris folder seperti di BitChord: ikon, judul, panah kanan, dan garis pemisah. */
@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    busy: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = AppTheme.colors
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 60.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = c.control,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = c.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else if (onClick != null) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = c.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 60.dp),
            thickness = 0.5.dp,
            color = c.divider,
        )
    }
}

@Composable
private fun PlaylistCard(item: LibraryItem, busy: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column(
        Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Artwork(
                listOf(AppleRed, c.surface),
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                corner = 12.dp,
                imageUrl = item.thumbnail,
            )
            if (busy) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .size(28.dp),
                    color = Color.White,
                    strokeWidth = 3.dp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            color = c.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                item.subtitle,
                color = c.textSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
