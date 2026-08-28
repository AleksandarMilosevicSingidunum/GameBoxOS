package com.gamebox.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GameBoxBlue = Color(0xFF5B8CFF)
val GameBoxPurple = Color(0xFF9A6BFF)
val GameBoxBackground = Color(0xFF080B12)
val GameBoxSurface = Color(0xFF141927)

private val colors = darkColorScheme(
    primary = GameBoxBlue,
    secondary = GameBoxPurple,
    background = GameBoxBackground,
    surface = GameBoxSurface,
    onBackground = Color(0xFFF4F7FF),
    onSurface = Color(0xFFF4F7FF)
)

@Composable
fun GameBoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
