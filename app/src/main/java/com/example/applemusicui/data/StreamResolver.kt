package com.example.applemusicui.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo

/** Mengubah link video YouTube menjadi alamat audio yang bisa diputar. */
object StreamResolver {

    suspend fun audioUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        require(videoUrl.isNotBlank()) { "Lagu contoh ini belum punya sumber audio" }
        YouTubeMusicRepository.ensureInit()
        val info = StreamInfo.getInfo(videoUrl)
        info.audioStreams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate }
            ?.content
            ?: error("Tidak ada aliran audio yang bisa diputar")
    }
}
