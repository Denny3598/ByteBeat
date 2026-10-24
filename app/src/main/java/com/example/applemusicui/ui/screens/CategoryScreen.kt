package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.applemusicui.data.CategorySection
import com.example.applemusicui.data.ExploreRepository
import com.example.applemusicui.data.HomeCardItem
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException

/** Identitas satu kategori yang dibuka, dibawa lewat tumpukan halaman di AppRoot. */
data class CategoryRef(val browseId: String, val params: String?, val title: String)

private const val CATEGORY_SEP = "\u0001"

/** Kunci untuk tumpukan halaman, supaya kategori bisa dibedakan dari halaman detail biasa. */
fun categoryKey(browseId: String, params: String?, title: String): String =
    listOf("cat", browseId, params.orEmpty(), title).joinToString(CATEGORY_SEP)

/** Kebalikan dari [categoryKey]. Mengembalikan null kalau kuncinya bukan kategori. */
fun parseCategoryKey(key: String): CategoryRef? {
    val parts = key.split(CATEGORY_SEP)
    if (parts.size < 4 || parts[0] != "cat") return null
    return CategoryRef(
        browseId = parts[1],
        params = parts[2].ifEmpty { null },
        title = parts.drop(3).joinToString(CATEGORY_SEP),
    )
}

private sealed interface CategoryState {
    data object Loading : CategoryState
    data class Ok(val sections: List<CategorySection>) : CategoryState
    data class Error(val message: String) : CategoryState
}

/** Halaman satu kategori (misalnya "Mood" atau "Genre"): rak-rak di dalamnya bisa dibuka satu per satu. */
@Composable
fun CategoryScreen(
    ref: CategoryRef,
    onPlay: (Song, List<Song>) -> Unit,
    onOpenPage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = AppTheme.colors
    var state by remember(ref) { mutableStateOf<CategoryState>(CategoryState.Loading) }
    var attempt by remember(ref) { mutableStateOf(0) }

    LaunchedEffect(ref, attempt) {
        state = CategoryState.Loading
        state = try {
            val sections = ExploreRepository.category(ref.browseId, ref.params)
            if (sections.isEmpty()) {
                CategoryState.Error("Kategori ini belum punya isi.")
            } else {
                CategoryState.Ok(sections)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CategoryState.Error("Gagal memuat kategori (${e.message.orEmpty()})")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(c.surface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = c.text,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                ref.title,
                color = c.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when (val s = state) {
            is CategoryState.Loading -> Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is CategoryState.Error -> Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    s.message,
                    color = c.textSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.text)
                        .clickable { attempt++ }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text("Coba lagi", color = c.background, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            is CategoryState.Ok -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 200.dp),
            ) {
                items(s.sections) { section ->
                    CategorySectionBlock(section) { item ->
                        if (item.videoId != null) {
                            val song = item.toCategorySong() ?: return@CategorySectionBlock
                            onPlay(song, section.items.mapNotNull { it.toCategorySong() })
                        } else {
                            item.browseId?.let(onOpenPage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySectionBlock(section: CategorySection, onClick: (HomeCardItem) -> Unit) {
    Column(Modifier.padding(bottom = 12.dp)) {
        if (section.title.isNotBlank()) {
            Text(
                section.title,
                color = AppTheme.colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(section.items) { item ->
                CategoryCard(item) { onClick(item) }
            }
        }
    }
}

@Composable
private fun CategoryCard(item: HomeCardItem, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column(
        Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface),
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

private fun HomeCardItem.toCategorySong(): Song? {
    val vid = videoId ?: return null
    return Song(
        title = title,
        artist = subtitle,
        art = listOf(Color(0xFFFA2D48), Color(0xFF333333)),
        url = "https://www.youtube.com/watch?v=$vid",
        thumbnail = thumbnail,
    )
}
