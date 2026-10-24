package com.example.applemusicui.data

import android.webkit.CookieManager
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Satu item di beranda atau pustaka: bisa berupa lagu, playlist, atau album. */
data class ShelfItem(
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
    val videoId: String? = null,
    val browseId: String? = null,
)

data class Shelf(val title: String, val items: List<ShelfItem>)

/**
 * Klien YouTube Music (InnerTube) untuk beranda dan pustaka.
 * Kalau sudah login, permintaan ditandatangani dengan SAPISIDHASH dari cookie WebView.
 */
object InnerTubeClient {
    private const val ORIGIN = "https://music.youtube.com"
    private const val CLIENT_VERSION = "1.20250101.01.00"
    private const val CLIENT_ID = "67"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Cookie login dari WebView, atau null kalau belum login. */
    fun sessionCookie(): String? {
        val raw = try {
            CookieManager.getInstance().getCookie(ORIGIN)
        } catch (e: Exception) {
            null
        }
        return raw?.takeIf { it.isNotBlank() }
    }

    fun isLoggedIn(): Boolean = sapisid(sessionCookie()) != null

    private fun sapisid(cookie: String?): String? {
        if (cookie == null) return null
        val jar = cookie.split(';').mapNotNull { entry ->
            val name = entry.substringBefore('=').trim()
            val value = entry.substringAfter('=', "").trim()
            if (value.isEmpty()) null else name to value
        }.toMap()
        return jar["SAPISID"] ?: jar["__Secure-3PAPISID"] ?: jar["__Secure-1PAPISID"]
    }

    private fun sapisidHash(secret: String): String {
        val ts = System.currentTimeMillis() / 1000
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$ts $secret $ORIGIN".toByteArray())
            .joinToString("") { "%02x".format(Locale.ROOT, it) }
        return "SAPISIDHASH ${ts}_$digest"
    }

    /** Panggilan browse ke YouTube Music. */
    suspend fun browse(browseId: String): JSONObject =
        call("browse", JSONObject().put("browseId", browseId))

    /** Panggilan next: dipakai untuk radio dan tautan album/artis lagu. */
    suspend fun next(videoId: String, playlistId: String? = null): JSONObject {
        val extra = JSONObject().put("videoId", videoId)
        playlistId?.let { extra.put("playlistId", it) }
        return call("next", extra)
    }

