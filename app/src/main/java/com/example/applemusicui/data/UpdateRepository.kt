package com.example.applemusicui.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.applemusicui.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Satu rilis baru: SHA commit-nya, alamat APK, dan catatannya. */
data class UpdateInfo(
    val sha: String,
    val apkUrl: String,
    val notes: String,
)

/**
 * Cek pembaruan dari release "latest" di GitHub. Workflow menulis SHA build ke file build-sha.txt
 * di release itu. Kalau SHA-nya beda dengan build yang terpasang, berarti ada versi baru.
 *
 * Memakai tautan unduhan langsung, bukan API GitHub, supaya tidak kena batas permintaan API
 * (yang sering habis dan membuat pembaruan "tidak ketemu").
 */
object UpdateRepository {
    private const val BASE = "https://github.com/Denny3598/ByteBeat/releases/download/latest"
    private const val APK_NAME = "ByteBeat.apk"
    private const val SHA_FILE = "build-sha.txt"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Mengembalikan rilis kalau lebih baru dari build yang terpasang. Null kalau sudah terbaru atau belum ada build. */
    suspend fun newerRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$BASE/$SHA_FILE?t=${System.currentTimeMillis()}")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "ByteBeat-Android")
            .build()

        http.newCall(req).execute().use { res ->
            // 404 berarti release belum punya file SHA (belum ada build baru), jadi anggap sudah terbaru.
            if (res.code == 404) return@withContext null
            if (!res.isSuccessful) error("HTTP ${res.code}")

            val sha = res.body?.string().orEmpty().trim()
            if (sha.length != 40 || sha == BuildConfig.BUILD_SHA) return@withContext null

            UpdateInfo(sha = sha, apkUrl = "$BASE/$APK_NAME", notes = "")
        }
    }

    /**
     * Mengunduh APK lewat DownloadManager. Begitu unduhan selesai, jendela pemasangan langsung terbuka.
     * Kalau izin "Pasang aplikasi tidak dikenal" belum ada, pengaturan izinnya yang dibuka dulu.
     */
    fun download(context: Context, info: UpdateInfo) {
        val app = context.applicationContext
        val manager = app.getSystemService(DownloadManager::class.java)
        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("ByteBeat")
            .setDescription("Mengunduh pembaruan")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, "ByteBeat-update.apk")
        val id = manager.enqueue(request)

        lateinit var receiver: BroadcastReceiver
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != id) return
                runCatching { app.unregisterReceiver(receiver) }
                val uri = manager.getUriForDownloadedFile(id)
                if (uri == null) {
                    Toast.makeText(app, "Unduhan ByteBeat gagal", Toast.LENGTH_LONG).show()
                } else {
                    openInstaller(app, uri)
                }
            }
        }
        ContextCompat.registerReceiver(
            app,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun openInstaller(app: Context, uri: Uri) {
        if (!app.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${app.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(settings)
            Toast.makeText(
                app,
                "Izinkan ByteBeat memasang aplikasi, lalu ketuk notifikasi unduhan lagi",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        val install = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(install)
    }
}
