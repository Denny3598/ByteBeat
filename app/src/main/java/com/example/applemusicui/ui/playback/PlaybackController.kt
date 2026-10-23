package com.example.applemusicui.ui.playback

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultMediaSourceFactory
import androidx.media3.exoplayer.ExoPlayer
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

/** Pemutar musik: menyimpan lagu aktif, antrean, dan posisi putar untuk UI. */
class PlaybackController(context: Context) {

    private val dataSource = DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    var current by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set

    private var queue: List<Song> = emptyList()
    private var loadJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
                if (playbackState == Player.STATE_ENDED) next()
            }
        })

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
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                player.play()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                player.stop()
                errorText = e.message ?: "Gagal memutar lagu ini"
            } finally {
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

    fun release() {
        scope.cancel()
        player.release()
    }

    companion object {
        // Sama dengan User-Agent di DownloaderImpl, supaya alamat audio diterima YouTube.
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0"
    }
}
