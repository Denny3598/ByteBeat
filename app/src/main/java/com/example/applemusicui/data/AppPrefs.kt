package com.example.applemusicui.data

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager

/** Status login YouTube (disimpan di cookie WebView) dan pengaturan kecil. */
object AppPrefs {
    private const val FILE = "app_prefs"
    private const val YT_URL = "https://music.youtube.com"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean {
        val cookies = CookieManager.getInstance().getCookie(YT_URL) ?: return false
        return cookies.contains("SAPISID") || cookies.contains("__Secure-3PAPISID")
    }

    fun logout() {
        val manager = CookieManager.getInstance()
        manager.removeAllCookies(null)
        manager.flush()
    }
}
