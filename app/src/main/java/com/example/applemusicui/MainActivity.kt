package com.example.applemusicui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.applemusicui.data.YouTubeMusicRepository
import com.example.applemusicui.ui.NowPlayingScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi ExoPlayer untuk pemutaran audio
        player = ExoPlayer.Builder(this).build()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()

                    var isNowPlayingOpen by remember { mutableStateOf(false) }
                    var isPlaying by remember { mutableStateOf(false) }

                    var songTitle by remember { mutableStateOf("Mencari lagu...") }
                    var songArtist by remember { mutableStateOf("YouTube Music") }
                    var coverUrl by remember { mutableStateOf("") }

                    // Integrasi YouTube Music + Audio Playback Sekaligus
                    LaunchedEffect(Unit) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val songs = YouTubeMusicRepository.searchSongs("TWICE Alcohol Free")
                                val song = songs.firstOrNull()

                                if (song != null) {
                                    // Mengambil URL stream audio langsung
                                    val audioStreamUrl = try {
                                        YouTubeMusicRepository.audioUrl(song.url)
                                    } catch (e: Exception) {
                                        ""
                                    }

                                    withContext(Dispatchers.Main) {
                                        songTitle = song.title
                                        songArtist = song.artist
                                        coverUrl = song.thumbnail ?: "" // Penanganan Null Safety (String? -> String)

                                        if (audioStreamUrl.isNotEmpty()) {
                                            val mediaItem = MediaItem.fromUri(audioStreamUrl)
                                            player?.setMediaItem(mediaItem)
                                            player?.prepare()
                                            player?.play()
                                            isPlaying = true
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        songTitle = "Alcohol-Free"
                                        songArtist = "TWICE"
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    songTitle = "Alcohol-Free"
                                    songArtist = "TWICE"
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {

                        // KONTEN UTAMA
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "ByteBeat Player",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { isNowPlayingOpen = true }) {
                                Text("Buka Full Screen Player")
                            }
                        }

                        // MINI PLAYER (BOTTOM BAR)
                        if (!isNowPlayingOpen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 16.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF222222))
                                    .clickable { isNowPlayingOpen = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = songTitle,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = songArtist,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            isPlaying = !isPlaying
                                            if (isPlaying) player?.play() else player?.pause()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // FULL SCREEN NOW PLAYING
                        AnimatedVisibility(
                            visible = isNowPlayingOpen,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            NowPlayingScreen(
                                title = songTitle,
                                artist = songArtist,
                                albumType = "YouTube Music",
                                imageUrl = coverUrl,
                                isPlaying = isPlaying,
                                onPlayPauseToggle = {
                                    isPlaying = !isPlaying
                                    if (isPlaying) player?.play() else player?.pause()
                                },
                                onDismiss = { isNowPlayingOpen = false }
                            )
                        }

                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
