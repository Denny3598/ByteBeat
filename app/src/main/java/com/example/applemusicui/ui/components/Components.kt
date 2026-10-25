package com.example.applemusicui.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.applemusicui.data.Album
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.AppleRed
import com.example.applemusicui.ui.theme.ThemeMode

/** Dipanggil saat avatar ditekan (sekarang membuka halaman Settings). */
val LocalOnAvatarClick = staticCompositionLocalOf<() -> Unit> { {} }

/** Cover art: gradien sebagai cadangan, lalu gambar asli di atasnya kalau ada. */
@Composable
fun Artwork(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    corner: Dp = 8.dp,
    imageUrl: String? = null,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(colors))
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun Avatar(modifier: Modifier = Modifier, size: Dp = 46.dp) {
    val c = AppTheme.colors
    val onClick = LocalOnAvatarClick.current
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(c.glass)
            .border(1.dp, c.glassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = "Pengaturan",
            tint = c.text,
            modifier = Modifier.size(size / 2),
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
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = c.text,
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
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = "Lihat Semua",
    actionColor: Color = AppleRed,
    onActionClick: (() -> Unit)? = null,
) {
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = c.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                action,
                color = actionColor,
                fontSize = 16.sp,
                modifier = Modifier.clickable(enabled = onActionClick != null) {
                    onActionClick?.invoke()
                },
            )
        }
    }
}

/** Baris lagu. Ditahan = popup aksi; titik tiga = sheet aksi. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(song: Song, highlighted: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    var menu by remember { mutableStateOf<SongMenuStyle?>(null) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menu = SongMenuStyle.Popup },
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(song.art, Modifier.size(56.dp), corner = 6.dp, imageUrl = song.thumbnail)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = if (highlighted) AppleRed else c.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    color = c.textSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
        menu?.let { style ->
            SongMenuHost(song = song, style = style, onDismiss = { menu = null })
        }
    }
}

@Composable
fun AlbumCard(album: Album, size: Dp = 150.dp) {
    val c = AppTheme.colors
    Column(Modifier.width(size)) {
        Artwork(album.art, Modifier.size(size), corner = 10.dp, imageUrl = album.thumbnail)
        Spacer(Modifier.height(8.dp))
        Text(
            album.title,
            color = c.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            album.subtitle,
            color = c.textSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mini player melayang, transparan seperti tab bar di bawah. */
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(shape)
            .background(c.glass)
            .border(1.dp, c.glassBorder, shape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(song.art, Modifier.size(48.dp), corner = 8.dp, imageUrl = song.thumbnail)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = c.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                color = c.textSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = "Putar/jeda",
            tint = c.control,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggle)
                .padding(6.dp),
        )
        Icon(
            Icons.Filled.FastForward,
            contentDescription = "Berikutnya",
            tint = c.control,
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

/** Tab bar berbentuk pil melayang; tab aktif diberi highlight. */
@Composable
fun PillTabBar(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val barShape = RoundedCornerShape(36.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(barShape)
            .background(c.glass)
            .border(1.dp, c.glassBorder, barShape),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val tabWidth = maxWidth / AppTab.entries.size
            val selectedIndex = AppTab.entries.indexOf(selected)
            // Pil aktif bergeser dengan pegas ke tab yang dipilih, tidak langsung berpindah.
            val pillX by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                label = "tabPill",
            )

            Box(
                Modifier
                    .offset(x = pillX)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(30.dp))
                    .background(c.selectedPill),
            )

            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppTab.entries.forEach { tab ->
                    val isSelected = tab == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
                            tint = if (isSelected) c.control else c.textSecondary,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            tab.label,
                            color = if (isSelected) c.control else c.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

/** Dialog pilihan tema (tidak lagi dipakai; tema sekarang ada di Settings). */
@Composable
fun ThemeDialog(current: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        titleContentColor = c.text,
        textContentColor = c.text,
        title = { Text("Tampilan") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == current,
                            onClick = { onSelect(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppleRed,
                                unselectedColor = c.textSecondary,
                            ),
                        )
                        Text(mode.label, color = c.text, fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai", color = AppleRed) }
        },
    )
}
