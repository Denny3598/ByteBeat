package com.example.applemusicui.ui.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/** Satu ExoPlayer untuk seluruh aplikasi, dipakai bersama oleh service dan UI. */
object SharedPlayer {
    // Sama dengan User-Agent di DownloaderImpl, supaya URL audio diterima YouTube.
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0"

    @Volatile
    private var instance: ExoPlayer? = null

    fun get(context: Context): ExoPlayer =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(appContext: Context): ExoPlayer {
        val dataSource = DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT)
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