    /** Panggilan ke endpoint InnerTube. [extra] berisi field tambahan untuk body. */
    private suspend fun call(endpoint: String, extra: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val cookie = sessionCookie()
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "WEB_REMIX")
                        .put("clientVersion", CLIENT_VERSION)
                        .put("hl", "id")
                        .put("gl", "ID"),
                ),
            )
        extra.keys().forEach { body.put(it, extra.get(it)) }

        val builder = Request.Builder()
            .url("$ORIGIN/youtubei/v1/$endpoint?prettyPrint=false")
            .post(body.toString().toRequestBody(JSON_TYPE))
            .header("User-Agent", USER_AGENT)
            .header("Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .header("X-Origin", ORIGIN)
            .header("X-YouTube-Client-Name", CLIENT_ID)
            .header("X-YouTube-Client-Version", CLIENT_VERSION)
            .header("Accept-Language", "id-ID,id;q=0.9,en;q=0.8")

        if (cookie != null) {
            builder.header("Cookie", cookie)
            builder.header("X-Goog-AuthUser", "0")
            sapisid(cookie)?.let { builder.header("Authorization", sapisidHash(it)) }
        }

        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("YouTube Music menjawab ${response.code}")
            JSONObject(text)
        }
    }

    /** Beranda: daftar shelf seperti "Dengarkan lagi" dan "Rekomendasi". */
    suspend fun home(): List<Shelf> = parseShelves(browse("FEmusic_home"))

    /** Pustaka akun: playlist yang disimpan. Butuh login. */
    suspend fun libraryPlaylists(): List<Shelf> =
        parseShelves(browse("FEmusic_liked_playlists"))

    /** Daftar lagu di playlist atau album. */
    suspend fun tracksOf(browseId: String): List<ShelfItem> {
        val root = browse(browseId)
        val rows = (findObject(root, "musicPlaylistShelfRenderer")
            ?: findObject(root, "musicShelfRenderer"))
            ?.optJSONArray("contents")
            ?: return emptyList()
        return (0 until rows.length()).mapNotNull { i ->
            rows.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemRenderer")
                ?.let { parseListItem(it) }
        }.filter { it.videoId != null }
    }

    private fun parseShelves(root: JSONObject): List<Shelf> {
        val sections = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?: return emptyList()

        val shelves = mutableListOf<Shelf>()
        for (i in 0 until sections.length()) {
            val section = sections.optJSONObject(i) ?: continue
            val carousel = section.optJSONObject("musicCarouselShelfRenderer")
            val shelfObj = carousel ?: section.optJSONObject("musicShelfRenderer") ?: continue
            val title = if (carousel != null) {
                runsText(
                    carousel.optJSONObject("header")
                        ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                        ?.optJSONObject("title"),
                )
            } else {
                runsText(shelfObj.optJSONObject("title"))
            }.orEmpty()
            val contents = shelfObj.optJSONArray("contents") ?: continue
            val items = (0 until contents.length()).mapNotNull { j ->
                contents.optJSONObject(j)?.let { parseItem(it) }
            }
            if (items.isNotEmpty()) shelves += Shelf(title, items)
        }
        return shelves
    }

    private fun parseItem(obj: JSONObject): ShelfItem? =
        obj.optJSONObject("musicTwoRowItemRenderer")?.let { parseTwoRow(it) }
            ?: obj.optJSONObject("musicResponsiveListItemRenderer")?.let { parseListItem(it) }

    private fun parseTwoRow(r: JSONObject): ShelfItem? {
        val title = runsText(r.optJSONObject("title")) ?: return null
        val subtitle = runsText(r.optJSONObject("subtitle")).orEmpty()
        val nav = r.optJSONObject("navigationEndpoint")
        return ShelfItem(
            title = title,
            subtitle = subtitle,
            thumbnail = thumbnailOf(r),
            videoId = nav?.optJSONObject("watchEndpoint")?.optString("videoId")?.ifEmpty { null },
            browseId = nav?.optJSONObject("browseEndpoint")?.optString("browseId")?.ifEmpty { null },
        )
    }

    private fun parseListItem(r: JSONObject): ShelfItem? {
        val cols = r.optJSONArray("flexColumns") ?: return null
        val title = flexText(cols, 0) ?: return null
        val subtitle = flexText(cols, 1).orEmpty()
        val videoId = r.optJSONObject("playlistItemData")?.optString("videoId")?.ifEmpty { null }
            ?: r.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.ifEmpty { null }
        return ShelfItem(
            title = title,
            subtitle = subtitle,
            thumbnail = thumbnailOf(r),
            videoId = videoId,
        )
    }

    private fun flexText(cols: JSONArray, index: Int): String? =
        cols.optJSONObject(index)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.let { runsText(it.optJSONObject("text")) }

    private fun runsText(obj: JSONObject?): String? {
        if (obj == null) return null
        val runs = obj.optJSONArray("runs") ?: return obj.optString("simpleText").ifEmpty { null }
        val text = buildString {
            for (i in 0 until runs.length()) {
                append(runs.optJSONObject(i)?.optString("text").orEmpty())
            }
        }
        return text.ifEmpty { null }
    }

    private fun thumbnailOf(r: JSONObject): String? {
        val container = r.optJSONObject("thumbnailRenderer") ?: r.optJSONObject("thumbnail")
        val thumbs = container?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: return null
        return thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")?.ifEmpty { null }
    }

    /** Mencari objek pertama dengan [key] di seluruh pohon JSON. */
    private fun findObject(node: Any?, key: String): JSONObject? {
        when (node) {
            is JSONObject -> {
                node.optJSONObject(key)?.let { return it }
                val keys = node.keys()
                while (keys.hasNext()) {
                    findObject(node.opt(keys.next()), key)?.let { return it }
                }
                return null
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    findObject(node.opt(i), key)?.let { return it }
                }
                return null
            }
            else -> return null
        }
    }
}

private val ART_PALETTE = listOf(
    Color(0xFF7B6CF6),
    Color(0xFFE040FB),
    Color(0xFFFF6A88),
    Color(0xFF5AC8FA),
    Color(0xFFFF9F0A),
    Color(0xFF30D158),
)

/** Warna sementara untuk cover yang belum dimuat. */
fun artFor(seed: String): List<Color> {
    val i = (seed.hashCode() and 0x7fffffff) % ART_PALETTE.size
    return listOf(ART_PALETTE[i], ART_PALETTE[(i + 1) % ART_PALETTE.size])
}

fun ShelfItem.toSong(): Song? {
    val id = videoId ?: return null
    return Song(
        title = title,
        artist = subtitle,
        art = artFor(title),
        url = "https://www.youtube.com/watch?v=$id",
        thumbnail = thumbnail,
    )
}

/**
 * Menentukan lagu yang diputar dan antreannya.
 * Lagu biasa: antreannya adalah sesama item di shelf yang punya videoId.
 * Playlist atau album: antreannya adalah seluruh lagu di dalamnya.
 */
suspend fun ShelfItem.resolve(siblings: List<ShelfItem>): Pair<Song, List<Song>>? {
    if (videoId != null) {
        val song = toSong() ?: return null
        return song to siblings.mapNotNull { it.toSong() }
    }
    val id = browseId ?: return null
    val tracks = InnerTubeClient.tracksOf(id).mapNotNull { it.toSong() }
    return tracks.firstOrNull()?.let { it to tracks }
}
