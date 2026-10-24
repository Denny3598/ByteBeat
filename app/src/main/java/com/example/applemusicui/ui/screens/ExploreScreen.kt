package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
        } catch (e: Throwable) {
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

            is ExploreState.Ok -> {
                // Dua kartu per baris, sama seperti tampilan BitChord.
                val rows = s.moods.chunked(2)
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 200.dp),
                ) {
                    items(
                        rows,
                        key = { row -> row.first().let { "${it.browseId}|${it.params.orEmpty()}" } },
                    ) { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                        ) {
                            row.forEach { mood -> MoodCard(mood, Modifier.weight(1f)) }
                            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Strip warna di kiri, cover di kanan yang dibuat duotone dari warna strip.
 * Judul di pojok kiri atas, sama seperti BitChord.
 */
@Composable
private fun MoodCard(mood: ExploreMood, modifier: Modifier = Modifier) {
    val tone = remember(mood.color) { moodTone(mood.color) }
    Box(
        modifier
            .aspectRatio(1.72f)
            .clip(RoundedCornerShape(18.dp))
            .background(tone.stripe),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.72f)
                .background(tone.placeholder),
        ) {
            mood.coverUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = tone.duotone,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Bayangan gelap di atas supaya judul tetap terbaca di warna pastel.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.24f),
                        0.62f to Color.Transparent,
                    ),
                ),
        )
        Text(
            mood.title,
            color = Color.White,
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 1f), blurRadius = 6f),
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

private class MoodTone(val stripe: Color, val placeholder: Color, val duotone: ColorFilter)

private fun moodTone(base: Color): MoodTone {
    // Warna YouTube sudah pastel; sedikit hitam menjaga supaya tidak menyala di latar gelap.
    val stripe = lerp(base, Color.Black, 0.08f)
    val shadow = lerp(base, Color.Black, 0.68f)
    val highlight = lerp(base, Color.White, 0.32f)
    return MoodTone(
        stripe = stripe,
        placeholder = lerp(shadow, highlight, 0.45f),
        duotone = duotone(shadow, highlight),
    )
}

/** Memetakan kecerahan tiap piksel ke rentang warna shadow sampai highlight. */
private fun duotone(shadow: Color, highlight: Color): ColorFilter {
    fun channel(from: Float, to: Float): FloatArray {
        val span = to - from
        return floatArrayOf(span * 0.299f, span * 0.587f, span * 0.114f, 0f, from * 255f)
    }
    return ColorFilter.colorMatrix(
        ColorMatrix(
            channel(shadow.red, highlight.red) +
                channel(shadow.green, highlight.green) +
                channel(shadow.blue, highlight.blue) +
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
        ),
    )
}
