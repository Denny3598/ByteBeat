package com.example.applemusicui.data

import android.net.Uri
import androidx.media3.common.MimeTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.ConcurrentHashMap

/** Alamat audio yang siap diputar, beserta tipenya (null = file biasa, mp4/webm). */
data class ResolvedAudio(val url: String, val mimeType: String?)

/**
 * Mengubah link video YouTube menjadi alamat audio yang bisa diputar.
 *
 * Mengambil alamat ini lewat jaringan memakan waktu beberapa detik, jadi hasilnya di-cache
 * sampai masa berlaku alamatnya habis, dan lagu berikutnya di antrean disiapkan lebih dulu
 * (lihat [prefetch]) supaya tidak ada jeda saat pindah lagu.
 */
object StreamResolver {

    private const val FALLBACK_TTL_MS = 30 * 60 * 1000L
    // Alamat dianggap habis sedikit lebih awal, supaya tidak dipakai di detik-detik terakhir.
    private const val EXPIRY_MARGIN_MS = 5 * 60 * 1000L

    private class Entry(val audio: ResolvedAudio, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, Entry>()
    private val inFlight = ConcurrentHashMap<String, Deferred<ResolvedAudio>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun audio(videoUrl: String): ResolvedAudio {
        require(videoUrl.isNotBlank()) { "Lagu contoh ini belum punya sumber audio" }

        cache[videoUrl]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.audio }

        // Kalau sedang diambil di belakang (prefetch), tunggu hasil itu, jangan ambil dua kali.
        val job = inFlight.computeIfAbsent(videoUrl) { url -> scope.async { resolve(url) } }
        try {
            return job.await()
        } finally {
            inFlight.remove(videoUrl, job)
        }
    }

    /** Mulai mengambil alamat audio di latar belakang, tanpa menunggu. */
    fun prefetch(videoUrl: String) {
        if (videoUrl.isBlank()) return
        if (cache[videoUrl]?.expiresAtMs?.let { it > System.currentTimeMillis() } == true) return
        scope.launch { runCatching { audio(videoUrl) } }
    }

    /** Dibuang kalau pemutar gagal, supaya percobaan ulang mengambil alamat baru. */
    fun invalidate(videoUrl: String) {
        cache.remove(videoUrl)
    }

    private fun resolve(videoUrl: String): ResolvedAudio {
        YouTubeMusicRepository.ensureInit()
        val info = StreamInfo.getInfo(videoUrl)

        // 1. Aliran file biasa (paling cepat dan paling stabil).
        val progressive = info.audioStreams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate }
        val resolved = when {
            progressive != null -> ResolvedAudio(progressive.content, null)
            // 2. Kalau tidak ada, pakai HLS (biasanya untuk video musik yang hanya punya aliran terpisah).
            !info.hlsUrl.isNullOrBlank() -> ResolvedAudio(info.hlsUrl, MimeTypes.APPLICATION_M3U8)
            // 3. Terakhir, manifest DASH.
            !info.dashMpdUrl.isNullOrBlank() -> ResolvedAudio(info.dashMpdUrl, MimeTypes.APPLICATION_MPD)
            else -> error("Tidak ada aliran audio yang bisa diputar")
        }

        cache[videoUrl] = Entry(resolved, expiryOf(resolved.url))
        return resolved
    }

    /** Membaca waktu habis dari parameter `expire` di URL YouTube (detik sejak epoch). */
    private fun expiryOf(url: String): Long {
        val now = System.currentTimeMillis()
        val expireSec = Uri.parse(url).getQueryParameter("expire")?.toLongOrNull()
            ?: return now + FALLBACK_TTL_MS
        return (expireSec * 1000L - EXPIRY_MARGIN_MS).coerceAtMost(now + FALLBACK_TTL_MS)
    }
}
