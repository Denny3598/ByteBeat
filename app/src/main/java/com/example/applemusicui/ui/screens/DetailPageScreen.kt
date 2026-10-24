package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.AlbumPage
import com.example.applemusicui.data.ArtistPage
import com.example.applemusicui.data.DetailRepository
import com.example.applemusicui.data.LibraryItem
import com.example.applemusicui.data.PodcastDetail
import com.example.applemusicui.data.PodcastEpisode
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.components.rememberCoverTint
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed
import kotlinx.coroutines.CancellationException

private sealed interface DetailState {
    data object Loading : DetailState
    data class Error(val message: String) : DetailState
    data class Album(val page: AlbumPage) : DetailState
    data class Artist(val page: ArtistPage) : DetailState
    data class Podcast(val page: PodcastDetail) : DetailState
}

/** Pilihan urutan lagu di halaman album dan playlist. */
private enum class TrackSort(val label: String) {
    Original("Urutan asli"),
    TitleAsc("Judul A–Z"),
    TitleDesc("Judul Z–A"),
    ArtistAsc("Artis A–Z");

    fun apply(list: List<Song>): List<Song> = when (this) {
        Original -> list
        TitleAsc -> list.sortedBy { it.title.lowercase() }
        TitleDesc -> list.sortedByDescending { it.title.lowercase() }
        ArtistAsc -> list.sortedBy { it.artist.lowercase() }
    }
}

/**
 * Halaman album/playlist (id bukan "UC...") atau artis (id "UC...").
 * Tampilan mengikuti BitChord: cover/foto besar, tombol Putar dan Acak, lalu daftar lagu.
 */
@Composable
fun DetailPageScreen(
    browseId: String,
    onPlay: (Song, List<Song>) -> Unit,
    onOpenPage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = AppTheme.colors
    val isArtist = browseId.startsWith("UC")
    var state by remember(browseId) { mutableStateOf<DetailState>(DetailState.Loading) }
    var attempt by remember(browseId) { mutableStateOf(0) }

    LaunchedEffect(browseId, attempt) {
        state = DetailState.Loading
        state = try {
            if (isArtist) {
                DetailState.Artist(DetailRepository.artist(browseId))
            } else if (browseId.startsWith("MPSP")) {
                DetailState.Podcast(DetailRepository.podcast(browseId))
            } else {
                DetailState.Album(DetailRepository.album(browseId))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            DetailState.Error("Halaman tidak bisa dibuka (${e.javaClass.simpleName} ${e.message.orEmpty()})")
        }
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        when (val s = state) {
            is DetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is DetailState.Error -> ErrorBody(s.message) { attempt++ }
            is DetailState.Album -> AlbumBody(s.page, onPlay)
            is DetailState.Artist -> ArtistBody(s.page, onPlay, onOpenPage)
            is DetailState.Podcast -> PodcastBody(s.page, onPlay)
        }

        // Tombol kembali, di atas konten.
        Box(
            Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            message,
            color = c.textSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(c.text)
                .clickable(onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text("Coba lagi", color = c.background, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Latar halaman. Kalau warna cover sudah diketahui, latar memakai warna itu dan memudar ke warna aplikasi.
 * Kalau belum, dipakai cover yang di-blur seperti sebelumnya.
 */
@Composable
private fun BoxScope.PageBackdrop(imageUrl: String?, tint: Color?) {
    val c = AppTheme.colors
    if (tint != null) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to tint,
                        0.5f to lerp(tint, c.background, 0.55f),
                        0.85f to c.background,
                        1f to c.background,
                    ),
                ),
        )
        return
    }
    Artwork(
        listOf(AppleRed, c.surface),
        Modifier
            .matchParentSize()
            .blur(60.dp),
        corner = 0.dp,
        imageUrl = imageUrl,
    )
    Box(
        Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    0f to c.background.copy(alpha = 0.45f),
                    0.55f to c.background.copy(alpha = 0.9f),
                    1f to c.background,
                ),
            ),
    )
}

