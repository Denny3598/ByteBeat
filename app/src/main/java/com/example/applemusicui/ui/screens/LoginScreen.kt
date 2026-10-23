package com.example.applemusicui.ui.screens

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.applemusicui.data.AppPrefs
import com.example.applemusicui.ui.theme.AppTheme

private const val LOGIN_URL =
    "https://accounts.google.com/ServiceLogin" +
        "?ltmpl=music&service=youtube&prompt=select_account" +
        "&continue=https%3A%2F%2Fmusic.youtube.com%2F"

// Dipakai hanya kalau user agent dari HP tidak mengandung "Chrome/".
private const val FALLBACK_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

/**
 * Menghapus penanda WebView dari user agent. Google sering menolak login
 * kalau melihat "; wv" atau "Version/4.0" (halaman "tidak dapat memproses login").
 * Versi Chrome dan model HP tetap dipertahankan, jadi tidak terlihat seperti browser palsu.
 */
internal fun loginUserAgent(platformUserAgent: String?): String {
    val stripped = platformUserAgent
        ?.replace("; wv", "")
        ?.replace("Version/4.0", "")
        ?.replace("  ", " ")
        ?.trim()
    return stripped?.takeIf { it.contains("Chrome/") } ?: FALLBACK_USER_AGENT
}

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onClose: () -> Unit) {
    val c = AppTheme.colors
    val finished = remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding(),
    ) {
        // Header: tombol kembali + judul
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(c.control)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = c.text)
            }
            Spacer(Modifier.width(16.dp))
            Text(
                "Login YouTube",
                color = c.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = loginUserAgent(settings.userAgentString)

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            // Login selesai saat Google mengarahkan kembali dan cookie SAPISID sudah ada.
                            if (!finished.value && AppPrefs.isLoggedIn()) {
                                finished.value = true
                                CookieManager.getInstance().flush()
                                onLoggedIn()
                            }
                        }
                    }

                    loadUrl(LOGIN_URL)
                }
            },
        )
    }
}
