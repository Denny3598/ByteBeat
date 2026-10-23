package com.example.applemusicui.data

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Satu kartu kategori di halaman Jelajahi (diambil dari YouTube Music). */
data class ExploreMood(
    val title: String,
    val color: Color,
    val browseId: String,
    val params: String?,
    val coverUrl: String?,
)

/** Mengambil kategori suasana/genre dan cover-nya dari YouTube Music (tanpa login). */
object ExploreRepository {
    private const val API = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val CLIENT_VERSION = "1.20241009.01.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun moods(): List<ExploreMood> = coroutineScope {
        val root = withContext(Dispatchers.IO) { browse("FEmusic_moods_and_genres", null) }
        val buttons = mutableListOf<JSONObject>()
        collect(root, "musicNavigationButtonRenderer", buttons)

        val base = buttons.mapNotNull { b ->
            val title = textOf(b.optJSONObject("buttonText")) ?: return@mapNotNull null
            val endpoint = b.optJSONObject("clickCommand")?.optJSONObject("browseEndpoint")
                ?: return@mapNotNull null
            val browseId = endpoint.optString("browseId").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val params = endpoint.optString("params").takeIf { it.isNotBlank() }
            val stripe = b.optJSONObject("solid")?.optLong("leftStripeColor", 0L) ?: 0L
            val color = if (stripe != 0L) Color(stripe.toInt()) else Color(0xFF6B6B6B)
            ExploreMood(title, color, browseId, params, coverUrl = null)
        }

        // Ambil cover tiap kategori secara paralel.
        base.map { m ->
            async(Dispatchers.IO) { m.copy(coverUrl = coverFor(m.browseId, m.params)) }
        }.awaitAll()
    }

    /** Playlist pertama di kategori dipakai sebagai cover. */
    private fun coverFor(browseId: String, params: String?): String? = try {
        val root = browse(browseId, params)
        val items = mutableListOf<JSONObject>()
        collect(root, "musicTwoRowItemRenderer", items)
        items.firstNotNullOfOrNull { thumbOf(it) }
    } catch (e: Exception) {
        null
    }

    private fun browse(browseId: String, params: String?): JSONObject {
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("hl", "id")
            .put("gl", "ID")
        val body = JSONObject()
            .put("context", JSONObject().put("client", client))
            .put("browseId", browseId)
        if (params != null) body.put("params", params)

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

    private fun textOf(obj: JSONObject?): String? {
        if (obj == null) return null
        obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.optJSONArray("runs") ?: return null
        return (0 until runs.length())
            .joinToString("") { runs.getJSONObject(it).optString("text") }
            .takeIf { it.isNotBlank() }
    }

    private fun thumbOf(renderer: JSONObject): String? {
        val arr = renderer.optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails") ?: return null
        if (arr.length() == 0) return null
        return arr.getJSONObject(arr.length() - 1).optString("url").takeIf { it.isNotBlank() }
    }
}
