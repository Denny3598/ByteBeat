package com.example.applemusicui.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.InnerTubeClient
import com.example.applemusicui.data.Shelf
import com.example.applemusicui.data.ShelfItem
import com.example.applemusicui.data.Song
import com.example.applemusicui.data.resolve
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.components.SectionHeader
import com.example.applemusicui.ui.components.ShelfCard
import com.example.applemusicui.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(onPlay: (Song, List<Song>) -> Unit = { _, _ -> }) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    val loggedIn = remember { InnerTubeClient.isLoggedIn() }
    var shelves by remember { mutableStateOf<List<Shelf>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loggedIn) {
        if (!loggedIn) return@LaunchedEffect
        loading = true
        try {
            shelves = InnerTubeClient.libraryPlaylists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorText = "Gagal memuat pustaka: ${e.message ?: e.javaClass.simpleName}"
        }
        loading = false
    }

    fun play(entry: ShelfItem, siblings: List<ShelfItem>) {
        scope.launch {
            try {
                entry.resolve(siblings)?.let { (song, queue) -> onPlay(song, queue) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorText = "Gagal memutar: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 200.dp),
    ) {
        item { ScreenHeader("Perpustakaan") }

        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                LibraryRow(Icons.Filled.Download, "Unduhan")
                HorizontalDivider(color = c.divider)
                LibraryRow(Icons.Filled.MusicNote, "Musik lokal")
            }
        }

        if (!loggedIn) {
            item {
                Text(
                    "Login dulu lewat profil untuk melihat playlist akunmu.",
                    color = c.textSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }

        if (loading) {
            item {
                Text(
                    "Memuat playlist…",
                    color = c.textSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }

        errorText?.let { msg ->
            item {
                Text(
                    msg,
                    color = c.textSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }

        shelves.forEachIndexed { index, shelf ->
            item(key = "lib-title-$index") { SectionHeader(shelf.title) }
            item(key = "lib-row-$index") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(shelf.items) { entry ->
                        ShelfCard(entry) { play(entry, shelf.items) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(icon: ImageVector, title: String) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = c.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textSecondary)
    }
}
