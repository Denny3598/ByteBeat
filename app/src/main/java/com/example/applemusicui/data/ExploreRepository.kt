package com.example.applemusicui.data

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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

/** Satu rak di dalam kategori, misalnya "Hits Indonesia" atau daftar playlist. */
data class CategorySection(val title: String, val items: List<HomeCardItem>)

private data class MoodButton(
    val title: String,
    val browseId: String,
    val params: String?,
    val stripeColor: Long?,
)

private data class MoodSection(val title: String, val items: List<MoodButton>)

/** Mengambil kategori suasana/genre dan cover-nya dari YouTube Music (tanpa login). */
object ExploreRepository {
    private const val API = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val CLIENT_VERSION = "1.20241009.01.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36"

    // callTimeout = batas total satu permintaan, supaya tidak pernah menggantung.
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()
        .also {
            it.dispatcher.maxRequests = 32
            it.dispatcher.maxRequestsPerHost = 16
        }
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /**
     * Daftar kategori saja, tanpa menunggu cover. Cover diambil per kartu lewat [coverFor],
     * sehingga layar langsung tampil. Melempar exception kalau halaman utama gagal dimuat.
     */
    suspend fun moods(): List<ExploreMood> = withContext(Dispatchers.IO) {
        val root = browse("FEmusic_moods_and_genres", null)
        parseButtons(root).map { m ->
            ExploreMood(
                title = m.title,
                color = colorOf(m.stripeColor, m.title),
                browseId = m.browseId,
                params = m.params,
                coverUrl = null,
            )
        }
    }

    /**
     * Sama seperti parser BitChord: section dibaca dari gridRenderer, tombol pintasan yang
     * muncul lagi di section lain dibuang, dan section "For you" dibuang.
     */
    private fun parseButtons(root: JSONObject): List<MoodButton> {
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")

        val sections = mutableListOf<MoodSection>()
        if (contents != null) {
            for (i in 0 until contents.length()) {
                val grid = contents.optJSONObject(i)?.optJSONObject("gridRenderer") ?: continue
                val title = textOf(
                    grid.optJSONObject("header")
                        ?.optJSONObject("gridHeaderRenderer")
                        ?.optJSONObject("title"),
                ).orEmpty()
                val items = grid.optJSONArray("items") ?: continue
                val buttons = mutableListOf<MoodButton>()
                for (j in 0 until items.length()) {
                    val button = items.optJSONObject(j)?.optJSONObject("musicNavigationButtonRenderer")
                        ?: continue
                    buttonOf(button)?.let { buttons += it }
                }
                if (title.isNotBlank() && buttons.isNotEmpty()) sections += MoodSection(title, buttons)
            }
        }

        val kept = sections.filterIndexed { index, section ->
            val later = sections.drop(index + 1)
                .flatMapTo(HashSet()) { s -> s.items.map { it.browseId to it.params } }
            val shortcut = section.items.any { (it.browseId to it.params) in later }
            !shortcut && !section.title.equals("For you", ignoreCase = true)
        }
        if (kept.isNotEmpty()) {
            return kept.flatMap { it.items }.distinctBy { it.browseId to it.params }
        }

        // Cadangan kalau struktur section berubah: ambil semua tombol, lalu buang yang kembar.
        val raw = mutableListOf<JSONObject>()
        collect(root, "musicNavigationButtonRenderer", raw)
        return raw.mapNotNull { buttonOf(it) }.distinctBy { it.browseId to it.params }
    }

    private fun buttonOf(button: JSONObject): MoodButton? {
        val endpoint = button.optJSONObject("clickCommand")?.optJSONObject("browseEndpoint")
            ?: button.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            ?: return null
        val browseId = endpoint.optString("browseId").takeIf { it.isNotBlank() } ?: return null
        val title = textOf(button.optJSONObject("buttonText"))?.takeIf { it.isNotBlank() } ?: return null
        val params = endpoint.optString("params").takeIf { it.isNotBlank() }
        val stripe = button.optJSONObject("solid")
            ?.optLong("leftStripeColor", 0L)
            ?.takeIf { it != 0L }
        return MoodButton(title, browseId, params, stripe)
    }

    /** Warna strip dari YouTube Music; kalau tidak ada, pakai warna cadangan yang kalem. */
    private fun colorOf(stripe: Long?, title: String): Color {
        if (stripe != null) return Color(stripe.toInt())
        return when ((title.hashCode() and Int.MAX_VALUE) % 8) {
            0 -> Color(0xFFCC6A55)
            1 -> Color(0xFFC07A92)
            2 -> Color(0xFF9C8AC0)
            3 -> Color(0xFF8090C8)
            4 -> Color(0xFFD0A060)
            5 -> Color(0xFF6A88B0)
            6 -> Color(0xFF7AAED0)
            else -> Color(0xFF86B890)
        }
    }

