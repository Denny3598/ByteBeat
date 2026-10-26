package com.example.applemusicui.ui.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.applemusicui.data.Song
import com.example.applemusicui.data.SongExtras
import com.example.applemusicui.data.StreamResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Bagian antrean: dari mana sebuah lagu masuk. */
enum class QueueSection { USER, CONTEXT, AUTOPLAY }

private val queueEntrySeq = java.util.concurrent.atomic.AtomicLong()

/**
 * Satu lagu di antrean beserta bagiannya. [id] tetap sama walau entri pindah posisi,
 * dipakai sebagai key di LazyColumn supaya baris yang sedang di-drag tidak hilang saat ditukar.
 */
data class QueueEntry(
    val song: Song,
    val section: QueueSection,
    val id: Long = queueEntrySeq.incrementAndGet(),
)

/**
 * Pemutar untuk UI dan antrean. Hanya ada satu per proses, dan tidak dilepas saat layar ditutup,
 * supaya lagu berikutnya tetap diputar walau aplikasi tidak sedang terbuka.
 *
 * Antrean dibagi tiga bagian seperti BitChord:
 * - USER: lagu yang ditambahkan lewat "Putar berikutnya" atau "Tambahkan ke antrean".
 * - CONTEXT: daftar yang sedang diputar (album, playlist, dan sebagainya).
 * - AUTOPLAY: radio dari YouTube Music, ditambahkan saat antrean hampir habis.
 * Lagu sebelum [currentIndex] adalah riwayat.
 */
class PlaybackController private constructor(context: Context) {

    companion object {
        private const val MAX_RETRY = 2
        private const val AUTOPLAY_REFILL_BELOW = 2

        @Volatile
        private var instance: PlaybackController? = null

        fun get(context: Context): PlaybackController =
            instance ?: synchronized(this) {
                instance ?: PlaybackController(context.applicationContext).also { instance = it }
            }
    }

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

    private val queue = mutableStateListOf<QueueEntry>()
    /** Seluruh antrean (riwayat, lagu yang sedang diputar, dan yang akan datang). */
    val entries: List<QueueEntry> get() = queue
    var currentIndex by mutableIntStateOf(-1)
        private set

