package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    var downloadWifiOnly by remember { mutableStateOf(true) }
    var preferMusicOnly by remember { mutableStateOf(true) }
    var loudnessNormalization by remember { mutableStateOf(true) }
    var fullScreenCover by remember { mutableStateOf(true) }
    var animatedCover by remember { mutableStateOf(true) }
    var syncedLyrics by remember { mutableStateOf(true) }
    var filterShortAudio by remember { mutableStateOf(true) }
    var selectedTheme by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Settings", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1C1C1E)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search settings", color = Color.Gray, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ACCOUNT & INTEGRATIONS
            SettingsGroupCard {
                SettingsItemRow(
                    icon = Icons.Default.Person,
                    title = "Account & integrations",
                    subtitle = "Login YouTube Music (@dennydamaraofficial)",
                    onClick = onLoginClick
                )
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsItemRow(
                    icon = Icons.Default.Share,
                    title = "Listen together",
                    subtitle = "Share a code and play the same music, in time"
                )
            }

            // AUDIO QUALITY
            SectionTitle("AUDIO QUALITY")
            SettingsGroupCard {
                SettingsItemRow(
                    icon = Icons.Default.Extension,
                    title = "Sources",
                    subtitle = "Where audio comes from and the order used"
                )
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsItemRow(icon = Icons.Default.Wifi, title = "On Wi-Fi", valueText = "Lossless")
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsItemRow(icon = Icons.Default.SignalCellularAlt, title = "On mobile data", valueText = "Low")
            }

            // DOWNLOADS
            SectionTitle("DOWNLOADS")
            SettingsGroupCard {
                SettingsItemRow(
                    icon = Icons.Default.Download,
                    title = "Download quality",
                    subtitle = "~35 MB per track, whatever the connection",
                    valueText = "Lossless"
                )
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsToggleRow(
                    title = "Download over Wi-Fi only",
                    checked = downloadWifiOnly,
                    onCheckedChange = { downloadWifiOnly = it }
                )
            }

            // PLAYBACK
            SectionTitle("PLAYBACK")
            SettingsGroupCard {
                SettingsToggleRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Prefer music-only version",
                    subtitle = "For music videos, show video details while loading audio",
                    checked = preferMusicOnly,
                    onCheckedChange = { preferMusicOnly = it }
                )
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = "Loudness normalization",
                    subtitle = "Levels every track to the same loudness",
                    checked = loudnessNormalization,
                    onCheckedChange = { loudnessNormalization = it }
                )
            }

            // APPEARANCE
            SectionTitle("APPEARANCE")
            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Theme", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTheme == 0,
                            onClick = { selectedTheme = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("System") }
                        SegmentedButton(
                            selected = selectedTheme == 1,
                            onClick = { selectedTheme = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Light") }
                        SegmentedButton(
                            selected = selectedTheme == 2,
                            onClick = { selectedTheme = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Dark") }
                    }
                }
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsToggleRow(
                    icon = Icons.Default.AspectRatio,
                    title = "Full-screen cover art",
                    subtitle = "Runs cover to edges of player",
                    checked = fullScreenCover,
                    onCheckedChange = { fullScreenCover = it }
                )
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsToggleRow(
                    icon = Icons.Default.MotionPhotosAuto,
                    title = "Animated cover art",
                    subtitle = "Plays looping video instead of still sleeve",
                    checked = animatedCover,
                    onCheckedChange = { animatedCover = it }
                )
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsToggleRow(
                    icon = Icons.Default.FormatAlignLeft,
                    title = "Synced lyrics",
                    subtitle = "Lights up words on player as sung",
                    checked = syncedLyrics,
                    onCheckedChange = { syncedLyrics = it }
                )
            }

            // LOCAL MUSIC & STORAGE
            SectionTitle("LOCAL MUSIC & STORAGE")
            SettingsGroupCard {
                SettingsItemRow(icon = Icons.Default.Folder, title = "Local music folder", subtitle = "All audio folders")
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsToggleRow(
                    icon = Icons.Default.FilterList,
                    title = "Filter non-music audio",
                    subtitle = "Hides clips under 30s & recordings",
                    checked = filterShortAudio,
                    onCheckedChange = { filterShortAudio = it }
                )
                HorizontalDivider(color = Color(0xFF2C2C2E))
                SettingsItemRow(icon = Icons.Default.CleaningServices, title = "Clear song cache", subtitle = "Frees space used by audio")
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column { content() }
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    valueText: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
        }
        if (valueText != null) {
            Text(valueText, color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF2D55)
            )
        )
    }
}
