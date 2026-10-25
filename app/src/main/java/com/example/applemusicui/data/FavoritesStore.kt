package com.example.applemusicui.data

import android.content.Context

/**
 * Cache lokal lagu yang disukai, disimpan lewat alamat videonya.
 * Sumber kebenarannya adalah akun YouTube Music; cache ini diisi ulang dari sana (lihat [LikeRepository.syncFromYouTube]).
 */
object FavoritesStore {
    private const val FILE = "favorites"
    private const val KEY = "urls"

    fun isFavorite(context: Context, url: String): Boolean =
        prefs(context).getStringSet(KEY, emptySet()).orEmpty().contains(url)

    /** Membalik status favorit lalu mengembalikan status yang baru. */
    fun toggle(context: Context, url: String): Boolean {
        val nowFavorite = !isFavorite(context, url)
        set(context, url, nowFavorite)
        return nowFavorite
    }

    fun set(context: Context, url: String, liked: Boolean) {
        val current = prefs(context).getStringSet(KEY, emptySet()).orEmpty().toMutableSet()
        if (liked) current.add(url) else current.remove(url)
        prefs(context).edit().putStringSet(KEY, current).apply()
    }

    /** Mengganti seluruh cache dengan daftar dari YouTube (atau kosong saat logout). */
    fun replaceAll(context: Context, urls: Set<String>) {
        prefs(context).edit().putStringSet(KEY, urls.toSet()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
