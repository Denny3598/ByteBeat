package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.applemusicui.data.AccountProfile
import com.example.applemusicui.data.AccountRepository
import com.example.applemusicui.ui.theme.AppTheme

/** Ruangan khusus akun: status login, masuk, keluar, dan apa saja yang ikut tersinkron. */
@Composable
fun AccountScreen(
    loggedIn: Boolean,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenLibrary: () -> Unit = {},
    onOpenLiked: () -> Unit = {},
) {
    val c = AppTheme.colors
    // Profil (nama dan foto) diambil dari YouTube Music saat sudah login.
    val profile by produceState<AccountProfile?>(null, loggedIn) {
        value = if (loggedIn) runCatching { AccountRepository.profile() }.getOrNull() else null
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
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
        Spacer(Modifier.height(16.dp))
        Text(
            "Akun",
            color = c.text,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )

        // Kartu status utama.
        Column(
            Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val photo = profile?.photo
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = c.text,
                    modifier = Modifier.size(64.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (loggedIn) profile?.name?.takeIf { it.isNotBlank() } ?: "Terhubung ke YouTube Music" else "Belum masuk",
                color = c.text,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (loggedIn) {
                    "Playlist, pustaka, dan suka kamu ikut tersinkron."
                } else {
                    "Masuk untuk memakai playlist, pustaka, dan suka dari akunmu."
                },
                color = c.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (loggedIn) {
                AccountButton(
                    icon = Icons.Filled.Logout,
                    label = "Keluar dari akun",
                    filled = false,
                    onClick = onLogout,
                )
            } else {
                AccountButton(
                    icon = Icons.Filled.Login,
                    label = "Masuk ke YouTube Music",
                    filled = true,
                    onClick = onLogin,
                )
            }
        }

        // Keterangan apa saja yang ikut akun.
        Text(
            "YANG IKUT AKUN",
            color = c.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface),
        ) {
            InfoRow(Icons.Filled.PlaylistPlay, "Playlist dan pustaka", onClick = onOpenLibrary)
            InfoRow(Icons.Filled.Favorite, "Lagu yang kamu suka", onClick = onOpenLiked)
        }

        Text(
            "Data akun hanya dipakai di aplikasi ini untuk memanggil YouTube Music.",
            color = c.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 32.dp),
        )
    }
}

@Composable
private fun AccountButton(
    icon: ImageVector,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    val bg = if (filled) c.text else c.background
    val fg = if (filled) c.background else c.text
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = c.text, fontSize = 17.sp)
    }
}
