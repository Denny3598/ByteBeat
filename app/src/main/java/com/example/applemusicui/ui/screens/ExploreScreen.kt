package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.applemusicui.data.ExploreMood
import com.example.applemusicui.data.ExploreRepository
import com.example.applemusicui.ui.components.ScreenHeader
import com.example.applemusicui.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException

private sealed interface ExploreState {
    data object Loading : ExploreState
    data class Ok(val moods: List<ExploreMood>) : ExploreState
    data class Error(val message: String) : ExploreState
}

@Composable
fun ExploreScreen() {
    val c = AppTheme.colors
    var state by remember { mutableStateOf<ExploreState>(ExploreState.Loading) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        state = ExploreState.Loading
        state = try {
            val list = ExploreRepository.moods()
            if (list.isEmpty()) {
                ExploreState.Error("YouTube Music tidak mengirim daftar kategori.")
            } else {
                ExploreState.Ok(list)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ExploreState.Error("Gagal memuat: ${e.javaClass.simpleName} ${e.message.orEmpty()}")
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Jelajahi")
        Spacer(Modifier.height(12.dp))

        when (val s = state) {
            is ExploreState.Loading -> Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is ExploreState.Error -> Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    s.message,
                    color = c.textSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.text)
                        .clickable { attempt++ }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text("Coba lagi", color = c.background, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            is ExploreState.Ok -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 200.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(s.moods, key = { it.browseId }) { mood ->
                    MoodCard(mood)
                }
            }
        }
    }
}

/** Kartu kategori: warna strip di kiri memudar ke cover di kanan. */
@Composable
private fun MoodCard(mood: ExploreMood, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.8f)
            .clip(RoundedCornerShape(20.dp))
            .background(mood.color),
    ) {
        if (mood.coverUrl != null) {
            AsyncImage(
                model = mood.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to mood.color,
                            0.42f to mood.color,
                            0.7f to mood.color.copy(alpha = 0.45f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }
        Text(
            mood.title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
        )
    }
}
