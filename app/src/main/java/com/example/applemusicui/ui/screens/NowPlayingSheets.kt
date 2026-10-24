package com.example.applemusicui.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.LyricsRepository
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.SongRow
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException

/** Sheet yang bisa dibuka dari pemutar penuh. */
enum class NowSheet { Lyrics, Queue }

/** Bagikan tautan lagu lewat pilihan aplikasi yang ada di HP. */
fun shareSong(context: Context, song: Song) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, song.url)
    context.startActivity(Intent.createChooser(send, "Bagikan lagu"))
}

private sealed interface LyricsState {
    data object Loading : LyricsState
    data object None : LyricsState
    data class Ok(val text: String) : LyricsState
    data class Error(val message: String) : LyricsState
}

/** Lirik lagu yang sedang diputar, dibuka sebagai sheet dari bawah. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(song: Song, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val videoId = song.url.substringAfter("v=", "").substringBefore("&")
    val state by produceState<LyricsState>(LyricsState.Loading, videoId) {
        value = try {
            val text = LyricsRepository.plain(videoId)
            if (text == null) LyricsState.None else LyricsState.Ok(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LyricsState.Error("Lirik gagal dimuat (${e.message.orEmpty()})")
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text("Lirik", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(song.title, color = c.textSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            when (val s = state) {
                is LyricsState.Loading -> CircularProgressIndicator(Modifier.padding(top = 12.dp))
                is LyricsState.Ok -> Text(s.text, color = c.text, fontSize = 17.sp, lineHeight = 28.sp)
                is LyricsState.None -> Text(
                    "Lirik untuk lagu ini belum tersedia.",
                    color = c.textSecondary,
                    fontSize = 15.sp,
                )
                is LyricsState.Error -> Text(s.message, color = c.textSecondary, fontSize = 15.sp)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Antrean pemutaran, dibuka sebagai sheet. Ketuk lagu untuk memutarnya. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(current: Song, controller: PlaybackController, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val list = remember(current.url) { controller.queueSnapshot() }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface) {
        Text(
            "Antrean",
            color = c.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (list.isEmpty()) {
            Text(
                "Antrean masih kosong.",
                color = c.textSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        } else {
            LazyColumn(
                Modifier.heightIn(max = 560.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                items(list) { song ->
                    SongRow(song, highlighted = song.url == current.url) {
                        controller.play(song, list)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
