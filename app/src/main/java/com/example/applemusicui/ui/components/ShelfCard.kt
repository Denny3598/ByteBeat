package com.example.applemusicui.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applemusicui.data.ShelfItem
import com.example.applemusicui.data.artFor
import com.example.applemusicui.ui.theme.AppTheme

@Composable
fun ShelfCard(item: ShelfItem, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.width(150.dp).clickable(onClick = onClick)) {
        Artwork(
            colors = artFor(item.title),
            modifier = Modifier.size(150.dp),
            corner = 12.dp,
            imageUrl = item.thumbnail,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            color = c.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            item.subtitle,
            color = c.textSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
