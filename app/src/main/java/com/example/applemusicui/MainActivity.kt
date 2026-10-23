package com.example.applemusicui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.applemusicui.ui.components.*
import com.example.applemusicui.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ByteBeatMainApp()
            }
        }
    }
}

enum class BottomTab(val title: String) {
    HOME("Home"),
    EXPLORE("Explore"),
    LIBRARY("Library"),
    SEARCH("Search")
}

@Composable
fun ByteBeatMainApp() {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
    var showSettings by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showSettings) {
                SettingsScreen(
                    onBackClick = { showSettings = false },
                    onLoginClick = { showLoginDialog = true }
                )
            } else {
                Scaffold(
                    topBar = {
                        MainTopAppBar(onProfileClick = { showSettings = true })
                    },
                    bottomBar = {
                        GlassmorphicBottomNavigation(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    },
                    containerColor = Color.Black
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            BottomTab.HOME -> HomeScreen()
                            BottomTab.EXPLORE -> ExploreScreen()
                            BottomTab.LIBRARY -> LibraryScreen()
                            BottomTab.SEARCH -> SearchScreen()
                        }
                    }
                }
            }

            if (showLoginDialog) {
                YouTubeLoginDialog(
                    onDismiss = { showLoginDialog = false },
                    onLoginSuccess = { cookie ->
                        showLoginDialog = false
                    }
                )
            }
        }
    }
}
