package com.gamebox.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamebox.os.ui.GameBoxApp
import com.gamebox.os.ui.theme.GameBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as GameBoxApplication).container
        setContent {
            GameBoxTheme {
                GameBoxApp(
                    container.gameRepository,
                    container.downloadRepository,
                    container.authorizedDownloadController
                )
            }
        }
    }
}
