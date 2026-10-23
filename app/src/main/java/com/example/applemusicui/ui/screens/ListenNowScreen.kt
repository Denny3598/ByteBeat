package com.example.applemusicui.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
fun ListenNowScreen(current: Song, onPlay: (Song, List<Song>) -> Unit) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    var shelves by remember { mutableStateOf<List<Shelf>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            shelves = InnerTubeClient.home()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorText = "Gagal memuat beranda: ${e.message ?: e.javaClass.simpleName}"
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
        item { ScreenHeader("Listen Now") }

        if (loading) {
            item {
                Text(
                    "Memuat beranda…",
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
            item(key = "home-title-$index") { SectionHeader(shelf.title) }
            item(key = "home-row-$index") {
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
