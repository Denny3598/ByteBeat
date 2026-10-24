package com.example.applemusicui.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.AppPrefs
import com.example.applemusicui.data.Song
import com.example.applemusicui.data.YouTubeMusicRepository
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.components.SongRow
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val HISTORY_FILE = "search_history"
private const val HISTORY_KEY = "items"
private const val HISTORY_MAX = 12

private fun loadHistory(context: Context): List<String> {
    val raw = context.getSharedPreferences(HISTORY_FILE, Context.MODE_PRIVATE)
        .getString(HISTORY_KEY, "")
        .orEmpty()
    return raw.split("\n").filter { it.isNotBlank() }
}

private fun saveHistory(context: Context, items: List<String>) {
    context.getSharedPreferences(HISTORY_FILE, Context.MODE_PRIVATE)
        .edit()
        .putString(HISTORY_KEY, items.joinToString("\n"))
        .apply()
}

/** Menaruh pencarian di paling atas daftar terbaru, tanpa duplikat. */
private fun pushHistory(context: Context, query: String): List<String> {
    val q = query.trim()
    val updated = (listOf(q) + loadHistory(context).filterNot { it.equals(q, ignoreCase = true) })
        .take(HISTORY_MAX)
    saveHistory(context, updated)
    return updated
}

@Composable
fun SearchScreen(current: Song, onPlay: (Song, List<Song>) -> Unit) {
    val c = AppTheme.colors
    val context = LocalContext.current

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(loadHistory(context)) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isBlank()) {
            results = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(450)
        val musicOnly = AppPrefs.prefs(context).getBoolean("music_only", true)
        val found = try {
            YouTubeMusicRepository.searchSongs(q, songsOnly = musicOnly)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        // Kalau pengguna sudah mengetik lagi, hasil lama ini dibuang.
        if (!isActive) return@LaunchedEffect
        results = found
        loading = false
        history = pushHistory(context, q)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Cari")

        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = { Text("Artis, lagu, atau album", color = c.textSecondary) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = c.textSecondary)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Hapus teks",
                        tint = c.textSecondary,
                        modifier = Modifier.clickable { query = "" },
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.textSecondary.copy(alpha = 0.12f),
                unfocusedContainerColor = c.textSecondary.copy(alpha = 0.12f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
                cursorColor = c.text,
            ),
        )

        val q = query.trim()
        if (q.isBlank()) {
            // Default kosong: hanya tampilkan riwayat pencarian.
            if (history.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Pencarian terbaru",
                        color = c.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Hapus",
                        color = AppleRed,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable {
                                saveHistory(context, emptyList())
                                history = emptyList()
                            }
                            .padding(8.dp),
                    )
                }
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 200.dp),
                ) {
                    items(history) { entry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { query = entry }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = c.textSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                entry,
                                color = c.text,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Ketik untuk mencari lagu, artis, atau album.",
                        color = c.textSecondary,
                        fontSize = 15.sp,
                    )
                }
            }
        } else if (loading && results == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val list = results.orEmpty()
            if (list.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (loading) "Mencari…" else "Tidak ada hasil untuk \"$q\"",
                        color = c.textSecondary,
                        fontSize = 15.sp,
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(list) { song ->
                        SongRow(song, highlighted = song.url == current.url) {
                            onPlay(song, list)
                        }
                    }
                }
            }
        }
    }
}