@Composable
private fun AlbumBody(page: AlbumPage, onPlay: (Song, List<Song>) -> Unit) {
    val c = AppTheme.colors
    var sort by remember(page.title) { mutableStateOf(TrackSort.Original) }
    val songs = remember(page.songs, sort) { sort.apply(page.songs) }
    val tint = rememberCoverTint(page.thumbnail)

    Box(Modifier.fillMaxSize()) {
        PageBackdrop(page.thumbnail, tint)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 200.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 64.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Artwork(
                        listOf(AppleRed, c.surface),
                        Modifier
                            .fillMaxWidth(0.66f)
                            .aspectRatio(1f)
                            .shadow(18.dp, RoundedCornerShape(12.dp)),
                        corner = 12.dp,
                        imageUrl = page.thumbnail,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        page.title,
                        color = c.text,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (page.subtitle.isNotBlank()) {
                        Text(
                            page.subtitle,
                            color = c.textSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    ActionRow(
                        onPlay = {
                            songs.firstOrNull()?.let { onPlay(it, songs) }
                        },
                        onShuffle = {
                            val list = songs.shuffled()
                            list.firstOrNull()?.let { onPlay(it, list) }
                        },
                        sort = sort,
                        onSort = { sort = it },
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            items(songs) { song ->
                TrackRow(song = song) { onPlay(song, songs) }
            }
        }
    }
}

@Composable
private fun ArtistBody(
    page: ArtistPage,
    onPlay: (Song, List<Song>) -> Unit,
    onOpenPage: (String) -> Unit,
) {
    val c = AppTheme.colors
    Box(Modifier.fillMaxSize()) {
        PageBackdrop(page.thumbnail, null)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 200.dp),
        ) {
            item {
                // Foto artis yang memudar ke latar.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.95f),
                ) {
                    Artwork(
                        listOf(AppleRed, c.surface),
                        Modifier.fillMaxSize(),
                        corner = 0.dp,
                        imageUrl = page.thumbnail,
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0.5f to Color.Transparent,
                                    1f to c.background,
                                ),
                            ),
                    )
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        page.name,
                        color = c.text,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(16.dp))
                    // Tombol Putar tepat di tengah; Acak di kiri, dengan ruang kosong di kanan agar seimbang.
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(c.textSecondary.copy(alpha = 0.15f))
                                    .clickable {
                                        val list = page.topSongs.shuffled()
                                        list.firstOrNull()?.let { onPlay(it, list) }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Shuffle,
                                    contentDescription = "Acak",
                                    tint = c.text,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Box(
                            Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(c.text)
                                .clickable {
                                    page.topSongs.firstOrNull()?.let { onPlay(it, page.topSongs) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Putar",
                                tint = c.background,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Box(Modifier.weight(1f))
                    }
                    page.description?.let { desc ->
                        Text(
                            desc,
                            color = c.textSecondary,
                            fontSize = 14.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 18.dp),
                        )
                    }
                }
            }

            if (page.topSongs.isNotEmpty()) {
                item { SectionTitle("Lagu populer") }
                items(page.topSongs.take(10)) { song ->
                    TrackRow(song = song) { onPlay(song, page.topSongs) }
                }
            }

            if (page.albums.isNotEmpty()) {
                item { SectionTitle("Album") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(page.albums, key = { it.browseId }) { album ->
                            AlbumCard(album) { onOpenPage(album.browseId) }
                        }
                    }
                }
            }
        }
    }
}

