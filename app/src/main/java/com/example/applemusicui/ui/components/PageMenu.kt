package com.example.applemusicui.ui.components

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.applemusicui.data.DetailRepository
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Halaman yang bisa ditahan: album, playlist, artis, atau show podcast. */
data class PageMenuTarget(
    val browseId: String,
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
)

/** Daftar lagu yang bisa diputar dari sebuah halaman, sesuai jenisnya. */
suspend fun playablesOf(browseId: String): List<Song> = when {
    browseId.startsWith("UC") -> DetailRepository.artist(browseId).topSongs
    browseId.startsWith("MPSP") -> DetailRepository.podcast(browseId).episodes.map { it.song }
    else -> DetailRepository.album(browseId).songs
}

/**
 * Popup saat kartu album, playlist, artis, atau podcast ditahan.
 * Tombol Putar dan Acak memuat lagunya dulu. Popup tetap terbuka sampai lagu siap.
 */
@Composable
fun PageMenuHost(
    target: PageMenuTarget,
    onPlay: (Song, List<Song>) -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun play(shuffle: Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            val songs = try {
                playablesOf(target.browseId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                emptyList()
            }
            busy = false
            val list = if (shuffle) songs.shuffled() else songs
            list.firstOrNull()?.let { onPlay(it, list) }
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss),
            )
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.surface),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(listOf(AppleRed, c.surface), Modifier.size(56.dp), corner = 8.dp, imageUrl = target.thumbnail)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            target.title,
                            color = c.text,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val line = if (busy) "Memuat lagu…" else target.subtitle
                        if (line.isNotBlank()) {
                            Text(
                                line,
                                color = c.textSecondary,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                PageAction(Icons.Filled.PlayArrow, "Putar") { play(shuffle = false) }
                PageAction(Icons.Filled.Shuffle, "Acak") { play(shuffle = true) }
                PageAction(Icons.Filled.OpenInNew, "Buka halaman") {
                    onOpen()
                    onDismiss()
                }
                PageAction(Icons.Filled.Share, "Bagikan") {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/browse/${target.browseId}")
                    context.startActivity(Intent.createChooser(send, "Bagikan"))
                    onDismiss()
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Membungkus kartu halaman: ketuk untuk membuka, tahan untuk popup aksi. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageMenuCard(
    target: PageMenuTarget,
    modifier: Modifier,
    onPlay: (Song, List<Song>) -> Unit,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier.combinedClickable(onClick = onClick, onLongClick = { menu = true })) {
        content()
    }
    if (menu) {
        PageMenuHost(target = target, onPlay = onPlay, onOpen = onClick, onDismiss = { menu = false })
    }
}

/** Kartu lagu: ketuk untuk memutar, tahan untuk popup lagu (Putar berikutnya, Antrean, Bagikan). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCardLongPress(
    song: Song?,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    // Ketuk dan tahan dipisah lewat pointerInput, supaya tahan lama tidak tertelan gulir baris di sekitarnya.
    Box(
        modifier.pointerInput(song, onClick) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { if (song != null) menu = true },
            )
        },
    ) {
        content()
    }
    if (menu && song != null) {
        SongMenuHost(song = song, style = SongMenuStyle.Popup, onDismiss = { menu = false })
    }
}

@Composable
private fun PageAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = c.text, fontSize = 16.sp)
    }
}
