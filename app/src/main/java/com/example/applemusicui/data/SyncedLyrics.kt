package com.example.applemusicui.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Satu baris lirik berwaktu: mulai dinyanyikan pada [timeMs]. */
data class LyricLine(val timeMs: Long, val text: String)

/**
 * Lirik berwaktu (per baris) dari LRCLIB (lrclib.net, gratis, tanpa login).
 * Dipakai untuk tampilan lirik yang menyala mengikuti lagu.
 */
object SyncedLyricsRepository {
    private const val BASE = "https://lrclib.net/api"
    // LRCLIB meminta User-Agent yang menyebut aplikasi.
    private const val USER_AGENT = "ByteBeat/1.0 (https://github.com/Denny3598/ByteBeat)"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    /**
     * Lirik berwaktu untuk lagu ini, atau null kalau tidak ada.
     * Coba pencarian langsung dulu (judul + artis + durasi), lalu pencarian bebas.
     */
    suspend fun fetch(title: String, artist: String, durationSec: Long): List<LyricLine>? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = cleanTitle(title)
            val direct = getJson(
                "$BASE/get?track_name=${enc(cleanTitle)}&artist_name=${enc(artist)}" +
                    if (durationSec > 0) "&duration=$durationSec" else "",
            )
            val synced = direct?.optString("syncedLyrics")?.takeIf { it.isNotBlank() }
                ?: searchSynced("$cleanTitle $artist")
            synced?.let { parseLrc(it) }?.takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun searchSynced(query: String): String? {
        val arr = getArray("$BASE/search?q=${enc(query)}") ?: return null
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val synced = item.optString("syncedLyrics").takeIf { it.isNotBlank() }
            if (synced != null) return synced
        }
        return null
    }

    private fun getJson(url: String): JSONObject? {
        val body = get(url) ?: return null
        return JSONObject(body)
    }

    private fun getArray(url: String): JSONArray? {
        val body = get(url) ?: return null
        return JSONArray(body)
    }

    private fun get(url: String): String? {
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            return res.body?.string()
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** Membuang tambahan seperti "(Official Video)" atau "[Lyrics]" dari judul YouTube. */
    private fun cleanTitle(title: String): String =
        title.replace(Regex("""\s*[(\[](official|lyrics?|video|audio|mv|music video|hd|4k)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), "")
            .trim()

    /** Mengubah teks LRC ([mm:ss.xx] baris) menjadi daftar baris berwaktu, sudah terurut. */
    internal fun parseLrc(lrc: String): List<LyricLine> {
        val regex = Regex("""\[(\d+):(\d{2})(?:[.:](\d{1,3}))?]""")
        val out = mutableListOf<LyricLine>()
        for (raw in lrc.lines()) {
            val matches = regex.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val text = raw.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.toLong()
                }
                out += LyricLine(timeMs = (min * 60 + sec) * 1000 + ms, text = text)
            }
        }
        return out.sortedBy { it.timeMs }
    }
}
