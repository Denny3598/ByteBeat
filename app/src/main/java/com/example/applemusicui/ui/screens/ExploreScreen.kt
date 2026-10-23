package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.DummyData
import com.example.applemusicui.data.Mood
import com.example.applemusicui.ui.components.ScreenHeader

@Composable
fun ExploreScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 200.dp),
    ) {
        ScreenHeader("Jelajahi")

        // Kartu mengisi sisa tinggi layar, dibagi rata per baris.
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DummyData.moods.chunked(2).forEach { pair ->
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEach { mood ->
                        MoodCard(mood, Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoodCard(mood: Mood, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(mood.base)
            .background(
                Brush.horizontalGradient(
                    0.38f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.18f),
                ),
            )
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                mood.title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                mood.subtitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
            )
        }
    }
}
