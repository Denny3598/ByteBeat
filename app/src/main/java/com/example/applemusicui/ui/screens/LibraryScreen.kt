package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.ui.components.AlbumCard
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.components.SectionHeader
import com.example.applemusicui.ui.theme.SurfaceDark
import com.example.applemusicui.ui.theme.TextSecondary

@Composable
fun LibraryScreen() {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 200.dp),
    ) {
        item {
            ScreenHeader("Perpustakaan") {
                Icon(Icons.Filled.History, contentDescription = "Riwayat", tint = Color.White)
            }
        }

        // Kartu statistik "Pengalaman mendengarkan Anda"
        item {
            LazyRow(
                Modifier.padding(top = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    StatCard(
                        width = screenWidth * 0.8f,
                        value = "511",
                        label = "MINUTES LISTENED",
                        footerTitle = "TOP ARTIST",
                        footerSub = "146 Plays · 2026",
                        memberSince = "09/26",
                    )
                }
                item {
                    StatCard(
                        width = screenWidth * 0.8f,
                        value = "Top Song",
                        label = "THIS MONTH",
                        footerTitle = "IT'S YOU",
                        footerSub = "Sezairi",
                        memberSince = "09/26",
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(top = 16.dp)) {
                LibraryRow(Icons.Filled.Download, "Downloads")
                HorizontalDivider(
                    Modifier.padding(start = 62.dp, end = 0.dp),
                    color = Color.White.copy(alpha = 0.12f),
                )
                LibraryRow(Icons.Filled.Folder, "Local Music")
            }
        }

        item { SectionHeader("Playlist", action = "Lihat semua") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { PlaylistTile(Icons.Filled.Add, "Playlist baru", "Disimpan ke YouTube Music") }
                item { PlaylistTile(Icons.Filled.Link, "Impor dari Spotify", "Dari tautan playlist") }
                item {
                    PlaylistTile(
                        Icons.Filled.MusicNote,
                        "Musik yang disukai",
                        "Playlist otomatis",
                        gradient = listOf(Color(0xFF8E6BF0), Color(0xFFFF5FA2)),
                    )
                }
            }
        }

        item { SectionHeader("Album") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(DummyData.albums) { AlbumCard(it, size = 130.dp) }
            }
        }
    }
}

@Composable
private fun StatCard(
    width: androidx.compose.ui.unit.Dp,
    value: String,
    label: String,
    footerTitle: String,
    footerSub: String,
    memberSince: String,
) {
    Box(
        Modifier
            .width(width)
            .height(170.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3A3236), Color(0xFF151416))))
            .padding(18.dp),
    ) {
        Text(
            "PENGALAMAN\nMENDENGARKAN ANDA",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Column(Modifier.align(Alignment.CenterStart)) {
            Text(value, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                label,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(
                footerTitle,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(footerSub, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
        }
        Column(Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.End) {
            Text(
                "ANGGOTA SEJAK",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(memberSince, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LibraryRow(icon: ImageVector, label: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 19.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun PlaylistTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>? = null,
) {
    Column(Modifier.width(130.dp)) {
        Box(
            Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (gradient != null) Brush.linearGradient(gradient)
                    else Brush.linearGradient(listOf(SurfaceDark, SurfaceDark))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            color = TextSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
