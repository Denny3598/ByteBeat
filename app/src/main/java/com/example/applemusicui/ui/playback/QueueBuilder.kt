/*
 * Diadaptasi dari BitChord (https://github.com/kushagrasinghx/BitChord), lisensi GPL v3.
 * Asal: shared/.../playback/QueueBuilder.kt dan QueueSections.kt.
 */
package com.example.applemusicui.ui.playback

import com.example.applemusicui.data.Song
import java.util.Locale

/** Lewat titik ini, tombol kembali mengulang lagu dari awal, bukan pindah ke lagu sebelumnya. */
const val BACK_RESTARTS_AFTER_MS = 10_000L

/**
 * Menyusun tambahan AutoPlay (radio) supaya tidak ada lagu yang sama dua kali
 * dan satu artis tidak mendominasi.
 *
 * Dua masalah yang diselesaikan:
 * - Radio YouTube sering memuat rekaman yang sama dua kali (audio resmi dan video musiknya
 *   punya ID berbeda dengan judul hampir sama). Deduplikasi berdasarkan ID saja tidak cukup,
 *   jadi lagu dicocokkan dengan judul yang sudah dinormalisasi plus artis.
 * - Radio cenderung banyak memuat artis dari lagu benih. Batas per artis mencegah antrean
 *   berubah menjadi diskografi satu artis.
 */
object QueueBuilder {

    /** Jumlah lagu maksimum dari satu artis dalam satu batch. */
    const val PER_ARTIST_LIMIT = 2

    /** Artis dari lagu benih boleh lebih banyak, tapi tetap ada batasnya. */
    const val SEED_ARTIST_LIMIT = 4

    /** Bagian dari [candidates] yang layak ditambahkan setelah [existing]: tidak ada yang sudah di antrean, tidak ada duplikat, paling banyak [limit]. */
    fun extend(existing: List<Song>, candidates: List<Song>, limit: Int): List<Song> {
        val taken = existing.toMutableList()
        val perArtist = mutableMapOf<String, Int>()
        val seedArtists = existing.lastOrNull()?.artist?.let(::artistSet).orEmpty()
        val out = mutableListOf<Song>()

        for (candidate in candidates) {
            if (out.size >= limit) break
            if (taken.any { isSameRecording(it, candidate) }) continue

            val artists = artistSet(candidate.artist)
            val key = artists.minOrNull()
            if (key != null) {
                val cap = if (artists.any { it in seedArtists }) SEED_ARTIST_LIMIT else PER_ARTIST_LIMIT
                val played = perArtist[key] ?: 0
                if (played >= cap) continue
                perArtist[key] = played + 1
            }

            taken += candidate
            out += candidate
        }
        return out
    }

    /** Apakah dua lagu adalah rekaman yang sama. ID bisa berbeda antara versi audio dan video. */
    fun isSameRecording(a: Song, b: Song): Boolean {
        val idA = videoIdOf(a)
        if (idA.isNotEmpty() && idA == videoIdOf(b)) return true
        val title = normalisedTitle(a.title)
        if (title.isEmpty() || title != normalisedTitle(b.title)) return false
        // Cukup satu nama artis yang sama. Urutan penagihan artis bisa berbeda antara dua versi.
        val left = artistSet(a.artist)
        val right = artistSet(b.artist)
        return left.isEmpty() || right.isEmpty() || left.any { it in right }
    }

    /** Judul tanpa embel-embel seperti "(Official Video)" atau "| Lyrics". Penanda remix dan cover tetap dipertahankan. */
    fun normalisedTitle(raw: String): String = raw.lowercase(Locale.ROOT)
        .substringBefore(" | ")
        .replace(NOISE, " ")
        .replace(PUNCTUATION, " ")
        .replace(SPACES, " ")
        .trim()

    /** Daftar artis dari satu kredit, dipisah supaya urutan penagihan tidak berpengaruh. */
    fun artistSet(raw: String): Set<String> = raw.lowercase(Locale.ROOT)
        .replace(TOPIC, " ")
        .split(",", "&", "·", "•", ";", " feat", " ft.", " ft ", " x ", " with ")
        .map { it.replace(PUNCTUATION, " ").replace(SPACES, " ").trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    /** ID video dari URL `watch?v=...`, atau kosong kalau tidak ada. */
    fun videoIdOf(song: Song): String = song.url.substringAfter("v=", "").substringBefore("&")

    private val NOISE = Regex(
        """\((?:official|lyric|lyrics|lyrical|audio|video|visuali[sz]er|full song|hd|4k)[^)]*\)""" +
            """|\(from[^)]*\)""" +
            """|\[[^]]*]""" +
            """|\b(?:official (?:video|audio|music video)|lyrical video|full video|4k video)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val TOPIC = Regex("""\s*-\s*topic\b""", RegexOption.IGNORE_CASE)
    private val PUNCTUATION = Regex("""[^\p{L}\p{N}]+""")
    private val SPACES = Regex("""\s+""")
}
