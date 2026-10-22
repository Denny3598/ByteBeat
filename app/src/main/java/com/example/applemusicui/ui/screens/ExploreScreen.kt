package com.example.applemusicui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CategoryItem(val title: String, val color: Color)

@Composable
fun ExploreScreen() {
    val categories = listOf(
        CategoryItem("Bepergian", Color(0xFFD97706)),
        CategoryItem("Fokus", Color(0xFF4B5563)),
        CategoryItem("Gaming", Color(0xFF374151)),
        CategoryItem("Merasa senang", Color(0xFF10B981)),
        CategoryItem("Olah Raga", Color(0xFFEA580C)),
        CategoryItem("Pesta", Color(0xFF8B5CF6)),
        CategoryItem("Romantis", Color(0xFFDC2626)),
        CategoryItem("Santai", Color(0xFF2563EB)),
        CategoryItem("Sedih", Color(0xFF475569)),
        CategoryItem("Semangat!", Color(0xFFCA8A04)),
        CategoryItem("Tidur", Color(0xFF6B21A8)),
        CategoryItem("Afrika", Color(0xFF15803D))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Jelajahi", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(categories) { category ->
                Box(
                    modifier = Modifier
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(category.color)
                        .padding(12.dp)
                ) {
                    Text(
                        text = category.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }
        }
    }
}
