package com.example.applemusicui.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.applemusicui.data.AppPrefs
import com.example.applemusicui.data.LyricsRepository
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.playback.QueueEntry
import com.example.applemusicui.ui.playback.QueueSection
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed
import kotlinx.coroutines.CancellationException
import kotlin.math.abs

/** Kunci preferensi: tampilkan riwayat di antrean atau tidak. */
const val QUEUE_SHOW_HISTORY_KEY = "queue_show_history"

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
 * Keadaan satu drag di antrean. Baris yang dipegang digambar sebagai overlay di atas daftar,
 * mengikuti jari; posisinya di daftar ditukar langsung setiap kali melewati tetangganya.
 * Diadaptasi dari BitChord (GPL v3).
 */
private class QueueDragState {
    var heldId by mutableStateOf<Long?>(null)
    var heldTop by mutableFloatStateOf(0f)
    var heldCenter = 0f
    var heldSize = 0
    /** Posisi atas baris yang dipegang saat tukar terakhir. Tukar baru dicek lagi setelah posisinya berubah. */
    var lastSwapOffset: Int? = null
}

/**
 * Antrean seperti BitChord: riwayat, lagu yang sedang diputar, lalu lagu berikutnya yang ditambahkan
 * pengguna (bisa ditarik untuk diatur urutannya, dihapus, atau dikosongkan), daftar asal, dan AutoPlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(controller: PlaybackController, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val entries = controller.entries
    val now = controller.currentIndex
    val listState = rememberLazyListState()
    val drag = remember { QueueDragState() }
    val hasUpcomingUser = entries.withIndex().any { it.index > now && it.value.section == QueueSection.USER }
    val prefs = AppPrefs.prefs(LocalContext.current)
    val showHistory = remember { prefs.getBoolean(QUEUE_SHOW_HISTORY_KEY, true) }
    val density = LocalDensity.current
    val edgeZonePx = with(density) { QUEUE_EDGE_ZONE.toPx() }
    val edgeSpeedPx = with(density) { QUEUE_EDGE_SPEED.toPx() }

    // Baris ditahan dekat tepi daftar: daftar ikut bergulir sendiri, dan penukaran posisi terus berjalan.
    LaunchedEffect(drag.heldId) {
        if (drag.heldId == null) return@LaunchedEffect
        // scrollBy hanya tersedia di dalam scroll { }, yang juga memberi prioritas gulir untuk loop ini.
        listState.scroll {
            var previous = withFrameNanos { it }
            while (true) {
                val id = drag.heldId ?: break
                val frame = withFrameNanos { it }
                // Frame yang terlewat tidak dibayar penuh, supaya daftar tidak melonjak.
                val seconds = ((frame - previous) / 1_000_000_000f).coerceAtMost(1f / 30f)
                previous = frame
                val info = listState.layoutInfo
                val speed = edgeScrollSpeed(
                    top = drag.heldTop,
                    bottom = drag.heldTop + drag.heldSize,
                    viewportStart = info.viewportStartOffset,
                    viewportEnd = info.viewportEndOffset,
                    zone = edgeZonePx,
                    speed = edgeSpeedPx,
                )
                if (speed != 0f && scrollBy(speed * seconds) != 0f) {
                    swapWithNeighbour(id, listState, drag, controller)
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface) {
        Box(Modifier.fillMaxWidth().heightIn(max = 620.dp)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
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
                val past = if (showHistory) entries.withIndex().filter { it.index < now }.takeLast(20) else emptyList()
                if (past.isNotEmpty()) {
                    item(key = "heading-past") { QueueHeading("Riwayat") }
                    items(past, key = { it.value.id }) { indexed ->
                        QueueRow(
                            indexed.value.song,
                            highlighted = false,
                            dim = true,
                            onClick = { controller.jumpTo(indexed.index) },
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = tween(200)),
                        ) {}
                    }
                }

                // Yang sedang diputar.
                entries.getOrNull(now)?.let { entry ->
                    item(key = "heading-now") { QueueHeading("Sedang diputar") }
                    item(key = entry.id) {
                        QueueRow(
                            entry.song,
                            highlighted = true,
                            dim = false,
                            onClick = {},
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = tween(200)),
                        ) {
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
                    item(key = "heading-${section.name}") { QueueHeading(label) }
                    itemsIndexed(upcoming, key = { _, indexed -> indexed.value.id }) { _, indexed ->
                        val entry = indexed.value
                        val i = indexed.index
                        val isHeld = drag.heldId == entry.id
                        QueueRow(
                            entry.song,
                            highlighted = false,
                            dim = false,
                            onClick = { controller.jumpTo(i) },
                            modifier = Modifier
                                .animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = tween(200))
                                .alpha(if (isHeld) 0f else 1f)
                        ) {
                            if (section == QueueSection.USER) {
                                QueueDragHandle(entry.id, listState, drag, controller)
                            }
                            QueueAction(Icons.Filled.Close, "Hapus") { controller.removeUpcoming(i) }
                        }
                    }
                }

                item(key = "bottom-spacer") { Spacer(Modifier.height(24.dp)) }
            }

            // Baris yang sedang ditarik, digambar di atas daftar tepat di bawah jari.
            drag.heldId?.let { id ->
                entries.firstOrNull { it.id == id }?.let { entry ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).zIndex(1f)) {
                        QueueRow(
                            entry.song,
                            highlighted = false,
                            dim = false,
                            onClick = {},
                            modifier = Modifier
                                .offset { IntOffset(0, drag.heldTop.toInt()) }
                                .background(c.surface),
                        ) {
                            Icon(
                                Icons.Filled.DragHandle,
                                contentDescription = null,
                                tint = c.textSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Jarak dari tepi daftar tempat gulir otomatis mulai, dan kecepatan maksimumnya. */
