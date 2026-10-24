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

/** Mengganti ukuran thumbnail YouTube dengan versi resolusi tinggi supaya tajam di layar HP. */
fun hdThumb(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return url
        .replace(Regex("=w\\d+-h\\d+"), "=w1200-h1200")
        .replace(Regex("=s\\d+"), "=s1200")
}

/** Cover untuk kartu daftar di beranda: cukup tajam, tapi tidak memenuhi memori seperti 1200px. */
fun cardThumb(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return url
        .replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
        .replace(Regex("=s\\d+"), "=s544")
}

/** Satu kartu di beranda: lagu (punya videoId) atau album/playlist (punya browseId). */
data class HomeCardItem(
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
    val videoId: String?,
    val browseId: String?,
)

/** Satu rak beranda, misalnya "Dengarkan lagi" beserta subjudulnya. */
data class HomeShelf(
    val title: String,
    val items: List<HomeCardItem>,
    val subtitle: String = "",
)

/** Satu halaman beranda, beserta token untuk memuat rak berikutnya (null kalau sudah habis). */
data class HomePage(
    val shelves: List<HomeShelf>,
    val continuation: String?,
)

/** Beranda YouTube Music. Memakai login bila ada, supaya rekomendasinya milik akun. */
object HomeRepository {
    private const val API = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val YT_ORIGIN = "https://music.youtube.com"
    private const val CLIENT_VERSION = "1.20241009.01.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Halaman pertama beranda: rak-rak pertama plus token untuk halaman berikutnya. */
    suspend fun firstPage(): HomePage = withContext(Dispatchers.IO) {
        val root = browse("FEmusic_home", null)
        val list = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")

        val parsed = parseSections(list?.optJSONArray("contents"))
        var shelves = parsed.first
        var token = parsed.second
            ?: list?.optJSONArray("continuations")?.optJSONObject(0)
                ?.optJSONObject("nextContinuationData")?.optString("continuation")?.takeIf { it.isNotBlank() }

        // Kalau struktur section berubah, cadangannya: cari semua rak kartu di mana pun dalam JSON.
        if (shelves.isEmpty()) {
            val carousels = mutableListOf<JSONObject>()
            collect(root, "musicCarouselShelfRenderer", carousels)
            shelves = carousels.mapNotNull { carouselShelf(it) }
        }
        HomePage(shelves, token)
    }

    /** Memuat rak berikutnya dengan token dari [firstPage] atau halaman sebelumnya. */
    suspend fun nextPage(token: String): HomePage = withContext(Dispatchers.IO) {
        val root = browse(null, token)
        val section = root.optJSONObject("continuationContents")
            ?.optJSONObject("sectionListContinuation")
        val parsed = parseSections(section?.optJSONArray("contents"))
        val next = parsed.second
            ?: section?.optJSONArray("continuations")?.optJSONObject(0)
                ?.optJSONObject("nextContinuationData")?.optString("continuation")?.takeIf { it.isNotBlank() }
        HomePage(parsed.first, next)
    }

    /** Membaca daftar section: rak kartu, rak banner, rak daftar, dan token lanjutan kalau ada. */
    private fun parseSections(sections: JSONArray?): Pair<List<HomeShelf>, String?> {
        val shelves = mutableListOf<HomeShelf>()
        var token: String? = null
        if (sections == null) return shelves to null
        for (i in 0 until sections.length()) {
            val section = sections.optJSONObject(i) ?: continue
            val shelf = section.optJSONObject("musicCarouselShelfRenderer")?.let { carouselShelf(it) }
                ?: section.optJSONObject("musicImmersiveCarouselShelfRenderer")?.let { carouselShelf(it) }
                ?: section.optJSONObject("musicShelfRenderer")?.let { plainShelf(it) }
            if (shelf != null) shelves += shelf

            val cont = section.optJSONObject("continuationItemRenderer")
                ?.optJSONObject("continuationEndpoint")
                ?.optJSONObject("continuationCommand")
                ?.optString("token")
                ?.takeIf { it.isNotBlank() }
            if (cont != null) token = cont
        }
        return shelves to token
    }

