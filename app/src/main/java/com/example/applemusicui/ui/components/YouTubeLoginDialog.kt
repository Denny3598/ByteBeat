package com.example.applemusicui.ui.components

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YouTubeLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Selesai", color = Color(0xFFFF2D55))
            }
        },
        title = { Text("Login YouTube Music", color = Color.White) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    val cookies = CookieManager.getInstance().getCookie(url)
                                    if (cookies != null && cookies.contains("SAPISID")) {
                                        onLoginSuccess(cookies)
                                    }
                                }
                            }
                            settings.javaScriptEnabled = true
                            loadUrl("https://music.youtube.com")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        containerColor = Color(0xFF1C1C1E)
    )
}
