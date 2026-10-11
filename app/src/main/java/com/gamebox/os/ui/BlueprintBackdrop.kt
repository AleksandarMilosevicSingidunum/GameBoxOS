package com.gamebox.os.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val backgroundBrush = Brush.verticalGradient(
    listOf(Color(0xFF060B12), Color(0xFF03080D), Color(0xFF050A10)),
)
private val blueAmbientBrush = Brush.radialGradient(
    listOf(Color(0x125B8CFF), Color.Transparent),
)
private val violetAmbientBrush = Brush.radialGradient(
    listOf(Color(0x108B5CF6), Color.Transparent),
)

@Composable
internal fun BlueprintBackdrop(compact: Boolean) {
    Box(Modifier.fillMaxSize().background(backgroundBrush)) {
        if (!compact) {
            AmbientGlow(
                modifier = Modifier.size(620.dp).offset(x = (-260).dp, y = (-360).dp),
                brush = blueAmbientBrush,
            )
            AmbientGlow(
                modifier = Modifier.size(520.dp).align(Alignment.BottomEnd)
                    .offset(x = 260.dp, y = 260.dp),
                brush = violetAmbientBrush,
            )
        }
    }
}

@Composable
private fun BoxScope.AmbientGlow(modifier: Modifier, brush: Brush) {
    Box(modifier.background(brush, CircleShape))
}
