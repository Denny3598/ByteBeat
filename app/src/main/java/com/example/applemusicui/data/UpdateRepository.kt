package com.example.applemusicui.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.applemusicui.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Satu rilis baru: SHA commit-nya, alamat APK, dan catatannya. */
data class UpdateInfo(
    val sha: String,
    val apkUrl: String,
    val notes: String,
)

/**
 * Cek pembaruan dari release "latest" di GitHub. Workflow membuat release itu setiap kali ada build,
 * dan targetnya adalah SHA commit. Kalau SHA itu sama dengan build yang terpasang, berarti sudah terbaru.
 */
object UpdateRepository {
    private const val RELEASE_API = "https://api.github.com/repos/Denny3598/ByteBeat/releases/tags/latest"
    private const val APK_NAME = "ByteBeat.apk"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Mengembalikan rilis kalau lebih baru dari build yang terpasang. Null kalau sudah terbaru. */
    suspend fun newerRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ByteBeat-Android")
            .build()

        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("HTTP ${res.code}")
            val json = JSONObject(res.body?.string().orEmpty())

            val sha = json.optString("target_commitish")
            // SHA harus 40 karakter heksadesimal. Kalau bukan (misalnya nama branch), jangan tampilkan pembaruan palsu.
            if (sha.length != 40) return@withContext null
            if (sha == BuildConfig.BUILD_SHA) return@withContext null

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == APK_NAME) {
                    apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                    break
                }
            }
            if (apkUrl == null) return@withContext null

            UpdateInfo(sha = sha, apkUrl = apkUrl, notes = json.optString("body"))
        }
    }

    /**
     * Mengunduh APK lewat DownloadManager. Notifikasi unduhan selesai bisa diketuk untuk memasangnya.
     * Di Android, aplikasi ini perlu izin "Pasang aplikasi tidak dikenal" saat pertama kali memasang.
     */
    fun download(context: Context, info: UpdateInfo) {
        val manager = context.getSystemService(DownloadManager::class.java)
        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("ByteBeat")
            .setDescription("Mengunduh pembaruan")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "ByteBeat-update.apk")
        manager.enqueue(request)
    }
}
