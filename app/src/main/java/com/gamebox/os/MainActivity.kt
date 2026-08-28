package com.gamebox.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamebox.os.data.FakeGameRepository
import com.gamebox.os.ui.GameBoxApp
import com.gamebox.os.ui.theme.GameBoxTheme

class MainActivity : ComponentActivity() {
    private val repository = FakeGameRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameBoxTheme {
                GameBoxApp(repository)
            }
        }
    }
}
