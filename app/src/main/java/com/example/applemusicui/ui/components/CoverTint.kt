package com.example.applemusicui.ui.components

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Warna dominan dari cover, dipakai sebagai latar halaman album/playlist.
 * Null selama belum siap, atau kalau cover gagal dimuat.
 */
@Composable
fun rememberCoverTint(url: String?): Color? {
    val context = LocalContext.current
    val tint by produceState<Color?>(null, url) {
        value = null
        if (url.isNullOrBlank()) return@produceState
        value = try {
            withContext(Dispatchers.IO) { dominantColor(context, url) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    return tint
}

private suspend fun dominantColor(context: Context, url: String): Color? {
    // Ukuran kecil saja: cukup untuk mengambil warna, dan tidak membebani memori.
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(96)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return null
    val palette = Palette.from(bitmap).clearFilters().generate()
    val swatch = palette.vibrantSwatch
        ?: palette.dominantSwatch
        ?: palette.mutedSwatch
        ?: return null
    return Color(swatch.rgb).copy(alpha = 1f)
}
