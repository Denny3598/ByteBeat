package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.HomeCardItem
import com.example.applemusicui.data.HomeRepository
import com.example.applemusicui.data.HomeShelf
import com.example.applemusicui.data.LibraryRepository
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.components.SectionHeader
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed
import kotlinx.coroutines.launch

@Composable
fun ListenNowScreen(current: Song, onPlay: (Song, List<Song>) -> Unit) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    var shelves by remember { mutableStateOf<List<HomeShelf>?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        shelves = runCatching { HomeRepository.shelves() }.getOrDefault(emptyList())
    }

    fun open(item: HomeCardItem, shelf: HomeShelf) {
        if (item.videoId != null) {
            val song = item.toSong() ?: return
            val songs = shelf.items.mapNotNull { it.toSong() }
            onPlay(song, songs)
            return
        }
        val browseId = item.browseId ?: return
        if (busyId != null) return
        busyId = browseId
        scope.launch {
            val songs = runCatching { LibraryRepository.tracks(browseId) }.getOrDefault(emptyList())
            busyId = null
            songs.firstOrNull()?.let { onPlay(it, songs) }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Listen Now")

        val list = shelves
        when {
            list == null -> Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            list.isEmpty() -> Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Beranda belum bisa dimuat. Coba buka lagi nanti.",
                    color = c.textSecondary,
                    fontSize = 15.sp,
                )
            }

            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 200.dp),
            ) {
                list.forEachIndexed { index, shelf ->
                    item(key = "shelf-$index-${shelf.title}") {
                        if (index == 0) {
                            HeroShelf(shelf) { item -> open(item, shelf) }
                        } else {
                            ShelfRow(shelf) { item -> open(item, shelf) }
                        }
                    }
                }
            }
        }
    }
}

/** Rak pertama: kartu besar dengan judul di atas gradasi gelap, seperti Listen Now di BitChord. */
@Composable
private fun HeroShelf(shelf: HomeShelf, onClick: (HomeCardItem) -> Unit) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(title = shelf.title, action = null)
        BoxWithConstraints {
            val cardWidth = maxWidth * 0.78f
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(shelf.items) { item ->
                    HeroCard(item, Modifier.width(cardWidth)) { onClick(item) }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(item: HomeCardItem, modifier: Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Artwork(
            listOf(AppleRed, c.surface),
            Modifier.fillMaxSize(),
            corner = 0.dp,
            imageUrl = item.thumbnail,
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                    ),
                )
                .padding(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 14.dp),
        ) {
            Text(
                item.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    item.subtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Rak biasa: judul di atas, lalu deretan kartu persegi yang bisa digeser. */
@Composable
private fun ShelfRow(shelf: HomeShelf, onClick: (HomeCardItem) -> Unit) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(title = shelf.title, action = null)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shelf.items) { item ->
                SquareCard(item) { onClick(item) }
            }
        }
    }
}

@Composable
private fun SquareCard(item: HomeCardItem, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column(
        Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
    ) {
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                item.subtitle,
                color = c.textSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun HomeCardItem.toSong(): Song? {
    val vid = videoId ?: return null
    return Song(
        title = title,
        artist = subtitle,
        art = listOf(AppleRed, Color(0xFF333333)),
        url = "https://www.youtube.com/watch?v=$vid",
        thumbnail = thumbnail,
    )
}