    /** Rak kartu (biasa atau banner): judul dan subjudul besar, lalu item berupa kartu. */
    private fun carouselShelf(carousel: JSONObject): HomeShelf? {
        val headerRoot = carousel.optJSONObject("header")
        val header = headerRoot?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            ?: headerRoot?.optJSONObject("musicImmersiveCarouselShelfBasicHeaderRenderer")
        val title = textOf(header?.optJSONObject("title")) ?: return null
        val subtitle = textOf(header?.optJSONObject("strapline")).orEmpty()
        val contents = carousel.optJSONArray("contents") ?: return null
        val items = mutableListOf<HomeCardItem>()
        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i) ?: continue
            item.optJSONObject("musicTwoRowItemRenderer")?.let { parseItem(it) }?.let { items += it }
            item.optJSONObject("musicResponsiveListItemRenderer")?.let { parseResponsive(it) }?.let { items += it }
        }
        return if (items.isEmpty()) null else HomeShelf(title, items.distinctBy { it.videoId ?: it.browseId }, subtitle)
    }

    /** Rak daftar: baris lagu bergaya list, misalnya "Recents". */
    private fun plainShelf(shelf: JSONObject): HomeShelf? {
        val title = textOf(shelf.optJSONObject("title")).orEmpty().ifBlank { "Untuk kamu" }
        val contents = shelf.optJSONArray("contents") ?: return null
        val items = mutableListOf<HomeCardItem>()
        for (i in 0 until contents.length()) {
            val row = contents.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
            parseResponsive(row)?.let { items += it }
        }
        return if (items.isEmpty()) null else HomeShelf(title, items.distinctBy { it.videoId ?: it.browseId })
    }

    private fun parseItem(r: JSONObject): HomeCardItem? {
        val title = textOf(r.optJSONObject("title")) ?: return null
        val nav = r.optJSONObject("navigationEndpoint")
        val overlayVideo = (r.optJSONObject("thumbnailOverlay") ?: r.optJSONObject("overlay"))
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("playNavigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?.takeIf { it.isNotBlank() }
        val videoId = nav?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?.takeIf { it.isNotBlank() }
            ?: overlayVideo
        val browseId = nav?.optJSONObject("browseEndpoint")
            ?.optString("browseId")
            ?.takeIf { it.isNotBlank() }
        if (videoId == null && browseId == null) return null
        return HomeCardItem(
            title = title,
            subtitle = textOf(r.optJSONObject("subtitle")).orEmpty(),
            thumbnail = thumbOf(r),
            videoId = videoId,
            browseId = browseId,
        )
    }

    /** Baris lagu gaya daftar (dipakai rak "Recents"). */
    private fun parseResponsive(r: JSONObject): HomeCardItem? {
        val title = flexText(r, 0) ?: return null
        val videoId = r.optJSONObject("playlistItemData")
            ?.optString("videoId")
            ?.takeIf { it.isNotBlank() }
        val browseId = r.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
            ?.optString("browseId")
            ?.takeIf { it.isNotBlank() }
        if (videoId == null && browseId == null) return null
        return HomeCardItem(
            title = title,
            subtitle = flexText(r, 1).orEmpty(),
            thumbnail = thumbOf(r),
            videoId = videoId,
            browseId = browseId,
        )
    }

    private fun flexText(r: JSONObject, index: Int): String? {
        val cols = r.optJSONArray("flexColumns") ?: return null
        if (index >= cols.length()) return null
        val text = cols.getJSONObject(index)
            .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text") ?: return null
        return textOf(text)
    }

    /** Memanggil browse. Isi [browseId] untuk halaman awal, atau [continuation] untuk halaman berikutnya. */
    private fun browse(browseId: String?, continuation: String?): JSONObject {
        val cookie = CookieManager.getInstance().getCookie(YT_ORIGIN)
        val sapisid = cookie?.let { sapisid(it) }

        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("hl", "id")
            .put("gl", "ID")
        val body = JSONObject()
            .put("context", JSONObject().put("client", client))
        if (browseId != null) body.put("browseId", browseId)
        if (continuation != null) body.put("continuation", continuation)

        val builder = Request.Builder()
            .url(API)
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

    private fun textOf(obj: JSONObject?): String? {
        if (obj == null) return null
        obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.optJSONArray("runs") ?: return null
        return (0 until runs.length())
            .joinToString("") { runs.getJSONObject(it).optString("text") }
            .takeIf { it.isNotBlank() }
    }

    private fun thumbOf(r: JSONObject): String? = cardThumb(rawThumb(r))

    private fun rawThumb(r: JSONObject): String? {
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
