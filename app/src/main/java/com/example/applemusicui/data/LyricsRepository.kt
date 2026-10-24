package com.example.applemusicui.data

import android.webkit.CookieManager
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

/**
 * Lirik biasa (tanpa penyorotan per baris) dari YouTube Music.
 * Alurnya: cari tab lirik dari halaman "next" lagu, lalu baca teksnya.
 */
object LyricsRepository {
    private const val NEXT_API = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false"
    private const val BROWSE_API = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val YT_ORIGIN = "https://music.youtube.com"
    private const val CLIENT_VERSION = "1.20241009.01.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Teks lirik, atau null kalau lagu ini tidak punya lirik. */
    suspend fun plain(videoId: String): String? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        val next = post(NEXT_API, "videoId", videoId)
        val lyricsId = findBrowseId(next, "MPLY") ?: return@withContext null
        val root = post(BROWSE_API, "browseId", lyricsId)
        findDescription(root)?.takeIf { it.isNotBlank() }
    }

    private fun post(url: String, key: String, value: String): JSONObject {
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("hl", "id")
            .put("gl", "ID")
        val body = JSONObject()
            .put("context", JSONObject().put("client", client))
            .put(key, value)

        val cookie = CookieManager.getInstance().getCookie(YT_ORIGIN)
        val sapisid = cookie?.let { sapisidOf(it) }
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Origin", YT_ORIGIN)
            .header("Referer", "$YT_ORIGIN/")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", CLIENT_VERSION)
        if (cookie != null && sapisid != null) {
            val ts = System.currentTimeMillis() / 1000
            val hash = MessageDigest.getInstance("SHA-1")
                .digest("$ts $sapisid $YT_ORIGIN".toByteArray())
                .joinToString("") { "%02x".format(it) }
            builder
                .header("Authorization", "SAPISIDHASH ${ts}_$hash")
                .header("Cookie", cookie)
                .header("X-Goog-AuthUser", "0")
        }
        val req = builder.post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("HTTP ${res.code}")
            return JSONObject(res.body?.string().orEmpty())
        }
    }

    private fun sapisidOf(cookie: String): String? {
        for (part in cookie.split(";")) {
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2 && (kv[0] == "SAPISID" || kv[0] == "__Secure-3PAPISID")) return kv[1]
        }
        return null
    }

    /** Browse id pertama yang diawali [prefix], di mana pun letaknya dalam JSON. */
    private fun findBrowseId(node: Any?, prefix: String): String? {
        when (node) {
            is JSONObject -> {
                val id = node.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
                if (id.startsWith(prefix)) return id
                val keys = node.keys()
                while (keys.hasNext()) {
                    findBrowseId(node.get(keys.next()), prefix)?.let { return it }
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                findBrowseId(node.get(i), prefix)?.let { return it }
            }
        }
        return null
    }

    /** Teks di dalam musicDescriptionShelfRenderer, yaitu isi lirik. */
    private fun findDescription(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                node.optJSONObject("musicDescriptionShelfRenderer")?.let { shelf ->
                    val runs = shelf.optJSONObject("description")?.optJSONArray("runs")
                    if (runs != null) {
                        return (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text") }
                    }
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    findDescription(node.get(keys.next()))?.let { return it }
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                findDescription(node.get(i))?.let { return it }
            }
        }
        return null
    }
}
