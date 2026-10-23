package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Jelajahi", horizontalPadding = 0.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(DummyData.moods) { mood -> MoodCard(mood) }
        }
    }
}

@Composable
private fun MoodCard(mood: Mood) {
    Box(
        Modifier
            .height(88.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(mood.base)
            .background(
                Brush.horizontalGradient(
                    0.38f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.18f),
                ),
            )
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
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
