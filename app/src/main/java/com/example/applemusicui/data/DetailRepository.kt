package com.example.applemusicui.data

import androidx.compose.ui.graphics.Color
import com.example.applemusicui.ui.theme.AppleRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AlbumPage(
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
    val artistName: String,
    val artistBrowseId: String?,
    val songs: List<Song>,
)

data class ArtistPage(
    val name: String,
    val thumbnail: String?,
    val description: String?,
    val topSongs: List<Song>,
    val albums: List<LibraryItem>,
)

/** Halaman album, playlist, dan artis dari YouTube Music. Tidak perlu login. */
object DetailRepository {
    private const val API = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val CLIENT_VERSION = "1.20241009.01.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun album(browseId: String): AlbumPage = withContext(Dispatchers.IO) {
        val root = browse(browseId)
        val header = firstOf(root, "musicResponsiveHeaderRenderer", "musicDetailHeaderRenderer")
        val title = textOf(header?.optJSONObject("title")).orEmpty()
        val subtitleObj = header?.optJSONObject("subtitle")
        val subtitle = textOf(subtitleObj).orEmpty()
        val artist = artistRunIn(subtitleObj)
        val thumb = header?.let { thumbUnder(it) }
        val songs = songRows(root).mapNotNull { songOf(it, artist?.first.orEmpty(), thumb) }
        AlbumPage(
            title = title,
            subtitle = subtitle,
            thumbnail = thumb,
            artistName = artist?.first.orEmpty(),
            artistBrowseId = artist?.second,
            songs = songs,
        )
    }

    suspend fun artist(browseId: String): ArtistPage = withContext(Dispatchers.IO) {
        val root = browse(browseId)
        val header = firstOf(root, "musicImmersiveHeaderRenderer", "musicVisualHeaderRenderer")
        val name = textOf(header?.optJSONObject("title")).orEmpty()
        val description = textOf(header?.optJSONObject("description"))?.takeIf { it.isNotBlank() }
        val thumb = header?.let { thumbUnder(it) }
        val top = songRows(root)
            .mapNotNull { songOf(it, name, null) }
            .distinctBy { it.url }
        val twoRows = mutableListOf<JSONObject>()
        collect(root, "musicTwoRowItemRenderer", twoRows)
        val albums = twoRows.mapNotNull { r ->
            val id = browseIdOf(r)?.takeIf { it.startsWith("MPRE") } ?: return@mapNotNull null
            val t = textOf(r.optJSONObject("title")) ?: return@mapNotNull null
            LibraryItem(t, textOf(r.optJSONObject("subtitle")).orEmpty(), thumbUnder(r), id)
        }.distinctBy { it.browseId }
        ArtistPage(
            name = name,
            thumbnail = thumb,
            description = description,
            topSongs = top,
            albums = albums,
        )
    }

    /** Baris lagu hanya diambil dari shelf utama, supaya rekomendasi di bawah tidak ikut masuk. */
    private fun songRows(root: JSONObject): List<JSONObject> {
        val shelf = firstOf(root, "musicPlaylistShelfRenderer", "musicShelfRenderer") ?: root
        val rows = mutableListOf<JSONObject>()
        collect(shelf, "musicResponsiveListItemRenderer", rows)
        return rows
    }

    private fun songOf(r: JSONObject, artistName: String, thumb: String?): Song? {
        val videoId = r.optJSONObject("playlistItemData")
            ?.optString("videoId")
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val title = flexText(r, 0) ?: return null
        val artist = artistName.ifBlank { flexText(r, 1).orEmpty() }
        return Song(
            title = title,
            artist = artist,
            art = listOf(AppleRed, Color(0xFF333333)),
            url = "https://www.youtube.com/watch?v=$videoId",
            thumbnail = thumb ?: thumbUnder(r),
        )
    }

    private fun browse(browseId: String): JSONObject {
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("hl", "id")
            .put("gl", "ID")
        val body = JSONObject()
            .put("context", JSONObject().put("client", client))
            .put("browseId", browseId)

        val req = Request.Builder()
            .url(API)
            .header("User-Agent", USER_AGENT)
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", CLIENT_VERSION)
            .post(body.toString().toRequestBody(jsonType))
            .build()

        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("HTTP ${res.code}")
            return JSONObject(res.body?.string().orEmpty())
        }
    }

    /** Nama dan browse id artis dari sebuah teks yang punya tautan artis (misalnya subtitle album). */
    private fun artistRunIn(obj: JSONObject?): Pair<String, String>? {
        val runs = obj?.optJSONArray("runs") ?: return null
        for (i in 0 until runs.length()) {
            val run = runs.optJSONObject(i) ?: continue
            val id: String = run.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId")
                ?: ""
            if (id.startsWith("UC")) {
                val name = run.optString("text")
                if (name.isNotBlank()) return name to id
            }
        }
        return null
    }

    private fun browseIdOf(r: JSONObject): String? = r.optJSONObject("navigationEndpoint")
        ?.optJSONObject("browseEndpoint")
        ?.optString("browseId")
        ?.takeIf { it.isNotBlank() }

    private fun flexText(r: JSONObject, index: Int): String? {
        val cols = r.optJSONArray("flexColumns") ?: return null
        if (index >= cols.length()) return null
        val text = cols.getJSONObject(index)
            .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text") ?: return null
        return textOf(text)
    }

    /** Objek pertama, di mana pun letaknya, yang memakai salah satu kunci. */
    private fun firstOf(node: Any?, vararg keys: String): JSONObject? {
        when (node) {
            is JSONObject -> {
                for (key in keys) {
                    node.optJSONObject(key)?.let { return it }
                }
                val names = node.keys()
                while (names.hasNext()) {
                    firstOf(node.get(names.next()), *keys)?.let { return it }
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                firstOf(node.get(i), *keys)?.let { return it }
            }
        }
        return null
    }

    /** Mengumpulkan semua objek dengan kunci tertentu, di mana pun letaknya dalam JSON. */
    private fun collect(node: Any?, key: String, out: MutableList<JSONObject>) {
        when (node) {
            is JSONObject -> {
                node.optJSONObject(key)?.let { out.add(it) }
                val names = node.keys()
                while (names.hasNext()) {
                    collect(node.get(names.next()), key, out)
                }
            }
            is JSONArray -> for (i in 0 until node.length()) collect(node.get(i), key, out)
        }
    }

    /** Cover pertama yang ditemukan di dalam sebuah objek, dengan ukuran HD. */
    private fun thumbUnder(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                node.optJSONArray("thumbnails")?.let { arr ->
                    if (arr.length() > 0) {
                        val url = arr.getJSONObject(arr.length() - 1).optString("url")
                        if (url.isNotBlank()) return hdThumb(url)
                    }
                }
                val names = node.keys()
                while (names.hasNext()) {
                    thumbUnder(node.get(names.next()))?.let { return it }
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                thumbUnder(node.get(i))?.let { return it }
            }
        }
        return null
    }

    private fun textOf(obj: JSONObject?): String? {
        if (obj == null) return null
        obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.optJSONArray("runs") ?: return null
        return (0 until runs.length())
            .joinToString("") { runs.getJSONObject(it).optString("text") }
            .takeIf { it.isNotBlank() }
    }
}
