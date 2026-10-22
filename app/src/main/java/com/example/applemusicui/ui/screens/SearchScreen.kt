package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.data.RecentSearch
import com.example.applemusicui.data.Song
import com.example.applemusicui.data.defaultRecentSearches
import com.example.applemusicui.ui.components.Artwork
import com.example.applemusicui.ui.components.Avatar
import com.example.applemusicui.ui.components.SectionHeader
import com.example.applemusicui.ui.components.SongRow
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed

@Composable
fun SearchScreen(current: Song, onPlay: (Song) -> Unit) {
    val c = AppTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(0) }
    val recents = remember { mutableStateListOf<RecentSearch>().apply { addAll(defaultRecentSearches) } }
    val focusManager: FocusManager = LocalFocusManager.current

    val results = DummyData.songs.filter {
        it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
    }

    val submit: () -> Unit = {
        val q = query.trim()
        if (q.isNotEmpty()) {
            recents.removeAll { it.title.equals(q, ignoreCase = true) }
            recents.add(0, RecentSearch(q, "Pencarian", listOf(Color(0xFF7B6CF6), Color(0xFFE040FB))))
        }
        focusManager.clearFocus()
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 200.dp),
    ) {
        item(key = "search-bar") {
            SearchBar(query = query, onQueryChange = { query = it }, onSubmit = submit)
        }
        item(key = "search-tabs") {
            SegmentedTabs(listOf("YouTube", "Library", "Addons"), tab) { tab = it }
        }

        when {
            tab == 0 && query.isBlank() -> {
                item(key = "recent-header") {
                    SectionHeader(
                        "Pencarian terbaru",
                        action = if (recents.isEmpty()) null else "Hapus",
                        actionColor = c.text,
                        onActionClick = { recents.clear() },
                    )
                }
                items(recents, key = { it.title }) { item ->
                    RecentRow(
                        item = item,
                        onClick = { query = item.title },
                        onRemove = { recents.remove(item) },
                    )
                }
            }

            tab == 0 -> {
                if (results.isEmpty()) {
                    item(key = "no-results") { EmptyNote("Tidak ada hasil untuk \"$query\"") }
                } else {
                    items(results, key = { it.title }) { song ->
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            SongRow(song, highlighted = song.title == current.title) { onPlay(song) }
                        }
                    }
                }
            }

            tab == 1 -> item(key = "library-empty") { EmptyNote("Belum ada musik di Library") }

            else -> item(key = "addons-empty") { EmptyNote("Belum ada addon terpasang") }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = c.text, fontSize = 19.sp),
                cursorBrush = SolidColor(AppleRed),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Artis, Lagu, Lirik, dan Lainnya",
                                color = c.textSecondary,
                                fontSize = 19.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Hapus teks",
                    tint = c.textSecondary,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") }
                        .padding(4.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Avatar(size = 56.dp)
    }
}

@Composable
private fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface)
            .padding(5.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) c.segmentSelected else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (isSelected) c.segmentSelectedText else c.textSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RecentRow(item: RecentSearch, onClick: () -> Unit, onRemove: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(item.art, Modifier.size(64.dp), corner = 8.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = c.text,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.subtitle,
                color = c.textSecondary,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Filled.Close,
            contentDescription = "Hapus dari riwayat",
            tint = c.textSecondary,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .padding(10.dp),
        )
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        color = AppTheme.colors.textSecondary,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
    )
}
