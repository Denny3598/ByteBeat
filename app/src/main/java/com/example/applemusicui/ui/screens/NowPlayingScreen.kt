package com.example.applemusicui.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.theme.AppleRed
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Layar ini selalu di atas latar gelap (cover blur), jadi warnanya tetap putih.
private val Fg = Color.White
private val FgDim = Color.White.copy(alpha = 0.7f)
private val TrackOff = Color.White.copy(alpha = 0.3f)

@Composable
fun NowPlayingScreen(
    song: Song,
    controller: PlaybackController,
    startRect: Rect? = null,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    var volume by remember { mutableStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }

    var scrub by remember { mutableStateOf<Float?>(null) }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    var repeat by rememberSaveable { mutableStateOf(false) }

    // 0f = posisi mini player, 1f = pemutar penuh.
    val progress = remember { Animatable(if (startRect == null) 1f else 0f) }
    var coverRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
    }

    fun dismiss() {
        scope.launch {
            progress.animateTo(0f, tween(durationMillis = 280))
            onClose()
        }
    }

    val p = progress.value
    val titleAlpha = ((p - 0.2f) / 0.6f).coerceIn(0f, 1f)
    val controlsAlpha = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)

    // Cover membesar saat diputar dan mengecil saat dijeda.
    val coverScale by animateFloatAsState(
        targetValue = if (controller.isPlaying) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "coverScale",
    )

    // Cover selebar layar, berpindah dan membesar dari posisi cover di mini player.
    val coverModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .onGloballyPositioned { coverRect = it.boundsInRoot() }
        .graphicsLayer {
            val t = coverRect
            val s = startRect
            if (t != null && s != null) {
                val scale = lerp(s.width, t.width, p) / t.width * coverScale
                val cx = lerp(s.center.x, t.center.x, p)
                val cy = lerp(s.center.y, t.center.y, p)
                val visualLeft = cx - t.width * scale / 2f
                val visualTop = cy - t.height * scale / 2f
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
                translationX = visualLeft - t.left
                translationY = visualTop - t.top
                alpha = 1f
            } else {
                scaleX = coverScale
                scaleY = coverScale
                alpha = if (s != null) 0f else 1f
            }
        }

    val duration = controller.durationMs.toFloat().coerceAtLeast(1f)
    val position = controller.positionMs.toFloat().coerceIn(0f, duration)
    val shown = scrub ?: position

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = p)),
    ) {
        // Latar: cover yang di-blur. Di Android 12 ke bawah blur tidak aktif,
        // jadi lapisan gelap di bawahnya tetap menjaga keterbacaan.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = p }
                .blur(60.dp),
        ) {
            Artwork(
                colors = song.art,
                modifier = Modifier.fillMaxSize(),
                corner = 0.dp,
                imageUrl = song.thumbnail,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f * p),
                            Color.Black.copy(alpha = 0.85f * p),
                        ),
                    ),
                ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Gagang kecil di atas, ketuk untuk menutup
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable { dismiss() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.5f)),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Cover selebar layar
            Box(coverModifier) {
                Artwork(
                    colors = song.art,
                    modifier = Modifier.fillMaxSize(),
                    corner = 0.dp,
                    imageUrl = song.thumbnail,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Judul, progres, dan waktu (muncul lebih dulu)
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        alpha = titleAlpha
                        translationY = (1f - titleAlpha) * 30f
                    },
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            color = Fg,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song.artist,
                            color = FgDim,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    CircleIcon(Icons.Filled.FavoriteBorder, "Suka")
                    Spacer(Modifier.width(10.dp))
                    CircleIcon(Icons.Filled.MoreHoriz, "Opsi")
                }

                Spacer(Modifier.height(20.dp))

                SlimSlider(
                    value = shown,
                    range = 0f..duration,
                    onValueChange = { scrub = it },
                    onValueChangeFinished = {
                        scrub?.let { controller.seekTo(it.toLong()) }
                        scrub = null
                    },
                )

                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(shown.toLong()), color = FgDim, fontSize = 12.sp)
                    Text(
                        "-" + formatTime(duration.toLong() - shown.toLong()),
                        color = FgDim,
                        fontSize = 12.sp,
                    )
                }

                controller.errorText?.let {
                    Text(
                        it,
                        color = AppleRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Kontrol, volume, dan aksi bawah (muncul sedikit setelah judul)
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        alpha = controlsAlpha
                        translationY = (1f - controlsAlpha) * 30f
                    },
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Acak",
                        tint = if (shuffle) Fg else FgDim,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { shuffle = !shuffle }
                            .padding(10.dp),
                    )
                    Icon(
                        Icons.Filled.FastRewind,
                        contentDescription = "Sebelumnya",
                        tint = Fg,
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
                        tint = Fg,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { controller.next() }
                            .padding(14.dp),
                    )
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = "Ulangi",
                        tint = if (repeat) Fg else FgDim,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { repeat = !repeat }
                            .padding(10.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.VolumeDown,
                        contentDescription = "Volume kecil",
                        tint = FgDim,
                        modifier = Modifier.size(20.dp),
                    )
                    SlimSlider(
                        value = volume,
                        range = 0f..maxVolume.toFloat(),
                        onValueChange = {
                            volume = it
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0)
                        },
                        onValueChangeFinished = {},
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    )
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = "Volume besar",
                        tint = FgDim,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    BottomAction(Icons.Filled.IosShare, "Bagikan")
                    BottomAction(Icons.Filled.FormatQuote, "Lirik")
                    BottomAction(Icons.Filled.FavoriteBorder, "Favorit")
                    BottomAction(Icons.Filled.QueueMusic, "Antrean")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Progress/volume bar tipis tanpa thumb, seperti Apple Music. Area sentuh 24dp, garis 4dp. */
@Composable
private fun SlimSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)
    var width by remember { mutableStateOf(1f) }
    val changed by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)

    Box(
        modifier
            .fillMaxWidth()
            .height(24.dp)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(range) {
                detectTapGestures(
                    onTap = { o ->
                        changed(range.start + (o.x / width).coerceIn(0f, 1f) * span)
                        finished()
                    },
                )
            }
            .pointerInput(range) {
                detectHorizontalDragGestures(
                    onDragEnd = { finished() },
                    onDragCancel = { finished() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        changed(range.start + (change.position.x / width).coerceIn(0f, 1f) * span)
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(TrackOff),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(CircleShape)
                .background(Fg),
        )
    }
}

@Composable
private fun CircleIcon(icon: ImageVector, description: String) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Fg, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BottomAction(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = Fg, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = FgDim, fontSize = 11.sp)
    }
}

private fun formatTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
