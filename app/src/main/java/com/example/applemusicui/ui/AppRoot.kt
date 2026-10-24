package com.example.applemusicui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.AppPrefs
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.AppTab
import com.example.applemusicui.ui.components.LocalOnAvatarClick
import com.example.applemusicui.ui.components.LocalPlaybackController
import com.example.applemusicui.ui.components.MiniPlayer
import com.example.applemusicui.ui.components.PillTabBar
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.screens.DetailPageScreen
import com.example.applemusicui.ui.screens.ExploreScreen
import com.example.applemusicui.ui.screens.LibraryScreen
import com.example.applemusicui.ui.screens.ListenNowScreen
import com.example.applemusicui.ui.screens.AccountScreen
import com.example.applemusicui.ui.screens.LoginScreen
import com.example.applemusicui.ui.screens.NowPlayingScreen
import com.example.applemusicui.ui.screens.SearchScreen
import com.example.applemusicui.ui.screens.SettingsScreen
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.ThemeMode

@Composable
fun AppRoot(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    // Satu controller untuk seluruh proses; tidak dilepas saat UI ditutup, supaya antrean tetap jalan.
    val controller = remember { PlaybackController.get(context.applicationContext) }

    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showAvatarMenu by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAccount by rememberSaveable { mutableStateOf(false) }
    var showLogin by rememberSaveable { mutableStateOf(false) }
    var loggedIn by remember { mutableStateOf(AppPrefs.isLoggedIn()) }

    // Tumpukan halaman detail (album/artis). Halaman paling atas adalah yang terakhir.
    val pages = remember { mutableStateListOf<String>() }
    val openPage: (String) -> Unit = { id -> pages.add(id) }
    val closePage: () -> Unit = { if (pages.isNotEmpty()) pages.removeAt(pages.lastIndex) }

    val c = AppTheme.colors
    val current = controller.current
    val highlight = current ?: DummyData.songs.first()
    val play: (Song, List<Song>) -> Unit = { song, list -> controller.play(song, list) }

    // Tombol kembali menutup layar paling atas dulu. Yang paling bawah didaftarkan lebih dulu.
    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
    BackHandler(enabled = showAvatarMenu) { showAvatarMenu = false }
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showAccount) { showAccount = false }
    BackHandler(enabled = showLogin) { showLogin = false }
    BackHandler(enabled = pages.isNotEmpty()) { closePage() }

    CompositionLocalProvider(
        LocalOnAvatarClick provides { showAvatarMenu = true },
        LocalPlaybackController provides controller,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.background),
        ) {
            when (tab) {
                AppTab.Home -> ListenNowScreen(highlight, play, openPage)
                AppTab.Explore -> ExploreScreen()
                AppTab.Library -> LibraryScreen(onPlay = play, onOpenPage = openPage)
                AppTab.Search -> SearchScreen(highlight, play, openPage)
            }

            // Halaman detail menutupi konten tab, tapi mini player dan tab bar tetap di atasnya.
            pages.lastOrNull()?.let { id ->
                key(id) {
                    DetailPageScreen(
                        browseId = id,
                        onPlay = play,
                        onOpenPage = openPage,
                        onBack = closePage,
                    )
                }
            }

            // Mini player + tab bar melayang di atas konten.
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (current != null) {
                    // Ketuk area mini player (bukan tombolnya) untuk membuka pemutar penuh.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showNowPlaying = true },
                    ) {
                        MiniPlayer(
                            song = current,
                            isPlaying = controller.isPlaying,
                            onToggle = { controller.togglePlay() },
                            onNext = { controller.next() },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                PillTabBar(
                    selected = tab,
                    onSelect = {
                        // Ganti tab menutup halaman detail yang sedang terbuka.
                        pages.clear()
                        tab = it
                    },
                )
            }

            // Menu dari titik tiga di kanan atas: Akun dan Pengaturan.
            if (showAvatarMenu) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showAvatarMenu = false },
                ) {
                    Column(
                        Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 64.dp, end = 16.dp)
                            .width(230.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.surface)
                            .padding(vertical = 6.dp),
                    ) {
                        AvatarMenuItem(Icons.Filled.AccountCircle, "Akun") {
                            showAvatarMenu = false
                            showAccount = true
                        }
                        AvatarMenuItem(Icons.Filled.Settings, "Pengaturan") {
                            showAvatarMenu = false
                            showSettings = true
                        }
                    }
                }
            }

            if (showNowPlaying && current != null) {
                NowPlayingScreen(
                    song = current,
                    controller = controller,
                    onClose = { showNowPlaying = false },
                )
            }

            if (showSettings) {
                SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    loggedIn = loggedIn,
                    onBack = { showSettings = false },
                    onOpenAccount = { showAccount = true },
                )
            }

            if (showAccount) {
                AccountScreen(
                    loggedIn = loggedIn,
                    onBack = { showAccount = false },
                    onLogin = { showLogin = true },
                    onLogout = {
                        AppPrefs.logout()
                        loggedIn = false
                    },
                )
            }

            if (showLogin) {
                LoginScreen(
                    onLoggedIn = {
                        loggedIn = AppPrefs.isLoggedIn()
                        showLogin = false
                    },
                    onClose = { showLogin = false },
                )
            }
        }
    }
}

@Composable
private fun AvatarMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
