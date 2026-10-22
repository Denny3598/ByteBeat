package com.example.applemusicui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.example.applemusicui.ui.NowPlayingScreen
import com.example.applemusicui.ui.NowPlayingViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NowPlayingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    var isNowPlayingOpen by remember { mutableStateOf(false) }

                    // Tes memuat lagu otomatis saat dibuka
                    LaunchedEffect(Unit) {
                        viewModel.loadAndPlaySong("TWICE Alcohol Free")
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
                                            text = uiState.title,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = uiState.artist,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.togglePlayPause() }
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
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
                                title = uiState.title,
                                artist = uiState.artist,
                                albumType = uiState.albumType,
                                imageUrl = uiState.imageUrl,
                                isPlaying = uiState.isPlaying,
                                onPlayPauseToggle = { viewModel.togglePlayPause() },
                                onDismiss = { isNowPlayingOpen = false }
                            )
                        }

                    }
                }
            }
        }
    }
}
