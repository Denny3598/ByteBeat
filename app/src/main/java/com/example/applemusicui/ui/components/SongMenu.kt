package com.example.applemusicui.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.theme.AppTheme

/** Controller pemutar, dibagikan ke seluruh layar lewat CompositionLocal (diisi di AppRoot). */
val LocalPlaybackController = staticCompositionLocalOf<PlaybackController?> { null }

/** Popup: tampil saat baris lagu ditahan. Sheet: tampil saat titik tiga ditekan. */
enum class SongMenuStyle { Popup, Sheet }

private class SongAction(val label: String, val icon: ImageVector, val run: () -> Unit)

/**
 * Menu aksi untuk satu lagu. Hanya memuat aksi yang benar-benar berfungsi:
 * putar berikutnya, tambah ke antrean, dan bagikan.
 */
@Composable
fun SongMenuHost(song: Song, style: SongMenuStyle, onDismiss: () -> Unit) {
    when (style) {
        SongMenuStyle.Popup -> SongMenuPopup(song, onDismiss)
        SongMenuStyle.Sheet -> SongMenuSheet(song, onDismiss)
    }
}

@Composable
private fun rememberSongActions(song: Song, onDismiss: () -> Unit): List<SongAction> {
    val context = LocalContext.current
    val controller = LocalPlaybackController.current
    return listOf(
        SongAction("Putar berikutnya", Icons.Filled.PlaylistPlay) {
            controller?.playNext(song)
            onDismiss()
        },
        SongAction("Tambahkan ke antrean", Icons.Filled.QueueMusic) {
            controller?.addToQueue(song)
            onDismiss()
        },
        SongAction("Bagikan", Icons.Filled.Share) {
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, song.url)
            context.startActivity(Intent.createChooser(send, "Bagikan lagu"))
            onDismiss()
        },
    )
}

@Composable
private fun SongMenuPopup(song: Song, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val actions = rememberSongActions(song, onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            // Latar gelap. Ketuk di luar kartu untuk menutup.
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
                SongHeader(song)
                actions.forEach { action -> ActionRow(action.icon, action.label, action.run) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongMenuSheet(song: Song, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val actions = rememberSongActions(song, onDismiss)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
    ) {
        SongHeader(song)
        actions.forEach { action -> ActionRow(action.icon, action.label, action.run) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SongHeader(song: Song) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(song.art, Modifier.size(56.dp), corner = 6.dp, imageUrl = song.thumbnail)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                color = c.textSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
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
