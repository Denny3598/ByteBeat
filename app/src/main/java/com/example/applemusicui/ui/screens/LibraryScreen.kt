package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 200.dp),
        ) {
            message?.let { msg ->
                item {
                    Text(msg, color = AppleRed, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            }

            item {
                LibraryRowItem(
                    title = "Unduhan",
                    subtitle = "Belum ada unduhan",
                    icon = Icons.Filled.Download,
                )
            }
            item {
                LibraryRowItem(
                    title = "Musik lokal",
                    subtitle = "Dari perangkat ini",
                    icon = Icons.Filled.MusicNote,
                )
            }

            if (!loggedIn) {
                item {
                    Text(
                        "Masuk ke YouTube Music lewat Pengaturan untuk melihat pustaka akunmu.",
                        color = c.textSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            } else {
                item {
                    LibraryRowItem(
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
                    Text(
                        "Playlist",
                        color = c.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    )
                }

                val list = playlists
                when {
                    list == null -> item {
                        Text("Memuat playlist…", color = c.textSecondary, fontSize = 15.sp)
                    }

                    list.isEmpty() -> item {
                        Text("Belum ada playlist di akunmu.", color = c.textSecondary, fontSize = 15.sp)
                    }

                    else -> items(list, key = { it.browseId }) { item ->
                        LibraryRowItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            imageUrl = item.thumbnail,
                            busy = busyId == item.browseId,
                            onClick = { open(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRowItem(
    title: String,
    subtitle: String,
    imageUrl: String? = null,
    icon: ImageVector? = null,
    busy: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.control.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = c.text,
                    modifier = Modifier.size(26.dp),
                )
            }
        } else {
            Artwork(
                listOf(c.textSecondary, c.surface),
                Modifier.size(56.dp),
                corner = 8.dp,
                imageUrl = imageUrl,
            )
        }
        Spacer(Modifier.width(14.dp))
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
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