private val QUEUE_EDGE_ZONE = 40.dp
private val QUEUE_EDGE_SPEED = 340.dp

/**
 * Kecepatan gulir (px per detik) untuk baris yang ditahan di dekat tepi daftar.
 * Nol kalau baris berada di tengah. Diadaptasi dari BitChord (GPL v3).
 */
private fun edgeScrollSpeed(
    top: Float,
    bottom: Float,
    viewportStart: Int,
    viewportEnd: Int,
    zone: Float,
    speed: Float,
): Float {
    if (zone <= 0f) return 0f
    val intoStart = (viewportStart + zone) - top
    val intoEnd = bottom - (viewportEnd - zone)
    val reach = when {
        intoStart > 0f && intoEnd <= 0f -> -intoStart
        intoEnd > 0f && intoStart <= 0f -> intoEnd
        else -> return 0f
    }
    val ramp = speed * (0.2f + 0.8f * (abs(reach) / zone).coerceAtMost(1f))
    return if (reach < 0f) -ramp else ramp
}

/**
 * Pegangan untuk menarik lagu berikutnya. Tahan lalu geser; baris akan menukar posisi dengan
 * tetangganya setiap kali melewatinya, dan perubahan langsung dikirim ke pemutar.
 */
@Composable
private fun QueueDragHandle(
    entryId: Long,
    listState: androidx.compose.foundation.lazy.LazyListState,
    drag: QueueDragState,
    controller: PlaybackController,
) {
    Box(
        Modifier
            .size(36.dp)
            .pointerInput(entryId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entryId }
                            ?: return@detectDragGesturesAfterLongPress
                        drag.heldId = entryId
                        drag.heldSize = item.size
                        drag.heldCenter = item.offset + item.size / 2f
                        drag.heldTop = item.offset.toFloat()
                        drag.lastSwapOffset = null
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        if (drag.heldId != entryId) return@detectDragGesturesAfterLongPress
                        drag.heldCenter += amount.y
                        drag.heldTop = drag.heldCenter - drag.heldSize / 2f
                        swapWithNeighbour(entryId, listState, drag, controller)
                    },
                    onDragEnd = { endDrag(drag, entryId) },
                    onDragCancel = { endDrag(drag, entryId) },
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

private fun endDrag(drag: QueueDragState, entryId: Long) {
    if (drag.heldId != entryId) return
    drag.heldId = null
    drag.lastSwapOffset = null
}

/**
 * Menukar baris yang dipegang dengan tetangga terdekat yang juga lagu berikutnya dari pengguna,
 * kalau pusat baris sudah melewati setengah tinggi tetangganya.
 */
private fun swapWithNeighbour(
    entryId: Long,
    listState: androidx.compose.foundation.lazy.LazyListState,
    drag: QueueDragState,
    controller: PlaybackController,
) {
    val info = listState.layoutInfo
    val held = info.visibleItemsInfo.firstOrNull { it.key == entryId } ?: return
    // Tukar sebelumnya belum tampil di layout: jangan memutuskan lagi berdasarkan layout yang lama.
    drag.lastSwapOffset?.let { if (held.offset == it) return }
    drag.lastSwapOffset = null

    val entriesById: Map<Long, QueueEntry> = controller.entries.associateBy { it.id }
    val target = info.visibleItemsInfo
        .filter { item ->
            val id = item.key as? Long ?: return@filter false
            id != entryId && entriesById[id]?.section == QueueSection.USER
        }
        .minByOrNull { abs(it.offset + it.size / 2f - drag.heldCenter) }
        ?: return
    // Setengah tinggi baris per tukar, supaya baris tidak bolak-balik karena beda satu piksel.
    if (abs(drag.heldCenter - (target.offset + target.size / 2f)) > target.size / 2f) return

    val index = controller.entries.indexOfFirst { it.id == entryId }
    if (index < 0) return
    val dir = if (target.offset > held.offset) 1 else -1
    controller.moveUserUpcomingBy(index, dir)
    drag.lastSwapOffset = held.offset
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
    trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
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
