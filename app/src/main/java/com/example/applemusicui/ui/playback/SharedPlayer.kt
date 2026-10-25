package com.example.applemusicui.ui.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
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
        // Mulai memutar setelah 1 detik data (bawaan 2,5 detik), supaya lagu terdengar lebih cepat.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                1_000,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .build()
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .setLoadControl(loadControl)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
