package com.example.applemusicui.data

import android.webkit.CookieManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Menyukai lagu di akun YouTube Music yang sedang login. Tanpa login, tidak melakukan apa-apa. */
object LikeRepository {
    private const val LIKE_API = "https://music.youtube.com/youtubei/v1/like/like?prettyPrint=false"
    private const val UNLIKE_API = "https://music.youtube.com/youtubei/v1/like/removelike?prettyPrint=false"
    private const val YT_ORIGIN = "https://music.youtube.com"
    private const val CLIENT_VERSION = "1.20241009.01.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Mengirim suka atau batal suka ke YouTube Music. Mengembalikan true kalau berhasil. */
    suspend fun setLiked(songUrl: String, liked: Boolean): Boolean = withContext(Dispatchers.IO) {
        val videoId = songUrl.substringAfter("v=", "").substringBefore("&")
        if (videoId.isBlank()) return@withContext false
        val cookie = CookieManager.getInstance().getCookie(YT_ORIGIN) ?: return@withContext false
        val sapisid = sapisidOf(cookie) ?: return@withContext false

        try {
            val client = JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", CLIENT_VERSION)
                .put("hl", "id")
                .put("gl", "ID")
            val body = JSONObject()
                .put("context", JSONObject().put("client", client))
                .put("target", JSONObject().put("videoId", videoId))

            val ts = System.currentTimeMillis() / 1000
            val hash = MessageDigest.getInstance("SHA-1")
                .digest("$ts $sapisid $YT_ORIGIN".toByteArray())
                .joinToString("") { "%02x".format(it) }
            val req = Request.Builder()
                .url(if (liked) LIKE_API else UNLIKE_API)
                .header("User-Agent", USER_AGENT)
                .header("Origin", YT_ORIGIN)
                .header("Referer", "$YT_ORIGIN/")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", CLIENT_VERSION)
                .header("Authorization", "SAPISIDHASH ${ts}_$hash")
                .header("Cookie", cookie)
                .header("X-Goog-AuthUser", "0")
                .post(body.toString().toRequestBody(jsonType))
                .build()

            http.newCall(req).execute().use { it.isSuccessful }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    private fun sapisidOf(cookie: String): String? {
        for (part in cookie.split(";")) {
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2 && (kv[0] == "SAPISID" || kv[0] == "__Secure-3PAPISID")) return kv[1]
        }
        return null
    }
}
