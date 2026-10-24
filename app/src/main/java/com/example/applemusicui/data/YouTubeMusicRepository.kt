package com.example.applemusicui.data

import android.net.Uri
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Hasil pencarian yang sudah dikelompokkan seperti YouTube Music. */
data class SearchResults(
    val songs: List<Song>,
    val artists: List<LibraryItem>,
    val albums: List<LibraryItem>,
    val playlists: List<LibraryItem>,
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && artists.isEmpty() && albums.isEmpty() && playlists.isEmpty()
}

/** Mengambil data dari YouTube Music lewat NewPipe Extractor. */
object YouTubeMusicRepository {

    private var initialized = false

    @Synchronized
    fun ensureInit() {
        if (!initialized) {
            NewPipe.init(DownloaderImpl.instance, Localization("id", "ID"))
            initialized = true
        }
    }

    /** Cari lagu di YouTube Music. */
    suspend fun searchSongs(query: String, songsOnly: Boolean = true): List<Song> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()

        ensureInit()
        val extractor = ServiceList.YouTube.getSearchExtractor(
            q,
            if (songsOnly) listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS) else emptyList(),
            "",
        )
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toSong() }
    }

    /**
     * Cari lagu, artis, album, dan playlist sekaligus. Setiap bagian diambil terpisah,
     * jadi kalau satu bagian gagal, bagian lain tetap tampil.
     */
    suspend fun searchAll(query: String, songsOnly: Boolean = true): SearchResults = coroutineScope {
        val q = query.trim()
        if (q.isEmpty()) return@coroutineScope SearchResults(emptyList(), emptyList(), emptyList(), emptyList())

        val songs = async(Dispatchers.IO) {
            attempt { searchSongs(q, songsOnly) }
        }
        val artists = async(Dispatchers.IO) {
            attempt {
                searchFiltered(q, YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS)
                    .filterIsInstance<ChannelInfoItem>()
                    .mapNotNull { it.toArtist() }
            }
        }
        val albums = async(Dispatchers.IO) {
            attempt {
                searchFiltered(q, YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS)
                    .filterIsInstance<PlaylistInfoItem>()
                    .mapNotNull { it.toCollection() }
            }
        }
        val playlists = async(Dispatchers.IO) {
            attempt {
                searchFiltered(q, YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS)
                    .filterIsInstance<PlaylistInfoItem>()
                    .mapNotNull { it.toCollection() }
            }
        }

        SearchResults(
            songs = songs.await(),
            artists = artists.await(),
            albums = albums.await(),
            playlists = playlists.await(),
        )
    }

    /** Menjalankan satu pencarian; kalau gagal, hasilnya kosong saja (kecuali pembatalan). */
    private inline fun <T> attempt(block: () -> List<T>): List<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    /** Pencarian dengan filter jenis konten tertentu. Harus dipanggil dari thread IO. */
    private fun searchFiltered(query: String, filter: String): List<InfoItem> {
        ensureInit()
        val extractor = ServiceList.YouTube.getSearchExtractor(query, listOf(filter), "")
        extractor.fetchPage()
        return extractor.initialPage.items
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

    /** Artis: id-nya adalah id channel (diawali UC), dibuka dengan halaman artis. */
    private fun ChannelInfoItem.toArtist(): LibraryItem? {
        val id = url?.substringAfterLast("/")?.takeIf { it.startsWith("UC") } ?: return null
        val cover = thumbnails.maxByOrNull { it.height }?.url
        return LibraryItem(
            title = name.orEmpty(),
            subtitle = "Artis",
            thumbnail = cover,
            browseId = id,
        )
    }

    /** Album atau playlist: id diambil dari parameter list pada link, lalu dibuka sebagai VL + id. */
    private fun PlaylistInfoItem.toCollection(): LibraryItem? {
        val link = url ?: return null
        val list = Uri.parse(link).getQueryParameter("list")?.takeIf { it.isNotBlank() } ?: return null
        val cover = thumbnails.maxByOrNull { it.height }?.url
        return LibraryItem(
            title = name.orEmpty(),
            subtitle = uploaderName.orEmpty(),
            thumbnail = cover,
            browseId = "VL$list",
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
