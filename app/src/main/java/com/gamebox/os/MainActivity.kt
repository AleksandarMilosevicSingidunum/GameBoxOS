package com.gamebox.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamebox.os.ui.GameBoxApp
import com.gamebox.os.ui.theme.GameBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as GameBoxApplication).container.gameRepository
        setContent {
            GameBoxTheme {
                GameBoxApp(repository)
            }
        }
    }
}
