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
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.item
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.playback.QueueSection
import com.example.applemusicui.ui.theme.AppleRed

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

/**
 * Antrean seperti BitChord: riwayat, lagu yang sedang diputar, lalu lagu berikutnya yang ditambahkan
 * pengguna (bisa dinaikkan, diturunkan, atau dihapus), daftar asal, dan AutoPlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(controller: PlaybackController, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val entries = controller.entries
    val now = controller.currentIndex
    val hasUpcomingUser = entries.withIndex().any { it.index > now && it.value.section == QueueSection.USER }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface) {
        LazyColumn(
            Modifier.heightIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Antrean",
                        color = c.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasUpcomingUser) {
                        Text(
                            "Kosongkan",
                            color = AppleRed,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clickable { controller.clearUserUpcoming() }
                                .padding(8.dp),
                        )
                    }
                }
            }
            if (entries.isEmpty()) {
                item {
                    Text(
                        "Antrean masih kosong.",
                        color = c.textSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }

            // Riwayat: lagu yang sudah diputar, paling lama di atas.
            val past = entries.withIndex().filter { it.index < now }.takeLast(20)
            if (past.isNotEmpty()) {
                item { QueueHeading("Riwayat") }
                items(past, key = { "past-${it.index}" }) { indexed ->
                    QueueRow(indexed.value.song, highlighted = false, dim = true, onClick = { controller.jumpTo(indexed.index) }) {}
                }
            }

            // Yang sedang diputar.
            entries.getOrNull(now)?.let { entry ->
                item { QueueHeading("Sedang diputar") }
                item(key = "now") {
                    QueueRow(entry.song, highlighted = true, dim = false, onClick = {}) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = AppleRed, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Yang akan datang, per bagian.
            val sections = listOf(
                QueueSection.USER to "Berikutnya",
                QueueSection.CONTEXT to "Dari daftar",
                QueueSection.AUTOPLAY to "AutoPlay",
            )
            for ((section, label) in sections) {
                val upcoming = entries.withIndex().filter { it.index > now && it.value.section == section }
                if (upcoming.isEmpty()) continue
                item { QueueHeading(label) }
                items(upcoming, key = { "up-${section.name}-${it.index}" }) { indexed ->
                    val i = indexed.index
                    val isDragging = dragFrom == i
                    QueueRow(
                        indexed.value.song,
                        highlighted = false,
                        dim = false,
                        onClick = { controller.jumpTo(i) },
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset { IntOffset(0, if (isDragging) dragDy.roundToInt() else 0) }
                            .onSizeChanged { rowHeightPx = it.height.toFloat() },
                    ) {
                        if (section == QueueSection.USER) {
                            // Tahan pegangan lalu geser untuk mengatur urutan lagu berikutnya.
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .pointerInput(i) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                dragFrom = i
                                                dragDy = 0f
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragDy += amount.y
                                            },
                                            onDragEnd = {
                                                val steps = if (rowHeightPx > 0f) (dragDy / rowHeightPx).roundToInt() else 0
                                                if (steps != 0) controller.moveUserUpcomingBy(i, steps)
                                                dragFrom = null
                                                dragDy = 0f
                                            },
                                            onDragCancel = {
                                                dragFrom = null
                                                dragDy = 0f
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.DragHandle,
                                    contentDescription = "Geser untuk mengatur urutan",
                                    tint = AppTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        QueueAction(Icons.Filled.Close, "Hapus") { controller.removeUpcoming(i) }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun QueueHeading(text: String) {
    Text(
        text,
        color = AppTheme.colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun QueueRow(
    song: Song,
    highlighted: Boolean,
    dim: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit,
) {
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .alpha(if (dim) 0.55f else 1f)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(listOf(AppleRed, c.surface), Modifier.size(44.dp), corner = 6.dp, imageUrl = song.thumbnail)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (highlighted) AppleRed else c.text,
                fontSize = 15.sp,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                color = c.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Composable
private fun QueueAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
    }
}
