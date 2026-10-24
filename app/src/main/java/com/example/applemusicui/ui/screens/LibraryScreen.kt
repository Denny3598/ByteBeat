package com.example.applemusicui.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.AppPrefs
import com.example.applemusicui.data.LibraryItem
import com.example.applemusicui.data.LibraryRepository
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val LIKED_ID = "FEmusic_liked_videos"

/** Lima tab pustaka. Browse ID-nya adalah halaman library milik YouTube Music. */
private enum class LibrarySection(val label: String, val browseId: String) {
    Playlist("Playlist", "FEmusic_liked_playlists"),
    Album("Album", "FEmusic_liked_albums"),
    Artist("Artis", "FEmusic_library_corpus_track_artists"),
    Subscription("Langganan", "FEmusic_library_corpus_artists"),
    Podcast("Podcast", "FEmusic_library_non_music_audio_list"),
}

/** Data pustaka yang sudah dimuat. Disimpan selama aplikasi hidup, supaya Pustaka langsung tampil tanpa menunggu. */
private var cachedSections: Map<LibrarySection, List<LibraryItem>>? = null

/** Memuat ulang semua rak pustaka ke cache. Dipanggil saat aplikasi dibuka dan saat Pustaka dibuka. */
suspend fun refreshLibraryCache() {
    if (!AppPrefs.isLoggedIn()) return
    val fresh = loadSections()
    // Kalau semua gagal (misalnya tidak ada internet), data lama tetap dipakai.
    if (fresh.values.any { it.isNotEmpty() }) cachedSections = fresh
}

/** Dipanggil saat logout, supaya pustaka akun sebelumnya tidak tampil lagi. */
fun clearLibraryCache() {
    cachedSections = null
}

private suspend fun loadSections(): Map<LibrarySection, List<LibraryItem>> = coroutineScope {
    LibrarySection.entries
        .map { section ->
            section to async(Dispatchers.IO) {
                runCatching { LibraryRepository.library(section.browseId) }.getOrDefault(emptyList())
            }
        }
        .associate { (section, deferred) -> section to deferred.await() }
}

@Composable
fun LibraryScreen(
    onPlay: (Song, List<Song>) -> Unit = { _, _ -> },
    onOpenPage: (String) -> Unit = {},
) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    val loggedIn = AppPrefs.isLoggedIn()

    var sections by remember { mutableStateOf(cachedSections) }
    // null = tab "Semua" (tumpukan rak dengan "Lihat semua").
    var selected by remember { mutableStateOf<LibrarySection?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loggedIn) {
        refreshLibraryCache()
        sections = cachedSections ?: emptyMap()
    }

    // Tombol Back menutup grid "Lihat semua" dulu, baru keluar dari layar.
    BackHandler(enabled = selected != null) { selected = null }

    fun open(item: LibraryItem) {
        // Album, playlist, dan artis dibuka halamannya. Lagu yang disukai langsung diputar.
        if (item.browseId != LIKED_ID) {
            onOpenPage(item.browseId)
            return
        }
        if (busyId != null) return
        busyId = item.browseId
        message = null
        scope.launch {
            val songs = runCatching { LibraryRepository.tracks(item.browseId) }
                .getOrDefault(emptyList())
            busyId = null
            val first = songs.firstOrNull()
            if (first == null) {
                message = "\"${item.title}\" tidak bisa dibuka sekarang."
            } else {
                onPlay(first, songs)
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Perpustakaan")

        message?.let { msg ->
            Text(
                msg,
                color = AppleRed,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        val current = selected
        val data = sections
        if (current == null) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 200.dp),
            ) {
                if (!loggedIn) {
                    item {
                        Text(
                            "Masuk ke YouTube Music lewat Pengaturan untuk melihat pustaka akunmu.",
                            color = c.textSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                } else if (data == null) {
                    item {
                        Text(
                            "Memuat pustaka…",
                            color = c.textSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                } else {
                    LibrarySection.entries.forEach { section ->
                        val list = data[section].orEmpty()
                        if (list.isNotEmpty()) {
                            item(key = section.name) {
                                Column {
                                    SectionTitle(section.label) { selected = section }
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        items(list.take(12), key = { it.browseId }) { item ->
                                            LibraryCard(item, busy = busyId == item.browseId) { open(item) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val list = data?.get(current).orEmpty()
            // Judul dan tombol kembali untuk tampilan "Lihat semua".
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c.surface)
                        .clickable { selected = null },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = c.text,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    current.label,
                    color = c.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 200.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(list, key = { it.browseId }) { item ->
                    LibraryCard(
                        item = item,
                        busy = busyId == item.browseId,
                        modifier = Modifier.fillMaxWidth(),
                    ) { open(item) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, onSeeAll: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = c.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Lihat semua",
            color = c.text,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable(onClick = onSeeAll)
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun LibraryCard(
    item: LibraryItem,
    busy: Boolean,
    modifier: Modifier = Modifier.width(160.dp),
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth()) {
            Artwork(
                listOf(AppleRed, c.surface),
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                corner = 14.dp,
                imageUrl = item.thumbnail,
            )
            if (busy) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .size(28.dp),
                    color = Color.White,
                    strokeWidth = 3.dp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            item.title,
            color = c.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                item.subtitle,
                color = c.textSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Baris folder: ikon, judul, panah kanan (kalau bisa diketuk), dan garis pemisah. */
@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    busy: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = AppTheme.colors
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 60.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = c.control,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = c.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else if (onClick != null) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = c.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 60.dp),
            thickness = 0.5.dp,
            color = c.divider,
        )
    }
}