    /**
     * Cover satu kategori: playlist pertama di kategori dipakai sebagai cover.
     * Dibatasi 3 permintaan sekaligus, supaya respons JSON besar tidak menumpuk di memori.
     */
    suspend fun coverFor(browseId: String, params: String?): String? = coverLimit.withPermit {
        withContext(Dispatchers.IO) {
            try {
                val root = browse(browseId, params)
                val items = mutableListOf<JSONObject>()
                collect(root, "musicTwoRowItemRenderer", items)
                items.firstNotNullOfOrNull { thumbOf(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Isi satu kategori: daftar rak (playlist, album, atau lagu) yang bisa dibuka satu per satu. */
    suspend fun category(browseId: String, params: String?): List<CategorySection> = withContext(Dispatchers.IO) {
        val root = browse(browseId, params)
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")

        val sections = mutableListOf<CategorySection>()
        if (contents != null) {
            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i)?.let { sectionOf(it) } ?: continue
                sections += section
            }
        }
        if (sections.isEmpty()) {
            // Cadangan kalau struktur berubah: kumpulkan semua kartu dalam satu rak tanpa judul.
            val raw = mutableListOf<JSONObject>()
            collect(root, "musicTwoRowItemRenderer", raw)
            val list = raw.mapNotNull { itemOf(it) }.distinctBy { it.videoId ?: it.browseId }
            if (list.isNotEmpty()) sections += CategorySection("", list)
        }
        sections
    }

    private fun sectionOf(s: JSONObject): CategorySection? {
        s.optJSONObject("gridRenderer")?.let { grid ->
            val title = textOf(
                grid.optJSONObject("header")?.optJSONObject("gridHeaderRenderer")?.optJSONObject("title"),
            ).orEmpty()
            val items = grid.optJSONArray("items") ?: return null
            val list = (0 until items.length()).mapNotNull { j ->
                items.optJSONObject(j)?.optJSONObject("musicTwoRowItemRenderer")?.let { itemOf(it) }
            }
            return if (list.isEmpty()) null else CategorySection(title, list.distinctBy { it.videoId ?: it.browseId })
        }
        s.optJSONObject("musicCarouselShelfRenderer")?.let { shelf ->
            val header = shelf.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            val title = textOf(header?.optJSONObject("title")).orEmpty()
            val contents = shelf.optJSONArray("contents") ?: return null
            val list = (0 until contents.length()).mapNotNull { j ->
                contents.optJSONObject(j)?.optJSONObject("musicTwoRowItemRenderer")?.let { itemOf(it) }
            }
            return if (list.isEmpty()) null else CategorySection(title, list.distinctBy { it.videoId ?: it.browseId })
        }
        s.optJSONObject("musicShelfRenderer")?.let { shelf ->
            val title = textOf(shelf.optJSONObject("title")).orEmpty()
            val contents = shelf.optJSONArray("contents") ?: return null
            val list = (0 until contents.length()).mapNotNull { j ->
                contents.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer")?.let { responsiveOf(it) }
            }
            return if (list.isEmpty()) null else CategorySection(title, list.distinctBy { it.videoId ?: it.browseId })
        }
        return null
    }

    /** Kartu dua baris: playlist/album (browseId) atau lagu (videoId). */
    private fun itemOf(r: JSONObject): HomeCardItem? {
        val title = textOf(r.optJSONObject("title")) ?: return null
        val nav = r.optJSONObject("navigationEndpoint")
        val videoId = nav?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
            ?: r.optJSONObject("thumbnailOverlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.takeIf { it.isNotBlank() }
        val browseId = nav?.optJSONObject("browseEndpoint")?.optString("browseId")?.takeIf { it.isNotBlank() }
        if (videoId == null && browseId == null) return null
        return HomeCardItem(
            title = title,
            subtitle = textOf(r.optJSONObject("subtitle")).orEmpty(),
            thumbnail = thumbOf(r),
            videoId = videoId,
            browseId = browseId,
        )
    }

    /** Baris lagu gaya daftar di dalam kategori. */
    private fun responsiveOf(r: JSONObject): HomeCardItem? {
        val cols = r.optJSONArray("flexColumns") ?: return null
        val title = cols.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.let { textOf(it.optJSONObject("text")) } ?: return null
        val subtitle = cols.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.let { textOf(it.optJSONObject("text")) }.orEmpty()
        val videoId = r.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }
        val browseId = r.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")?.optString("browseId")?.takeIf { it.isNotBlank() }
        if (videoId == null && browseId == null) return null
        return HomeCardItem(title, subtitle, thumbOf(r), videoId, browseId)
    }

    private val coverLimit = Semaphore(3)

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
            ?.optJSONArray("thumbnails")
            ?: renderer.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            ?: return null
        if (arr.length() == 0) return null
        val url = arr.getJSONObject(arr.length() - 1).optString("url").takeIf { it.isNotBlank() }
            ?: return null
        // Ukuran sedang saja: puluhan cover 1200px sekaligus menghabiskan memori (OutOfMemoryError).
        return url
            .replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
            .replace(Regex("=s\\d+"), "=s544")
    }
}
