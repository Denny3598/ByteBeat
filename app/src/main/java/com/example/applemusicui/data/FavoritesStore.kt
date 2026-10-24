package com.example.applemusicui.data

import android.content.Context

/** Lagu yang ditandai favorit, disimpan di HP lewat alamat videonya. */
object FavoritesStore {
    private const val FILE = "favorites"
    private const val KEY = "urls"

    fun isFavorite(context: Context, url: String): Boolean =
        prefs(context).getStringSet(KEY, emptySet()).orEmpty().contains(url)

    /** Membalik status favorit lalu mengembalikan status yang baru. */
    fun toggle(context: Context, url: String): Boolean {
        val current = prefs(context).getStringSet(KEY, emptySet()).orEmpty().toMutableSet()
        val nowFavorite = if (current.remove(url)) {
            false
        } else {
            current.add(url)
            true
        }
        prefs(context).edit().putStringSet(KEY, current).apply()
        return nowFavorite
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