/** Halaman show podcast, mengikuti PodcastScreen ArchiveTune: header besar, lalu daftar episode dengan cover, tanggal, dan durasi. */
@Composable
private fun PodcastBody(page: PodcastDetail, onPlay: (Song, List<Song>) -> Unit) {
    val c = AppTheme.colors
    var sort by remember(page.title) { mutableStateOf(TrackSort.Original) }
    val episodes = remember(page.episodes, sort) { sort.applyEpisodes(page.episodes) }
    val songs = remember(episodes) { episodes.map { it.song } }
    val tint = rememberCoverTint(page.thumbnail)

    Box(Modifier.fillMaxSize()) {
        PageBackdrop(page.thumbnail, tint)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 200.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 64.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Artwork(
                        listOf(AppleRed, c.surface),
                        Modifier
                            .fillMaxWidth(0.66f)
                            .aspectRatio(1f)
                            .shadow(18.dp, RoundedCornerShape(12.dp)),
                        corner = 12.dp,
                        imageUrl = page.thumbnail,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        page.title,
                        color = c.text,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (page.author.isNotBlank()) {
                        Text(
                            page.author,
                            color = c.textSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Text(
                        "${page.episodes.size} episode",
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    ActionRow(
                        onPlay = { songs.firstOrNull()?.let { onPlay(it, songs) } },
                        onShuffle = {
                            val list = songs.shuffled()
                            list.firstOrNull()?.let { onPlay(it, list) }
                        },
                        sort = sort,
                        onSort = { sort = it },
                    )
                    page.description?.takeIf(String::isNotBlank)?.let { desc ->
                        Text(
                            desc,
                            color = c.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            items(episodes, key = { it.song.url }) { episode ->
                EpisodeRow(episode) { onPlay(episode.song, songs) }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: PodcastEpisode, onClick: () -> Unit) {
    val c = AppTheme.colors
    val meta = listOfNotNull(episode.dateText, episode.durationText).joinToString("  •  ")
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                listOf(AppleRed, c.surface),
                Modifier.size(88.dp),
                corner = 12.dp,
                imageUrl = episode.song.thumbnail,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    episode.song.title,
                    color = c.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (episode.song.artist.isNotBlank()) {
                    Text(
                        episode.song.artist,
                        color = c.textSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                episode.description?.takeIf(String::isNotBlank)?.let { desc ->
                    Text(
                        desc,
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 122.dp),
            thickness = 0.5.dp,
            color = c.divider,
        )
    }
}

private fun TrackSort.applyEpisodes(list: List<PodcastEpisode>): List<PodcastEpisode> = when (this) {
    TrackSort.Original -> list
    TrackSort.TitleAsc -> list.sortedBy { it.song.title.lowercase() }
    TrackSort.TitleDesc -> list.sortedByDescending { it.song.title.lowercase() }
    TrackSort.ArtistAsc -> list.sortedBy { it.song.artist.lowercase() }
}

@Composable
private fun ActionRow(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    sort: TrackSort,
    onSort: (TrackSort) -> Unit,
) {
    val c = AppTheme.colors
    var sortMenuOpen by remember { mutableStateOf(false) }
    // Tombol Putar tepat di tengah; Acak di kiri dan Urutkan di kanan dengan jarak yang sama.
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(c.textSecondary.copy(alpha = 0.15f))
                    .clickable(onClick = onShuffle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Acak",
                    tint = c.text,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Row(
            Modifier
                .width(150.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.text)
                .clickable(onClick = onPlay),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = c.background,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Putar", color = c.background, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        // Tombol urutkan dengan menu pilihan di bawahnya.
        Box(Modifier.weight(1f)) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(c.textSecondary.copy(alpha = 0.15f))
                    .clickable { sortMenuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Sort,
                    contentDescription = "Urutkan lagu",
                    tint = c.text,
                    modifier = Modifier.size(22.dp),
                )
            }
            DropdownMenu(
                expanded = sortMenuOpen,
                onDismissRequest = { sortMenuOpen = false },
            ) {
                TrackSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.label,
                                color = if (option == sort) c.text else c.textSecondary,
                                fontWeight = if (option == sort) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onSort(option)
                            sortMenuOpen = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = AppTheme.colors.text,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp),
    )
}

/** Satu baris lagu dengan cover kecil di kiri, seperti daftar lagu di aplikasi musik modern. */
@Composable
private fun TrackRow(song: Song, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                listOf(AppleRed, c.surface),
                Modifier.size(52.dp),
                corner = 6.dp,
                imageUrl = song.thumbnail,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = c.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (song.artist.isNotBlank()) {
                    Text(
                        song.artist,
                        color = c.textSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 86.dp),
            thickness = 0.5.dp,
            color = c.divider,
        )
    }
}

@Composable
private fun AlbumCard(item: LibraryItem, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.width(150.dp).clickable(onClick = onClick)) {
        Artwork(
            listOf(AppleRed, c.surface),
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            corner = 12.dp,
            imageUrl = item.thumbnail,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            color = c.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                item.subtitle,
                color = c.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
