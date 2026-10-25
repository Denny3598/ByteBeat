package com.example.applemusicui.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.FavoritesStore
import com.example.applemusicui.data.LikeRepository
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.components.SongMenuHost
import com.example.applemusicui.ui.components.SongMenuStyle
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.theme.AppleRed
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Layar ini selalu di atas latar gelap (cover blur), jadi warnanya tetap putih.
private val Fg = Color.White
private val FgDim = Color.White.copy(alpha = 0.7f)
private val TrackOff = Color.White.copy(alpha = 0.3f)

/**
 * Pemutar penuh. Naik dari bawah saat dibuka, dan turun saat ditutup atau
 * ditarik ke bawah (sheet mengikuti jari, lalu menutup kalau sudah cukup jauh).
 */
@Composable
fun NowPlayingScreen(
    song: Song,
    controller: PlaybackController,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    var volume by remember { mutableStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }

    var scrub by remember { mutableStateOf<Float?>(null) }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    var repeat by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf<SongMenuStyle?>(null) }
    var sheet by remember { mutableStateOf<NowSheet?>(null) }
    var liked by remember(song.url) { mutableStateOf(FavoritesStore.isFavorite(context, song.url)) }

    // Tampilan langsung berubah, lalu dikirim ke akun YouTube Music. Kalau gagal, dikembalikan.
    fun toggleLike() {
        liked = !liked
        scope.launch { liked = LikeRepository.toggle(context, song.url) }
    }

    // Posisi sheet dalam piksel: screenHeightPx = tersembunyi di bawah, 0f = terbuka penuh.
    val dragY = remember { Animatable(screenHeightPx) }

    LaunchedEffect(Unit) {
        dragY.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
    }

    fun close() {
        scope.launch {
            dragY.animateTo(screenHeightPx, tween(durationMillis = 300, easing = FastOutSlowInEasing))
            onClose()
        }
    }

    fun settle() {
        scope.launch {
            dragY.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium))
        }
    }

    val p = 1f - (dragY.value / screenHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val titleAlpha = ((p - 0.2f) / 0.6f).coerceIn(0f, 1f)
    val controlsAlpha = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)

    val duration = controller.durationMs.toFloat().coerceAtLeast(1f)
    val position = controller.positionMs.toFloat().coerceIn(0f, duration)
    val shown = scrub ?: position

    // Latar gelap di belakang sheet, makin pudar saat sheet ditarik turun.
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * p)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = dragY.value }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragY.value > screenHeightPx * 0.3f) close() else settle()
                        },
                        onDragCancel = { settle() },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                dragY.snapTo((dragY.value + dragAmount).coerceAtLeast(0f))
                            }
                        },
                    )
                },
        ) {
            // Latar: cover yang di-blur di seluruh layar. Di Android 12 ke bawah blur tidak aktif,
            // jadi lapisan gelap di atasnya tetap menjaga keterbacaan.
            Box(
                Modifier
                    .fillMaxSize()
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
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )

            Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                // Cover edge-to-edge sampai atas layar. Bagian bawahnya memudar ke latar blur,
                // jadi tidak ada batas yang terlihat.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.5f to Color.Black,
                                    1f to Color.Transparent,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        },
                ) {
                    Artwork(
                        colors = song.art,
                        modifier = Modifier.fillMaxSize(),
                        corner = 0.dp,
                        imageUrl = song.thumbnail,
                    )
                }

                // Judul, progres, dan waktu (muncul lebih dulu)
                Column(
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 12.dp)
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
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                song.artist,
                                color = FgDim,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        CircleIcon(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Suka",
                        ) { toggleLike() }
                        Spacer(Modifier.width(10.dp))
                        CircleIcon(Icons.Filled.MoreHoriz, "Opsi") { menu = SongMenuStyle.Sheet }
                    }

                    Spacer(Modifier.height(18.dp))

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

                // Kontrol, volume, dan aksi bawah (muncul sedikit setelah judul)
                Column(
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
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

                    Spacer(Modifier.height(20.dp))

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

                    Spacer(Modifier.height(20.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        BottomAction(Icons.Filled.IosShare, "Bagikan") { shareSong(context, song) }
                        BottomAction(Icons.Filled.FormatQuote, "Lirik") { sheet = NowSheet.Lyrics }
                        BottomAction(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Favorit",
                        ) { toggleLike() }
                        BottomAction(Icons.Filled.QueueMusic, "Antrean") { sheet = NowSheet.Queue }
                    }
                }
            }

            // Gagang di atas, di area status bar. Ketuk untuk menutup, atau tarik ke bawah.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .clickable { close() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                )
            }
        }

        // Menu aksi lagu saat titik tiga ditekan.
        menu?.let { style ->
            SongMenuHost(song = song, style = style, onDismiss = { menu = null })
        }

        // Sheet Lirik dan Antrean.
        when (sheet) {
            NowSheet.Lyrics -> LyricsSheet(song = song, onDismiss = { sheet = null })
            NowSheet.Queue -> QueueSheet(current = song, controller = controller, onDismiss = { sheet = null })
            null -> Unit
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
private fun CircleIcon(icon: ImageVector, description: String, onClick: () -> Unit = {}) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Fg, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BottomAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Fg, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = FgDim, fontSize = 11.sp)
    }
}

private fun formatTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
