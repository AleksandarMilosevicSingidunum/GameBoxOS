package com.gamebox.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.gamebox.os.ui.GameBoxApp
import com.gamebox.os.ui.OfflineStatusBanner
import com.gamebox.os.ui.theme.GameBoxTheme

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as GameBoxApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameBoxTheme {
                Column(Modifier.fillMaxSize()) {
                    OfflineStatusBanner(this@MainActivity)
                    GameBoxApp(
                        container.gameRepository,
                        container.downloadRepository,
                        container.authorizedDownloadController,
                        container.remoteDownloadController,
                        container.gameLaunchController,
                        container.saveSafetyController,
                        container.settingsRepository,
                        container.catalogDiscoveryRepository
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.gameLaunchController.onHostResumed()
    }
}
