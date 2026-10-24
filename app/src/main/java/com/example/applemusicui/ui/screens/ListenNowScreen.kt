package com.example.applemusicui.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.HomeCardItem
import com.example.applemusicui.data.HomeRepository
import com.example.applemusicui.data.HomeShelf
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.components.SongMenuHost
import com.example.applemusicui.ui.components.SongMenuStyle
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed

@Composable
fun ListenNowScreen(
    current: Song,
    onPlay: (Song, List<Song>) -> Unit,
    onOpenPage: (String) -> Unit,
) {
    val c = AppTheme.colors
    var shelves by remember { mutableStateOf<List<HomeShelf>?>(null) }
    var token by remember { mutableStateOf<String?>(null) }
    var loadingMore by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Halaman pertama.
    LaunchedEffect(Unit) {
        val page = runCatching { HomeRepository.firstPage() }.getOrNull()
        shelves = page?.shelves ?: emptyList()
        token = page?.continuation
    }

    // Rak berikutnya dimuat saat pengguna menggulir mendekati akhir daftar, seperti BitChord.
    LaunchedEffect(Unit) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                val count = shelves?.size ?: 0
                val t = token
                if (t != null && !loadingMore && count > 0 && last >= count - 2) {
                    loadingMore = true
                    val page = runCatching { HomeRepository.nextPage(t) }.getOrNull()
                    if (page != null) {
                        shelves = shelves.orEmpty() + page.shelves
                        token = page.continuation
                    } else {
                        token = null
                    }
                    loadingMore = false
                }
            }
    }

    // Lagu langsung diputar; album, playlist, dan artis dibuka halamannya.
    fun open(item: HomeCardItem, shelf: HomeShelf) {
        if (item.videoId != null) {
            val song = item.toSong() ?: return
            val songs = shelf.items.mapNotNull { it.toSong() }
            onPlay(song, songs)
            return
        }
        val browseId = item.browseId ?: return
        onOpenPage(browseId)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Listen Now")

        val list = shelves
        when {
            list == null -> SkeletonHome(Modifier.weight(1f))

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
                state = listState,
                contentPadding = PaddingValues(bottom = 200.dp),
            ) {
                list.forEachIndexed { index, shelf ->
                    item(key = "shelf-$index-${shelf.title}") {
                        if (index == 0) {
                            RecentsShelf(shelf, current) { item -> open(item, shelf) }
                        } else {
                            SquareShelf(shelf) { item -> open(item, shelf) }
                        }
                    }
                }
                if (loadingMore) {
                    item(key = "loading-more") {
                        CircularProgressIndicator(
                            color = c.textSecondary,
                            modifier = Modifier
                                .padding(vertical = 24.dp)
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfTitle(title: String, subtitle: String) {
    val c = AppTheme.colors
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp)) {
        Text(
            title,
            color = c.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle.uppercase(),
                color = c.textSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Rak pertama: daftar baris, 4 lagu per halaman, bisa digeser ke samping. */
@Composable
private fun RecentsShelf(shelf: HomeShelf, current: Song, onClick: (HomeCardItem) -> Unit) {
    Column(Modifier.padding(bottom = 8.dp)) {
        ShelfTitle(shelf.title, shelf.subtitle)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(shelf.items.chunked(4)) { page ->
                Column(Modifier.fillParentMaxWidth(0.9f)) {
                    page.forEach { item ->
                        ListRow(item, highlighted = isCurrent(item, current)) { onClick(item) }
                    }
                }
            }
        }
    }
}

/** Baris lagu di Home. Ditahan = popup aksi; titik tiga = sheet aksi. Album/playlist tidak punya menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListRow(item: HomeCardItem, highlighted: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val song = item.toSong()
    var menu by remember { mutableStateOf<SongMenuStyle?>(null) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { if (song != null) menu = SongMenuStyle.Popup },
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                listOf(AppleRed, c.surface),
                Modifier.size(56.dp),
                corner = 6.dp,
                imageUrl = item.thumbnail,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = if (highlighted) AppleRed else c.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        item.subtitle,
                        color = c.textSecondary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (song != null) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Opsi",
                    tint = c.textSecondary,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { menu = SongMenuStyle.Sheet }
                        .padding(4.dp),
                )
            }
        }
        val open = menu
        if (song != null && open != null) {
            SongMenuHost(song = song, style = open, onDismiss = { menu = null })
        }
    }
}

/** Rak biasa: deretan kartu persegi besar dengan judul dan subjudul. */
@Composable
private fun SquareShelf(shelf: HomeShelf, onClick: (HomeCardItem) -> Unit) {
    Column(Modifier.padding(bottom = 16.dp)) {
        ShelfTitle(shelf.title, shelf.subtitle)
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
            .width(170.dp)
            .clickable(onClick = onClick),
    ) {
        Artwork(
            listOf(AppleRed, c.surface),
            Modifier
                .fillMaxWidth()
                .height(170.dp),
            corner = 14.dp,
            imageUrl = item.thumbnail,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            item.title,
            color = c.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
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

private fun isCurrent(item: HomeCardItem, current: Song): Boolean {
    val vid = item.videoId ?: return false
    return current.url.contains("v=$vid")
}

/** Kerangka abu-abu saat beranda dimuat, dengan kilau yang berjalan (seperti di video). */
@Composable
private fun SkeletonHome(modifier: Modifier) {
    val c = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    val block = c.textSecondary.copy(alpha = alpha)
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        SkeletonBar(block, 120.dp, 22.dp)
        Spacer(Modifier.height(14.dp))
        repeat(4) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)).background(block))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    SkeletonBar(block, 180.dp, 14.dp)
                    Spacer(Modifier.height(8.dp))
                    SkeletonBar(block, 120.dp, 12.dp)
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        SkeletonBar(block, 170.dp, 22.dp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(2) {
                Column(Modifier.width(170.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(block),
                    )
                    Spacer(Modifier.height(10.dp))
                    SkeletonBar(block, 130.dp, 14.dp)
                }
            }
        }
    }
}

@Composable
private fun SkeletonBar(color: Color, width: Dp, height: Dp) {
    Box(
        Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(color),
    )
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