    private var loadJob: Job? = null
    private var autoplayJob: Job? = null
    private var lastAutoplaySeed: String? = null
    private var retries = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                retries = 0
                durationMs = player.duration.coerceAtLeast(0L)
            }
            if (playbackState == Player.STATE_ENDED) {
                // Lagu terakhir di antrean berhenti, seperti Apple Music.
                if (currentIndex >= 0 && currentIndex < queue.lastIndex) next() else isPlaying = false
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val song = current ?: return
            if (retries < MAX_RETRY) {
                // Stream bisa kedaluwarsa atau koneksi putus: ambil alamat baru dan lanjut dari posisi terakhir.
                retries++
                StreamResolver.invalidate(song.url)
                startCurrent(positionMs)
            } else {
                isPlaying = false
                loading = false
                errorText = "Lagu terhenti. Ketuk putar untuk mencoba lagi."
            }
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

    /** Putar [song]; [list] adalah daftar asal, dan lagu-lagu sebelum [song] jadi riwayat. */
    fun play(song: Song, list: List<Song> = listOf(song)) {
        val items = if (list.contains(song)) list else listOf(song) + list
        val index = items.indexOf(song)

        // Antrean yang ditambahkan pengguna tetap dipertahankan dan diletakkan tepat setelah lagu yang dipilih.
        val keptUser = if (currentIndex >= 0) {
            queue.drop(currentIndex + 1).filter { it.section == QueueSection.USER }
        } else {
            emptyList()
        }
        queue.clear()
        items.take(index).forEach { queue.add(QueueEntry(it, QueueSection.CONTEXT)) }
        queue.add(QueueEntry(song, QueueSection.CONTEXT))
        queue.addAll(keptUser)
        items.drop(index + 1).forEach { queue.add(QueueEntry(it, QueueSection.CONTEXT)) }

        currentIndex = items.take(index).size
        lastAutoplaySeed = null
        retries = 0
        startCurrent(0L)
    }

    /** Lompat ke lagu di indeks [index] di antrean (dipakai dari sheet Antrean). */
    fun jumpTo(index: Int) {
        if (index !in queue.indices) return
        currentIndex = index
        retries = 0
        startCurrent(0L)
    }

    private fun startCurrent(startMs: Long) {
        val entry = queue.getOrNull(currentIndex) ?: return
        val song = entry.song
        current = song
        errorText = null
        positionMs = startMs
        durationMs = 0L
        loadJob?.cancel()
        loading = true
        loadJob = scope.launch {
            try {
                val resolved = StreamResolver.audio(song.url)
                val item = MediaItem.Builder()
                    .setUri(resolved.url)
                    .setMimeType(resolved.mimeType)
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
                if (startMs > 0L) player.seekTo(startMs)
                player.play()
                loading = false
                prefetchNext()
                refillAutoplay(song)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                player.stop()
                errorText = e.message ?: "Gagal memutar lagu ini"
                loading = false
            }
        }
    }

    /** Menyiapkan alamat audio lagu berikutnya di latar belakang, supaya pindah lagu tidak jeda. */
    private fun prefetchNext() {
        val next = queue.getOrNull(currentIndex + 1) ?: return
        StreamResolver.prefetch(next.song.url)
    }

    /** Kalau antrean hampir habis, tambahkan radio dari lagu ini sebagai bagian AUTOPLAY. */
    private fun refillAutoplay(seed: Song) {
        val remaining = queue.size - currentIndex - 1
        if (remaining > AUTOPLAY_REFILL_BELOW) return
        if (autoplayJob?.isActive == true || lastAutoplaySeed == seed.url) return
        val videoId = seed.url.substringAfter("v=", "").substringBefore("&")
        if (videoId.isBlank()) return
        lastAutoplaySeed = seed.url
        autoplayJob = scope.launch {
            val more = runCatching { withContext(Dispatchers.IO) { SongExtras.radio(videoId) } }
                .getOrDefault(emptyList())
            // Tanpa lagu yang sama dua kali, dan tanpa satu artis mendominasi (lihat QueueBuilder).
            val picked = QueueBuilder.extend(queue.map { it.song }, more, limit = more.size)
            picked.forEach { queue.add(QueueEntry(it, QueueSection.AUTOPLAY)) }
        }
    }

    /**
     * Menambahkan lagu tepat setelah lagu yang sedang diputar, mendahului lagu lain di antrean pengguna.
     */
    fun playNext(song: Song) {
        if (currentIndex < 0) {
            play(song)
            return
        }
        queue.add(currentIndex + 1, QueueEntry(song, QueueSection.USER))
    }

    /** Menambahkan lagu di akhir bagian USER antrean. */
    fun addToQueue(song: Song) {
        if (currentIndex < 0) {
            play(song)
            return
        }
        var at = currentIndex + 1
        while (at < queue.size && queue[at].section == QueueSection.USER) at++
        queue.add(at, QueueEntry(song, QueueSection.USER))
    }

    /** Menghapus lagu yang belum diputar. Lagu yang sedang diputar dan riwayat tidak bisa dihapus. */
    fun removeUpcoming(index: Int) {
        if (index <= currentIndex || index !in queue.indices) return
        queue.removeAt(index)
    }

    /**
     * Memindahkan lagu yang belum diputar sejauh [steps] posisi (positif = ke bawah).
     * Hanya di dalam bagiannya sendiri (USER, CONTEXT, atau AUTOPLAY), dan berhenti di
     * batas bagian atau sebelum lagu yang sudah diputar. Diadaptasi dari BitChord (GPL v3).
     */
    fun moveUpcomingBy(index: Int, steps: Int) {
        if (steps == 0) return
        val dir = if (steps > 0) 1 else -1
        var at = index
        repeat(abs(steps)) {
            val to = at + dir
            if (at <= currentIndex || to <= currentIndex || to !in queue.indices) return
            if (queue[at].section != queue[to].section) return
            val item = queue.removeAt(at)
            queue.add(to, item)
            at = to
        }
    }

    /** Mengosongkan lagu USER yang belum diputar. */
    fun clearUserUpcoming() {
        for (i in queue.indices.reversed()) {
            if (i > currentIndex && queue[i].section == QueueSection.USER) queue.removeAt(i)
        }
    }

    fun togglePlay() {
        val song = current ?: return
        if (errorText != null) {
            // Setelah gagal, ketuk putar akan mengambil ulang stream.
            retries = 0
            startCurrent(positionMs)
            return
        }
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        if (queue.isEmpty() || currentIndex < 0) return
        jumpTo((currentIndex + 1) % queue.size)
    }

    /** Seperti BitChord: kalau sudah lewat 10 detik, ulang dari awal. */
    fun previous() {
        if (positionMs > BACK_RESTARTS_AFTER_MS) {
            seekTo(0L)
            return
        }
        if (queue.isEmpty() || currentIndex < 0) return
        jumpTo((currentIndex - 1 + queue.size) % queue.size)
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        positionMs = ms
    }
}
