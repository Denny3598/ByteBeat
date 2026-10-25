/*
 * Diadaptasi dari BitChord (https://github.com/kushagrasinghx/BitChord), lisensi GPL v3.
 * Lihat berkas LICENSE di root proyek.
 * Asal: shared/.../data/lyrics/LrcLib.kt dan LyricGaps.kt.
 */
package com.example.applemusicui.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Satu baris lirik berwaktu: mulai dinyanyikan pada [timeMs].
 * [text] kosong berarti jeda instrumental.
 */
data class LyricLine(val timeMs: Long, val text: String) {
    val isGap: Boolean get() = text.isEmpty()
}

/**
 * Lirik berwaktu per baris dari LRCLIB (lrclib.net, gratis, tanpa login).
 * LRCLIB tidak punya waktu per kata, jadi sorotan dilakukan per baris.
 */
object SyncedLyricsRepository {

    private const val BASE = "https://lrclib.net/api"

    // LRCLIB meminta User-Agent yang menyebut aplikasi.
    private const val USER_AGENT = "ByteBeat/1.0 (https://github.com/Denny3598/ByteBeat)"

    /** Jeda instrumental hanya ditampilkan kalau cukup panjang, supaya tidak berkedip di antara dua frasa. */
    internal const val MIN_GAP_MS = 4_000L

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    /**
     * Lirik berwaktu untuk lagu ini, atau null kalau tidak ada.
     * Coba pencarian langsung (judul + artis + durasi) dulu, lalu pencarian bebas
     * yang memilih hasil dengan durasi paling dekat.
     */
    suspend fun fetch(title: String, artist: String, durationSec: Long): List<LyricLine>? =
        withContext(Dispatchers.IO) {
            try {
                val cleanTitle = cleanTitle(title)
                val cleanArtist = cleanTitle(artist)
                val seconds = durationSec.toInt()
                val synced = exactMatch(cleanTitle, cleanArtist, seconds)
                    ?: bestSearchHit(cleanTitle, cleanArtist, seconds)
                synced?.let { parseLrc(it) }?.takeIf { it.isNotEmpty() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }

    private fun exactMatch(title: String, artist: String, seconds: Int): String? {
        val url = "$BASE/get".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .addQueryParameter("artist_name", artist)
            .addQueryParameter("duration", seconds.toString())
            .build()
        val body = get(url.toString()) ?: return null
        return JSONObject(body).lyricsOrNull("syncedLyrics")
    }

    /**
     * Pencarian bebas. Memilih hasil dengan durasi paling dekat ke lagu yang sedang diputar,
     * supaya edisi lain dari lagu yang sama tidak ikut terpilih.
     */
    private fun bestSearchHit(title: String, artist: String, seconds: Int): String? {
        val url = "$BASE/search".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .addQueryParameter("artist_name", artist)
            .build()
        val body = get(url.toString()) ?: return null
        val arr = JSONArray(body)
        var best: String? = null
        var bestDiff = Double.MAX_VALUE
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val synced = item.lyricsOrNull("syncedLyrics") ?: continue
            val diff = abs(item.optDouble("duration", 0.0) - seconds)
            if (diff < bestDiff) {
                bestDiff = diff
                best = synced
            }
        }
        return best
    }

    private fun get(url: String): String? {
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            return res.body?.string()
        }
    }

    /**
     * Teks dari kunci [key], atau null kalau kunci tidak ada atau nilainya JSON null.
     * Penting: optString() di Android mengubah JSON null menjadi teks "null", yang lolos isNotBlank().
     */
    private fun JSONObject.lyricsOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    /** Membuang tambahan seperti "(Official Video)" atau "[Lyrics]" dari judul YouTube. */
    private fun cleanTitle(title: String): String =
        title.replace(NOISE, " ")
            .substringBefore(" | ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { title }

    /**
     * Mengubah teks LRC ([mm:ss.xx] baris) menjadi daftar baris berwaktu yang sudah terurut.
     * Baris tanpa kata dianggap jeda. Jeda hanya dipertahankan kalau cukup panjang, dan
     * jeda di awal lagu ditambahkan sebagai baris kosong di t=0.
     */
    internal fun parseLrc(lrc: String): List<LyricLine> {
        val all = lrc.lineSequence().mapNotNull { line ->
            val match = STAMP.find(line) ?: return@mapNotNull null
            val text = line.substring(match.range.last + 1).trim()
            LyricLine(timeMs = stampMs(match), text = text)
        }.sortedBy { it.timeMs }.toList()

        val kept = all.filterIndexed { index, line ->
            if (!line.isGap) return@filterIndexed true
            // Stempel terakhir yang tidak diikuti kata menutup lagu; selalu dipertahankan.
            val next = all.getOrNull(index + 1) ?: return@filterIndexed true
            next.timeMs - line.timeMs >= MIN_GAP_MS
        }

        val first = kept.firstOrNull() ?: return kept
        return if (!first.isGap && first.timeMs >= MIN_GAP_MS) {
            listOf(LyricLine(0L, "")) + kept
        } else {
            kept
        }
    }

    /** Menerjemahkan stempel [mm:ss.xx] atau [mm:ss.xxx] menjadi milidetik. */
    private fun stampMs(match: MatchResult): Long {
        val (minutes, seconds, fraction) = match.destructured
        val fractionMs = when (fraction.length) {
            2 -> fraction.toLong() * 10
            3 -> fraction.toLong()
            else -> 0L
        }
        return minutes.toLong() * 60_000 + seconds.toLong() * 1_000 + fractionMs
    }

    private val STAMP = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})]""")

    private val NOISE = Regex(
        """\((?:from|feat\.?|official|lyrical|video|audio|remix)[^)]*\)|\[[^]]*]|""" +
            """\b(?:official (?:video|audio|music video)|lyrical|full song|4k video)\b""",
        RegexOption.IGNORE_CASE,
    )
}
