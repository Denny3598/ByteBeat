package com.example.applemusicui.data

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Profil akun YouTube Music yang sedang login: nama, handle atau email, dan foto. */
data class AccountProfile(
    val name: String,
    val handle: String,
    val photo: String?,
)

/** Mengambil profil akun dari endpoint account_menu, dengan cara yang sama seperti BitChord. */
object AccountRepository {
    private const val API = "https://music.youtube.com/youtubei/v1/account/account_menu?prettyPrint=false"
    private const val YT_ORIGIN = "https://music.youtube.com"
    private const val CLIENT_VERSION = "1.20241009.01.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Null kalau belum login atau profil tidak ditemukan di respons. */
    suspend fun profile(): AccountProfile? = withContext(Dispatchers.IO) {
        val cookie = CookieManager.getInstance().getCookie(YT_ORIGIN) ?: return@withContext null
        val sapisid = sapisidOf(cookie) ?: return@withContext null

        val ts = System.currentTimeMillis() / 1000
        val hash = MessageDigest.getInstance("SHA-1")
            .digest("$ts $sapisid $YT_ORIGIN".toByteArray())
            .joinToString("") { "%02x".format(it) }

        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("hl", "id")
            .put("gl", "ID")
        val body = JSONObject().put("context", JSONObject().put("client", client))

        val req = Request.Builder()
            .url(API)
            .header("User-Agent", USER_AGENT)
            .header("Origin", YT_ORIGIN)
            .header("Referer", "$YT_ORIGIN/")
            .header("Authorization", "SAPISIDHASH ${ts}_$hash")
            .header("Cookie", cookie)
            .header("X-Goog-AuthUser", "0")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", CLIENT_VERSION)
            .post(body.toString().toRequestBody(jsonType))
            .build()

        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("HTTP ${res.code}")
            val root = JSONObject(res.body?.string().orEmpty())
            val header = firstOf(root, "activeAccountHeaderRenderer") ?: return@withContext null
            val name = textOf(header.optJSONObject("accountName")).orEmpty()
            if (name.isBlank()) return@withContext null
            val handle = textOf(header.optJSONObject("email"))
                ?: textOf(header.optJSONObject("channelHandle")).orEmpty()
            AccountProfile(
                name = name,
                handle = handle,
                photo = photoOf(header.optJSONObject("accountPhoto")),
            )
        }
    }

    private fun sapisidOf(cookie: String): String? {
        for (part in cookie.split(";")) {
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2 && (kv[0] == "SAPISID" || kv[0] == "__Secure-3PAPISID")) return kv[1]
        }
        return null
    }

    /** Objek pertama, di mana pun letaknya, yang memakai kunci tersebut. */
    private fun firstOf(node: Any?, key: String): JSONObject? {
        when (node) {
            is JSONObject -> {
                node.optJSONObject(key)?.let { return it }
                val names = node.keys()
                while (names.hasNext()) {
                    firstOf(node.get(names.next()), key)?.let { return it }
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                firstOf(node.get(i), key)?.let { return it }
            }
        }
        return null
    }

    private fun textOf(obj: JSONObject?): String? {
        if (obj == null) return null
        obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.optJSONArray("runs") ?: return null
        return (0 until runs.length())
            .joinToString("") { runs.getJSONObject(it).optString("text") }
            .takeIf { it.isNotBlank() }
    }

    /** Foto terbesar dari daftar thumbnails. */
    private fun photoOf(obj: JSONObject?): String? {
        val arr = obj?.optJSONArray("thumbnails") ?: return null
        if (arr.length() == 0) return null
        return arr.optJSONObject(arr.length() - 1)?.optString("url")?.takeIf { it.isNotBlank() }
    }
}
