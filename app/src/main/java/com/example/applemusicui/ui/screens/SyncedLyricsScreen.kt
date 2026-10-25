package com.example.applemusicui.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.applemusicui.data.LyricLine
import com.example.applemusicui.data.Song
import com.example.applemusicui.data.SyncedLyricsRepository
import com.example.applemusicui.ui.components.rememberCoverTint
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlin.math.abs

private sealed interface SyncedState {
    data object Loading : SyncedState
    data class Ok(val lines: List<LyricLine>) : SyncedState
    data object None : SyncedState
}

/**
 * Lirik layar penuh. Kalau ada lirik berwaktu, tampil seperti BitChord: baris yang sedang dinyanyikan
 * besar dan terang dengan efek cahaya, kata-katanya menyala berurutan, baris lain memudar dan buram
 * menurut jaraknya, ketuk baris untuk loncat, dan ada tombol offset untuk menggeser waktu lirik.
 * Kalau tidak ada, dipakai lirik teks biasa dari [LyricsSheet].
 */
@Composable
fun LyricsView(song: Song, controller: PlaybackController, onDismiss: () -> Unit) {
    val state by produceState<SyncedState>(SyncedState.Loading, song.url) {
        value = try {
            val lines = SyncedLyricsRepository.fetch(song.title, song.artist, controller.durationMs / 1000)
            if (lines.isNullOrEmpty()) SyncedState.None else SyncedState.Ok(lines)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncedState.None
        }
    }

    when (val s = state) {
        is SyncedState.Loading -> Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize().background(AppTheme.colors.background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SyncedState.Ok -> SyncedLyricsDialog(s.lines, song, controller, onDismiss)
        is SyncedState.None -> LyricsSheet(song = song, onDismiss = onDismiss)
    }
}

@Composable
private fun SyncedLyricsDialog(
    lines: List<LyricLine>,
    song: Song,
    controller: PlaybackController,
    onDismiss: () -> Unit,
) {
    val c = AppTheme.colors
    val tint = rememberCoverTint(song.thumbnail)
    val listState = rememberLazyListState()
    // Offset positif = lirik muncul lebih cepat; negatif = lebih lambat. Dalam milidetik.
    var offsetMs by remember { mutableLongStateOf(0L) }

    val activeIndex by remember(lines) {
        derivedStateOf { activeLine(lines, controller.positionMs + offsetMs) }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) listState.animateScrollToItem(activeIndex)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to (tint ?: c.background),
                        0.6f to c.background,
                        1f to c.background,
                    ),
                ),
        ) {
            LyricParticles(color = tint ?: c.text, modifier = Modifier.fillMaxSize())
            BoxWithConstraints(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().statusBarsPadding(),
                    contentPadding = PaddingValues(
                        top = maxHeight * 0.35f,
                        bottom = maxHeight * 0.45f,
                    ),
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Tutup", tint = c.text)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(song.artist, color = c.textSecondary, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                    itemsIndexed(lines) { i, line ->
                        val dist = if (activeIndex < 0) 3 else abs(i - activeIndex)
                        val alpha by animateFloatAsState(
                            when (dist) {
                                0 -> 1f
                                1 -> 0.6f
                                2 -> 0.4f
                                else -> 0.25f
                            },
                            label = "lyricAlpha",
                        )
                        val size by animateFloatAsState(if (dist == 0) 30f else 24f, label = "lyricSize")
                        val blur by animateDpAsState(
                            when (dist) {
                                0, 1 -> 0.dp
                                2 -> 2.dp
                                else -> 4.dp
                            },
                            label = "lyricBlur",
                        )
                        val plain = line.text.ifBlank { "…" }
                        val text: AnnotatedString = if (dist == 0) {
                            // Kata yang sudah lewat tampil terang, sisanya redup; dibagi rata sepanjang durasi baris.
                            val end = lines.getOrNull(i + 1)?.timeMs ?: (line.timeMs + 4000L)
                            val span = (end - line.timeMs).coerceAtLeast(1L)
                            val frac = ((controller.positionMs + offsetMs - line.timeMs).toFloat() / span).coerceIn(0f, 1f)
                            sweptText(plain, frac, c.text)
                        } else {
                            AnnotatedString(plain)
                        }
                        Text(
                            text = text,
                            color = c.text.copy(alpha = alpha),
                            fontSize = size.sp,
                            lineHeight = (size * 1.2f).sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                shadow = if (dist == 0) {
                                    Shadow(color = c.text.copy(alpha = 0.45f), blurRadius = 32f)
                                } else {
                                    null
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                                .blur(blur)
                                .clickable { controller.seekTo((line.timeMs - offsetMs).coerceAtLeast(0L)) },
                        )
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OffsetButton("Lebih cepat") { offsetMs += 500L }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Offset ${"%.1f".format(offsetMs / 1000f)} dtk",
                                color = c.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { offsetMs = 0L },
                            )
                            Spacer(Modifier.width(12.dp))
                            OffsetButton("Lebih lambat") { offsetMs -= 500L }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OffsetButton(label: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(c.textSecondary.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = c.text, fontSize = 13.sp)
    }
}

/** Teks dengan kata-kata yang sudah lewat tampil terang, sisanya redup. */
private fun sweptText(text: String, frac: Float, bright: Color): AnnotatedString {
    val words = text.split(" ")
    val played = (frac * words.size).toInt()
    return buildAnnotatedString {
        words.forEachIndexed { idx, word ->
            val color = if (idx < played) bright else bright.copy(alpha = 0.4f)
            withStyle(SpanStyle(color = color)) { append(word) }
            if (idx < words.lastIndex) append(" ")
        }
    }
}

/** Indeks baris terakhir yang waktunya sudah lewat, atau -1 kalau belum ada yang mulai. */
private fun activeLine(lines: List<LyricLine>, posMs: Long): Int {
    var lo = 0
    var hi = lines.size - 1
    var ans = -1
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        if (lines[mid].timeMs <= posMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return ans
}
