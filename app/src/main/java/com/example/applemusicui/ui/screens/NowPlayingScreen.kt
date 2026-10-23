package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed

@Composable
fun NowPlayingScreen(song: Song, controller: PlaybackController, onClose: () -> Unit) {
    val c = AppTheme.colors
    var scrub by remember { mutableStateOf<Float?>(null) }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    var repeat by rememberSaveable { mutableStateOf(false) }

    val duration = controller.durationMs.toFloat().coerceAtLeast(1f)
    val position = controller.positionMs.toFloat().coerceIn(0f, duration)
    val shown = scrub ?: position

    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .background(
                Brush.verticalGradient(
                    listOf(song.art.first().copy(alpha = 0.75f), c.background),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            // Tombol turun
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Tutup",
                    tint = c.text,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                        .padding(6.dp),
                )
            }

            Spacer(Modifier.size(24.dp))

            // Cover
            Artwork(
                colors = song.art,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                corner = 14.dp,
                imageUrl = song.thumbnail,
            )

            Spacer(Modifier.size(28.dp))

            // Judul + tombol opsi
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = c.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artist,
                        color = c.textSecondary,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                CircleIcon(Icons.Filled.MoreHoriz, "Opsi")
            }

            Spacer(Modifier.size(22.dp))

            // Progres
            Slider(
                value = shown,
                onValueChange = { scrub = it },
                onValueChangeFinished = {
                    scrub?.let { controller.seekTo(it.toLong()) }
                    scrub = null
                },
                valueRange = 0f..duration,
                colors = SliderDefaults.colors(
                    thumbColor = c.text,
                    activeTrackColor = c.text,
                    inactiveTrackColor = c.divider,
                ),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text(formatTime(shown.toLong()), color = c.textSecondary, fontSize = 13.sp)
                Text(
                    "-" + formatTime(duration.toLong() - shown.toLong()),
                    color = c.textSecondary,
                    fontSize = 13.sp,
                )
            }

            if (controller.errorText != null) {
                Text(
                    controller.errorText.orEmpty(),
                    color = AppleRed,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            // Kontrol pemutaran
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            ) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Acak",
                    tint = if (shuffle) c.text else c.textSecondary,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { shuffle = !shuffle }
                        .padding(10.dp),
                )
                Icon(
                    Icons.Filled.FastRewind,
                    contentDescription = "Sebelumnya",
                    tint = c.text,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable { controller.previous() }
                        .padding(14.dp),
                )
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { controller.togglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (controller.loading) {
                        CircularProgressIndicator(
                            Modifier.size(32.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            if (controller.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (controller.isPlaying) "Jeda" else "Putar",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Icon(
                    Icons.Filled.FastForward,
                    contentDescription = "Berikutnya",
                    tint = c.text,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable { controller.next() }
                        .padding(14.dp),
                )
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = "Ulangi",
                    tint = if (repeat) c.text else c.textSecondary,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { repeat = !repeat }
                        .padding(10.dp),
                )
            }

            Spacer(Modifier.size(28.dp))

            // Aksi bawah
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            ) {
                BottomAction(Icons.Filled.IosShare, "Bagikan")
                BottomAction(Icons.Filled.FormatQuote, "Lirik")
                BottomAction(Icons.Filled.FavoriteBorder, "Suka")
                BottomAction(Icons.Filled.QueueMusic, "Antrean")
            }

            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String) {
    val c = AppTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.control),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = c.text, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun BottomAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    val c = AppTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = c.text, modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(4.dp))
        Text(label, color = c.textSecondary, fontSize = 12.sp)
    }
}

private fun formatTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
