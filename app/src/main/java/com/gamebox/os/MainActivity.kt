package com.gamebox.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamebox.os.ui.GameBoxApp
import com.gamebox.os.ui.theme.GameBoxTheme

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as GameBoxApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameBoxTheme {
                GameBoxApp(
                    container.gameRepository,
                    container.downloadRepository,
                    container.authorizedDownloadController,
                    container.gameLaunchController,
                    container.saveSafetyController,
                    container.settingsRepository
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.gameLaunchController.onHostResumed()
    }
}
