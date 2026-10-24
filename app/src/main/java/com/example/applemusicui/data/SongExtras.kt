package com.example.applemusicui.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data tambahan untuk satu lagu, diambil dari endpoint "next" YouTube Music:
 * radio lagu, dan id album serta artis untuk tombol "Buka album" dan "Buka artis".
 */
object SongExtras {
    data class Links(val albumId: String?, val artistId: String?)

    /** Lagu-lagu radio dari lagu ini. Daftar pertama biasanya lagu itu sendiri. */
    suspend fun radio(videoId: String): List<Song> {
        val root = InnerTubeClient.next(videoId, "RDAMVM$videoId")
        return renderers(root).mapNotNull { songOf(it) }
    }

    /** Id album dan artis dari lagu ini. Best-effort: kalau tidak ketemu, nilainya null. */
    suspend fun links(videoId: String): Links {
        val root = InnerTubeClient.next(videoId)
        val items = renderers(root)
        val own = items.firstOrNull { videoIdOf(it) == videoId } ?: items.firstOrNull()
        return Links(
            albumId = own?.let { bylineId(it, "MPRE") } ?: findBrowseId(root, "MPRE"),
            artistId = own?.let { bylineId(it, "UC") } ?: findBrowseId(root, "UC"),
        )
    }

    private fun songOf(r: JSONObject): Song? {
        val vid = videoIdOf(r) ?: return null
        val title = runsOf(r.optJSONObject("title")) ?: return null
        val artist = runsOf(r.optJSONObject("longBylineText")).orEmpty()
        val thumb = r.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { it.optJSONObject(it.length() - 1)?.optString("url")?.ifEmpty { null } }
        return Song(
            title = title,
            artist = artist,
            art = artFor(title),
            url = "https://www.youtube.com/watch?v=$vid",
            thumbnail = thumb,
        )
    }

    private fun videoIdOf(r: JSONObject): String? =
        r.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?.ifEmpty { null }

    /** Browse id di baris "oleh ..." (artis dan album) yang awalannya cocok. */
    private fun bylineId(r: JSONObject, prefix: String): String? {
        val runs = r.optJSONObject("longBylineText")?.optJSONArray("runs") ?: return null
        for (i in 0 until runs.length()) {
            val id = runs.optJSONObject(i)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId")
                .orEmpty()
            if (id.startsWith(prefix)) return id
        }
        return null
    }

    private fun runsOf(obj: JSONObject?): String? {
        if (obj == null) return null
        val runs = obj.optJSONArray("runs") ?: return obj.optString("simpleText").ifEmpty { null }
        val text = buildString {
            for (i in 0 until runs.length()) append(runs.optJSONObject(i)?.optString("text").orEmpty())
        }
        return text.ifEmpty { null }
    }

    /** Semua playlistPanelVideoRenderer di dalam respons, sesuai urutannya. */
    private fun renderers(node: Any?): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        collect(node, "playlistPanelVideoRenderer", out)
        return out
    }

    private fun collect(node: Any?, key: String, out: MutableList<JSONObject>) {
        when (node) {
            is JSONObject -> {
                node.optJSONObject(key)?.let { out += it; return }
                val keys = node.keys()
                while (keys.hasNext()) collect(node.get(keys.next()), key, out)
            }
            is JSONArray -> for (i in 0 until node.length()) collect(node.get(i), key, out)
        }
    }

    /** Browse id pertama yang diawali [prefix], di mana pun letaknya dalam JSON. */
    private fun findBrowseId(node: Any?, prefix: String): String? {
        when (node) {
            is JSONObject -> {
                val id = node.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
                if (id.startsWith(prefix)) return id
                val keys = node.keys()
                while (keys.hasNext()) findBrowseId(node.get(keys.next()), prefix)?.let { return it }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                findBrowseId(node.get(i), prefix)?.let { return it }
            }
        }
        return null
    }
}
