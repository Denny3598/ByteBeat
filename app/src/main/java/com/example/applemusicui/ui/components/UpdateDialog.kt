package com.example.applemusicui.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.applemusicui.data.UpdateInfo
import com.example.applemusicui.data.UpdateRepository
import com.example.applemusicui.ui.theme.AppTheme

/** Popup saat ada build baru: tombol Unduh memulai unduhan APK, Nanti menutup popup. */
@Composable
fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text("Versi baru tersedia", color = c.text, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "Build terbaru ByteBeat sudah siap. Unduhannya berjalan di latar belakang, " +
                    "lalu ketuk notifikasi unduhan untuk memasangnya.",
                color = c.textSecondary,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    UpdateRepository.download(context, info)
                    onDismiss()
                },
            ) {
                Text("Unduh", color = c.text, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nanti", color = c.textSecondary)
            }
        },
    )
}
