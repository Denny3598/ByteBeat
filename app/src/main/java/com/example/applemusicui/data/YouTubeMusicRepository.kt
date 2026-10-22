package com.example.applemusicui.data

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Mengambil data dari YouTube Music lewat NewPipe Extractor. */
object YouTubeMusicRepository {

    private var initialized = false

    /** Hasil pencarian terakhir; dipakai pemutar sebagai antrean lagu berikutnya. */
    @Volatile
    var lastResults: List<Song> = emptyList()
        private set

    @Synchronized
    private fun ensureInit() {
        if (!initialized) {
            NewPipe.init(DownloaderImpl.instance, Localization("id", "ID"))
            initialized = true
        }
    }

    /** Cari lagu di YouTube Music dan simpan hasilnya sebagai antrean. */
    suspend fun searchSongs(query: String): List<Song> {
        val songs = search(query)
        if (query.isNotBlank()) lastResults = songs
        return songs
    }

    /** Cari lagu yang cocok dengan judul + artis (untuk lagu dummy tanpa alamat). */
    suspend fun findSong(song: Song): Song {
        return search("${song.title} ${song.artist}").firstOrNull()
            ?: throw IllegalStateException("Lagu tidak ditemukan di YouTube Music")
    }

    /** Ambil alamat audio terbaik untuk sebuah video. */
    suspend fun audioUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        ensureInit()
        val info = StreamInfo.getInfo(videoUrl)
        val streams = info.audioStreams
        val best = streams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate }
            ?: throw IllegalStateException("Stream audio tidak tersedia")
        best.content
    }

    private suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()

        ensureInit()
        val extractor = ServiceList.YouTube.getSearchExtractor(
            q,
            listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS),
            "",
        )
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toSong() }
    }

    private fun StreamInfoItem.toSong(): Song {
        val title = name ?: ""
        val cover = thumbnails.maxByOrNull { it.height }?.url
        return Song(
            title = title,
            artist = uploaderName ?: "",
            art = placeholderColors(title),
            url = url ?: "",
            thumbnail = cover,
        )
    }

    private val palette = listOf(
        listOf(Color(0xFF1B2A49), Color(0xFF3C5A99)),
        listOf(Color(0xFFD93A2B), Color(0xFFF08A5D)),
        listOf(Color(0xFF3B1E8C), Color(0xFFE040FB)),
        listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
        listOf(Color(0xFF3A2A22), Color(0xFFB07A4F)),
        listOf(Color(0xFF0F7A1F), Color(0xFF6FCF7F)),
    )

    private fun placeholderColors(seed: String): List<Color> {
        val index = (seed.hashCode() and Int.MAX_VALUE) % palette.size
        return palette[index]
    }
}
