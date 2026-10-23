package com.example.applemusicui.ui.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.applemusicui.data.Song
import com.example.applemusicui.data.StreamResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Pemutar untuk UI. Player aslinya dipegang [SharedPlayer] dan service latar belakang. */
class PlaybackController(context: Context) {

    private val appContext = context.applicationContext
    private val player: ExoPlayer = SharedPlayer.get(appContext)

    var current: Song? by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(player.isPlaying)
        private set
    var loading by mutableStateOf(false)
        private set
    var errorText: String? by mutableStateOf<String?>(null)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set

    private var queue: List<Song> = emptyList()
    private var loadJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                durationMs = player.duration.coerceAtLeast(0L)
            }
            if (playbackState == Player.STATE_ENDED) next()
        }
    }

    // Membuat koneksi ke service, sehingga service ikut hidup dan notifikasi muncul.
    private val browserFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, MusicPlaybackService::class.java)),
    ).buildAsync()

    init {
        player.addListener(listener)
        // Posisi diperbarui 4 kali per detik untuk slider dan waktu.
        scope.launch {
            while (isActive) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                delay(250)
            }
        }
    }

    /** Putar [song]; [list] adalah antrean untuk tombol berikutnya/sebelumnya. */
    fun play(song: Song, list: List<Song> = listOf(song)) {
        queue = list
        current = song
        errorText = null
        positionMs = 0L
        durationMs = 0L
        loadJob?.cancel()
        loading = true
        loadJob = scope.launch {
            try {
                val url = StreamResolver.audioUrl(song.url)
                val item = MediaItem.Builder()
                    .setUri(url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setArtworkUri(song.thumbnail?.let { Uri.parse(it) })
                            .build(),
                    )
                    .build()
                player.setMediaItem(item)
                player.prepare()
                player.play()
                loading = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                player.stop()
                errorText = e.message ?: "Gagal memutar lagu ini"
                loading = false
            }
        }
    }

    fun togglePlay() {
        if (current == null) return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        val index = queue.indexOf(current)
        if (queue.isEmpty() || index < 0) return
        play(queue[(index + 1) % queue.size], queue)
    }

    /** Seperti Apple Music: kalau sudah lewat 3 detik, ulang dari awal. */
    fun previous() {
        if (positionMs > 3_000L) {
            seekTo(0L)
            return
        }
        val index = queue.indexOf(current)
        if (queue.isEmpty() || index < 0) return
        play(queue[(index - 1 + queue.size) % queue.size], queue)
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        positionMs = ms
    }

    /** Hanya melepas listener dan koneksi UI. Musik tetap berjalan di service. */
    fun release() {
        player.removeListener(listener)
        scope.cancel()
        MediaController.releaseFuture(browserFuture)
    }
}
