package com.example.applemusicui.ui.screens

import android.content.SharedPreferences
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.AppPrefs
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val c = AppTheme.colors
    val prefs = AppPrefs.prefs(LocalContext.current)

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
            "Settings",
            color = c.text,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )

        SectionLabel("Kualitas audio")
        SettingsCard {
            SettingsRow(Icons.Filled.Wifi, "Di Wi-Fi", value = "Lossless")
            SettingsRow(Icons.Filled.SignalCellularAlt, "Di data seluler", value = "Rendah")
            SettingsRow(Icons.Filled.GraphicEq, "Kualitas output", value = "16-bit PCM")
        }

        SectionLabel("Unduhan")
        SettingsCard {
            SettingsRow(
                icon = Icons.Filled.Download,
                title = "Kualitas unduhan",
                subtitle = "Sekitar 35 MB per lagu",
                value = "Lossless",
            )
            SwitchRow(
                icon = Icons.Filled.Wifi,
                title = "Unduh hanya lewat Wi-Fi",
                prefs = prefs,
                key = "dl_wifi_only",
                default = true,
            )
        }

        SectionLabel("Pemutaran")
        SettingsCard {
            SwitchRow(
                icon = Icons.Filled.PlayCircle,
                title = "Utamakan versi musik",
                subtitle = "Untuk video musik, pakai versi audionya",
                prefs = prefs,
                key = "music_only",
                default = true,
            )
            SwitchRow(
                icon = Icons.Filled.VolumeUp,
                title = "Normalisasi volume",
                subtitle = "Samakan kenyaringan setiap lagu",
                prefs = prefs,
                key = "loudness",
                default = true,
            )
            SwitchRow(
                icon = Icons.Filled.Tune,
                title = "Berhenti saat ditutup dari recent apps",
                prefs = prefs,
                key = "stop_on_close",
                default = true,
            )
        }

        SectionLabel("Tampilan")
        SettingsCard {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Brightness6,
                        contentDescription = null,
                        tint = c.text,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("Tema", color = c.text, fontSize = 17.sp)
                }
                Spacer(Modifier.height(12.dp))
                ThemeSegment(current = themeMode, onSelect = onThemeModeChange)
            }
            SwitchRow(
                icon = Icons.Filled.Tune,
                title = "Cover layar penuh",
                subtitle = "Cover memenuhi pemutar, bukan kotak",
                prefs = prefs,
                key = "full_cover",
                default = true,
            )
        }

        SectionLabel("Bahasa")
        SettingsCard {
            SettingsRow(Icons.Filled.Language, "Bahasa aplikasi", value = "Indonesia")
        }

        SectionLabel("Tentang")
        SettingsCard {
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Tentang ByteBeat",
                value = "1.0.0",
                onClick = onOpenAbout,
            )
        }

        Text(
            "ByteBeat · Pemutaran berasal dari YouTube Music",
            color = c.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 32.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = AppTheme.colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 17.sp)
            if (subtitle != null) {
                Text(subtitle, color = c.textSecondary, fontSize = 14.sp)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(value, color = c.textSecondary, fontSize = 16.sp)
        }
        if (onClick != null) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    prefs: SharedPreferences,
    key: String,
    default: Boolean,
) {
    val c = AppTheme.colors
    var checked by remember { mutableStateOf(prefs.getBoolean(key, default)) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                checked = !checked
                prefs.edit().putBoolean(key, checked).apply()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 17.sp)
            if (subtitle != null) {
                Text(subtitle, color = c.textSecondary, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.text,
                checkedThumbColor = c.background,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = c.textSecondary.copy(alpha = 0.3f),
                uncheckedThumbColor = c.background,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun ThemeSegment(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.background)
            .padding(4.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val selected = mode == current
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) c.segmentSelected else Color.Transparent)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mode.shortLabel(),
                    color = if (selected) c.segmentSelectedText else c.textSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun ThemeMode.shortLabel(): String = when (this) {
    ThemeMode.System -> "Sistem"
    ThemeMode.Light -> "Terang"
    ThemeMode.Dark -> "Gelap"
}
