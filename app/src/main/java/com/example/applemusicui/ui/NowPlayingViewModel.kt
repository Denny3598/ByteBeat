package com.example.applemusicui.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applemusicui.data.YouTubeMusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val title: String = "Mengambil Lagu...",
    val artist: String = "ByteBeat",
    val albumType: String = "YouTube Music",
    val imageUrl: String = "",
    val streamUrl: String = "",
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false
)

class NowPlayingViewModel(
    private val repository: YouTubeMusicRepository = YouTubeMusicRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    fun loadAndPlaySong(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val results = repository.searchSongs(query)
                val song = results.firstOrNull()
                
                if (song != null) {
                    _uiState.value = _uiState.value.copy(
                        title = song.title,
                        artist = song.artist,
                        imageUrl = song.thumbnailUrl,
                        streamUrl = song.streamUrl,
                        isPlaying = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun togglePlayPause() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }
}
