package com.example.applemusicui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.Album
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.theme.AppleRed
import com.example.applemusicui.ui.theme.GlassDark
import com.example.applemusicui.ui.theme.TextSecondary

/** Cover art placeholder berupa gradien. */
@Composable
fun Artwork(colors: List<Color>, modifier: Modifier = Modifier, corner: Dp = 8.dp) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(colors))
    )
}

@Composable
fun Avatar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(46.dp)
            .border(2.dp, AppleRed, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF4A4A4A), Color(0xFF111111)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Person,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Judul besar layar + avatar bercincin merah di kanan atas. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.weight(1f),
        )
        trailing()
        Spacer(Modifier.width(12.dp))
        Avatar()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = "Lihat Semua") {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(action, color = AppleRed, fontSize = 16.sp)
        }
    }
}

@Composable
fun SongRow(song: Song, highlighted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(song.art, Modifier.size(56.dp), corner = 6.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (highlighted) AppleRed else Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                color = TextSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = "Opsi",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun AlbumCard(album: Album, size: Dp = 150.dp) {
    Column(Modifier.width(size)) {
        Artwork(album.art, Modifier.size(size), corner = 10.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            album.title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            album.subtitle,
            color = TextSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mini player melayang dengan efek "kaca" gelap + semburat warna cover. */
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(shape)
            .background(Color(0xFF121212))
            .background(
                Brush.horizontalGradient(
                    listOf(GlassDark, song.art.first().copy(alpha = 0.30f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(song.art, Modifier.size(48.dp), corner = 8.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(song.artist, color = TextSecondary, fontSize = 15.sp, maxLines = 1)
        }
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = "Putar/jeda",
            tint = AppleRed,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggle)
                .padding(6.dp),
        )
        Icon(
            Icons.Filled.FastForward,
            contentDescription = "Berikutnya",
            tint = AppleRed,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onNext)
                .padding(6.dp),
        )
    }
}

enum class AppTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Explore("Explore", Icons.Filled.GridView),
    Library("Library", Icons.Filled.LibraryMusic),
    Search("Search", Icons.Filled.Search),
}

/** Tab bar berbentuk pil melayang; tab aktif diberi highlight + warna merah. */
@Composable
fun PillTabBar(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    val barShape = RoundedCornerShape(36.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(barShape)
            .background(GlassDark)
            .border(1.dp, Color.White.copy(alpha = 0.08f), barShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = if (isSelected) AppleRed else TextSecondary,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    tab.label,
                    color = if (isSelected) AppleRed else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
