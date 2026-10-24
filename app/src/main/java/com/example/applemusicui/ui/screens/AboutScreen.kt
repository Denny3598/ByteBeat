package com.example.applemusicui.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.UpdateInfo
import com.example.applemusicui.data.UpdateRepository
import com.example.applemusicui.ui.theme.AppTheme
import kotlinx.coroutines.launch

/** Halaman tentang aplikasi: logo, deskripsi, tautan media sosial, cek pembaruan, dan ucapan terima kasih. */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onUpdateFound: (UpdateInfo) -> Unit,
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf("Tap to check for the latest build") }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    fun checkForUpdates() {
        if (checking) return
        checking = true
        updateStatus = "Checking…"
        scope.launch {
            val result = runCatching { UpdateRepository.newerRelease() }
            checking = false
            updateStatus = when {
                result.isFailure -> "Couldn't check. Try again later."
                result.getOrNull() == null -> "You're up to date"
                else -> {
                    onUpdateFound(result.getOrNull()!!)
                    "New version available"
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
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

        // Logo dan deskripsi.
        Spacer(Modifier.height(16.dp))
        AboutCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ByteBeatLogo()
                Spacer(Modifier.height(20.dp))
                Text(
                    "ByteBeat is a sleek Android music streaming app powered by YouTube Music, designed with an elegant Apple Music-inspired UI.",
                    color = c.text,
                    fontSize = 17.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Cek pembaruan.
        Spacer(Modifier.height(16.dp))
        AboutCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !checking) { checkForUpdates() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = c.text, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Check for updates", color = c.text, fontSize = 17.sp)
                    Text(updateStatus, color = c.textSecondary, fontSize = 14.sp)
                }
            }
        }

        // Tautan media sosial.
        Spacer(Modifier.height(16.dp))
        AboutCard {
            Text("Subscribe to Denny!", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Music, updates and more", color = c.textSecondary, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            SocialRow(
                icon = Icons.Filled.MusicNote,
                color = Color.Black,
                title = "TikTok",
                handle = "@dennydamara98",
                onClick = { openUrl("https://www.tiktok.com/@dennydamara98?_r=1&_t=ZS-9Aok1Rozl95") },
            )
            Divider()
            SocialRow(
                icon = Icons.Filled.CameraAlt,
                color = Color(0xFFE1306C),
                title = "Instagram",
                handle = "@its.denny1998",
                onClick = { openUrl("https://www.instagram.com/its.denny1998?cplk=NTJwMXZjemRtOTRk") },
            )
            Divider()
            SocialRow(
                icon = Icons.Filled.AlternateEmail,
                color = Color(0xFF111111),
                title = "Threads",
                handle = "@its.denny1998",
                onClick = { openUrl("https://www.threads.com/@its.denny1998") },
            )
            Divider()
            SocialRow(
                icon = Icons.Filled.Code,
                color = Color(0xFF24292F),
                title = "GitHub",
                handle = "Denny3598",
                onClick = { openUrl("https://github.com/Denny3598") },
            )
        }

        // Ucapan terima kasih.
        Spacer(Modifier.height(16.dp))
        AboutCard {
            Text("Thanks", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Special thanks to", color = c.textSecondary, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x33FA2D48))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Claude AI", color = Color(0xFFB0102A), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            "Version 1.0.0",
            color = c.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp),
        )
    }
}

/** Logo ByteBeat: kotak gradien dengan lima batang gelombang suara. Digambar langsung, tanpa file gambar. */
@Composable
fun ByteBeatLogo() {
    Box(
        Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFFFF2D55), Color(0xFFFF9500))),
            ),
    ) {
        Canvas(Modifier.fillMaxSize().padding(24.dp)) {
            val heights = listOf(0.4f, 0.75f, 1f, 0.6f, 0.85f)
            val gap = size.width * 0.07f
            val barW = (size.width - gap * (heights.size - 1)) / heights.size
            heights.forEachIndexed { i, h ->
                val barH = size.height * h
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(i * (barW + gap), (size.height - barH) / 2f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f),
                )
            }
        }
    }
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AppTheme.colors.surface)
            .padding(20.dp),
    ) {
        content()
    }
}

@Composable
private fun SocialRow(
    icon: ImageVector,
    color: Color,
    title: String,
    handle: String,
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(handle, color = c.textSecondary, fontSize = 14.sp)
        }
        Icon(
            Icons.Filled.OpenInNew,
            contentDescription = "Buka",
            tint = c.textSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppTheme.colors.textSecondary.copy(alpha = 0.2f)),
    )
}
