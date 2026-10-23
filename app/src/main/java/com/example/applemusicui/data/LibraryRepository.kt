package com.example.applemusicui.data

import android.webkit.CookieManager
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
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class LibraryItem(
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
    val browseId: String,
)

/** Pustaka akun YouTube Music yang sedang login (memakai cookie dari WebView login). */
object LibraryRepository {
    private const val API = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val CLIENT_VERSION = "1.20241009.01.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Playlist yang disimpan di akun. */
    suspend fun playlists(): List<LibraryItem> = withContext(Dispatchers.IO) {
        val root = browse("FEmusic_liked_playlists")
        val found = mutableListOf<JSONObject>()
        collect(root, "musicTwoRowItemRenderer", found)
        found.mapNotNull { r ->
            val title = textOf(r.optJSONObject("title")) ?: return@mapNotNull null
            val browseId = r.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId")
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            LibraryItem(
                title = title,
                subtitle = textOf(r.optJSONObject("subtitle")).orEmpty(),
                thumbnail = thumbOf(r),
                browseId = browseId,
            )
        }
    }

    /** Lagu di dalam playlist (atau di "FEmusic_liked_videos"). */
    suspend fun tracks(browseId: String): List<Song> = withContext(Dispatchers.IO) {
        val root = browse(browseId)
        val found = mutableListOf<JSONObject>()
        collect(root, "musicResponsiveListItemRenderer", found)
        found.mapNotNull { r ->
            val videoId = r.optJSONObject("playlistItemData")
                ?.optString("videoId")
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val title = flexText(r, 0, firstOnly = false) ?: return@mapNotNull null
            Song(
                title = title,
                artist = flexText(r, 1, firstOnly = true).orEmpty(),
                art = listOf(AppleRed, Color(0xFF333333)),
                url = "https://www.youtube.com/watch?v=$videoId",
                thumbnail = thumbOf(r),
            )
        }
    }

    private fun browse(browseId: String): JSONObject {
        val cookie = CookieManager.getInstance().getCookie(AppPrefs.YT_URL)
            ?: error("Belum login")
        val sapisid = sapisid(cookie) ?: error("Cookie login tidak lengkap")

        // SAPISIDHASH: SHA-1 dari "timestamp sapisid origin"
        val ts = System.currentTimeMillis() / 1000
        val hash = MessageDigest.getInstance("SHA-1")
            .digest("$ts $sapisid ${AppPrefs.YT_URL}".toByteArray())
            .joinToString("") { "%02x".format(it) }

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
            .header("Origin", AppPrefs.YT_URL)
            .header("Referer", "${AppPrefs.YT_URL}/")
            .header("Authorization", "SAPISIDHASH ${ts}_$hash")
            .header("Cookie", cookie)
            .header("X-Goog-AuthUser", "0")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", CLIENT_VERSION)
            .post(body.toString().toRequestBody(jsonType))
            .build()

        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("HTTP ${res.code}")
            return JSONObject(res.body?.string().orEmpty())
        }
    }

    private fun sapisid(cookie: String): String? {
        for (part in cookie.split(";")) {
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2 && (kv[0] == "SAPISID" || kv[0] == "__Secure-3PAPISID")) return kv[1]
        }
        return null
    }

    /** Mengumpulkan semua objek dengan kunci tertentu, di mana pun letaknya dalam JSON. */
    private fun collect(node: Any?, key: String, out: MutableList<JSONObject>) {
        when (node) {
            is JSONObject -> {
                node.optJSONObject(key)?.let { out.add(it) }
                val keys = node.keys()
                while (keys.hasNext()) {
                    collect(node.get(keys.next()), key, out)
                }
            }
            is JSONArray -> for (i in 0 until node.length()) collect(node.get(i), key, out)
        }
    }

    private fun flexText(r: JSONObject, index: Int, firstOnly: Boolean): String? {
        val cols = r.optJSONArray("flexColumns") ?: return null
        if (index >= cols.length()) return null
        val text = cols.getJSONObject(index)
            .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text") ?: return null
        return textOf(text, firstOnly)
    }

    private fun textOf(obj: JSONObject?, firstOnly: Boolean = false): String? {
        if (obj == null) return null
        obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.optJSONArray("runs") ?: return null
        val parts = (0 until runs.length()).map { runs.getJSONObject(it).optString("text") }
        val chosen = if (firstOnly) parts.take(1) else parts
        return chosen.joinToString("").takeIf { it.isNotBlank() }
    }

    private fun thumbOf(r: JSONObject): String? {
        val arr = r.optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: r.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            ?: return null
        if (arr.length() == 0) return null
        return arr.getJSONObject(arr.length() - 1).optString("url").takeIf { it.isNotBlank() }
    }
}
