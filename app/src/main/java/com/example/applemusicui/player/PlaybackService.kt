package com.example.applemusicui.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.applemusicui.MainActivity
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper

/** Menjalankan pemutar di latar belakang + notifikasi kontrol media. */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(createDataSourceFactory()))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    /** Menambahkan header yang diminta server YouTube sesuai jenis alamat streamnya. */
    private fun createDataSourceFactory(): DataSource.Factory {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(DEFAULT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        return ResolvingDataSource.Factory(http) { dataSpec ->
            val url = dataSpec.uri.toString()
            val headers = HashMap(dataSpec.httpRequestHeaders)

            when {
                YoutubeParsingHelper.isVisionOsStreamingUrl(url) ->
                    headers["User-Agent"] = YoutubeParsingHelper.getVisionOsUserAgent(null)
                YoutubeParsingHelper.isAndroidStreamingUrl(url) ->
                    headers["User-Agent"] = YoutubeParsingHelper.getAndroidUserAgent(null)
                YoutubeParsingHelper.isIosStreamingUrl(url) ->
                    headers["User-Agent"] = YoutubeParsingHelper.getIosUserAgent(null)
            }
            if (YoutubeParsingHelper.isWebStreamingUrl(url)) {
                headers["Origin"] = "https://www.youtube.com"
                headers["Referer"] = "https://www.youtube.com"
            }

            dataSpec.withRequestHeaders(headers)
        }
    }

    private companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
    }
}
