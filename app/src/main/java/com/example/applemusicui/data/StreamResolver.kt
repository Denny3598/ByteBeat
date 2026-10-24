package com.example.applemusicui.data

import androidx.media3.common.MimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo

/** Alamat audio yang siap diputar, beserta tipenya (null = file biasa, mp4/webm). */
data class ResolvedAudio(val url: String, val mimeType: String?)

/** Mengubah link video YouTube menjadi alamat audio yang bisa diputar. */
object StreamResolver {

    suspend fun audio(videoUrl: String): ResolvedAudio = withContext(Dispatchers.IO) {
        require(videoUrl.isNotBlank()) { "Lagu contoh ini belum punya sumber audio" }
        YouTubeMusicRepository.ensureInit()
        val info = StreamInfo.getInfo(videoUrl)

        // 1. Aliran file biasa (paling cepat dan paling stabil).
        val progressive = info.audioStreams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate }
        if (progressive != null) return@withContext ResolvedAudio(progressive.content, null)

        // 2. Kalau tidak ada, pakai HLS (biasanya untuk video musik yang hanya punya aliran terpisah).
        info.hlsUrl?.takeIf { it.isNotBlank() }?.let {
            return@withContext ResolvedAudio(it, MimeTypes.APPLICATION_M3U8)
        }

        // 3. Terakhir, manifest DASH.
        info.dashMpdUrl?.takeIf { it.isNotBlank() }?.let {
            return@withContext ResolvedAudio(it, MimeTypes.APPLICATION_MPD)
        }

        error("Tidak ada aliran audio yang bisa diputar")
    }
}
