package com.example.applemusicui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.applemusicui.data.AppPrefs
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.data.Song
import com.example.applemusicui.ui.components.AppTab
import com.example.applemusicui.ui.components.LocalOnAvatarClick
import com.example.applemusicui.ui.components.MiniPlayer
import com.example.applemusicui.ui.components.PillTabBar
import com.example.applemusicui.ui.playback.PlaybackController
import com.example.applemusicui.ui.screens.ExploreScreen
import com.example.applemusicui.ui.screens.LibraryScreen
import com.example.applemusicui.ui.screens.ListenNowScreen
import com.example.applemusicui.ui.screens.LoginScreen
import com.example.applemusicui.ui.screens.NowPlayingScreen
import com.example.applemusicui.ui.screens.SearchScreen
import com.example.applemusicui.ui.screens.SettingsScreen
import com.example.applemusicui.ui.theme.AppTheme
import com.example.applemusicui.ui.theme.ThemeMode

@Composable
fun AppRoot(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    val controller = remember { PlaybackController(context.applicationContext) }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showLogin by rememberSaveable { mutableStateOf(false) }
    var loggedIn by remember { mutableStateOf(AppPrefs.isLoggedIn()) }

    val c = AppTheme.colors
    val current = controller.current
    val highlight = current ?: DummyData.songs.first()
    val play: (Song, List<Song>) -> Unit = { song, list -> controller.play(song, list) }

    // Posisi mini player, dipakai agar cover membesar dari tempat yang sama saat pemutar dibuka.
    var miniBarBounds by remember { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current
    val coverStart: Rect? = miniBarBounds?.let { bar ->
        val pad = with(density) { 10.dp.toPx() }
        val side = with(density) { 48.dp.toPx() }
        Rect(bar.left + pad, bar.top + pad, bar.left + pad + side, bar.top + pad + side)
    }

    // Tombol kembali menutup layar paling atas dulu. Yang paling bawah didaftarkan lebih dulu.
    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showLogin) { showLogin = false }

    CompositionLocalProvider(LocalOnAvatarClick provides { showSettings = true }) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.background),
        ) {
            when (tab) {
                AppTab.Home -> ListenNowScreen(highlight, play)
                AppTab.Explore -> ExploreScreen()
                AppTab.Library -> LibraryScreen(onPlay = play)
                AppTab.Search -> SearchScreen(highlight, play)
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
                            .onGloballyPositioned { miniBarBounds = it.boundsInRoot() }
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
                PillTabBar(selected = tab, onSelect = { tab = it })
            }

            if (showNowPlaying && current != null) {
                NowPlayingScreen(
                    song = current,
                    controller = controller,
                    startRect = coverStart,
                    onClose = { showNowPlaying = false },
                )
            }

            if (showSettings) {
                SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    loggedIn = loggedIn,
                    onBack = { showSettings = false },
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
