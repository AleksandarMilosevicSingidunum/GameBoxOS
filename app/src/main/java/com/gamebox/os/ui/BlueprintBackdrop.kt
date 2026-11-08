package com.gamebox.os.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val backgroundBrush = Brush.verticalGradient(
    listOf(Color(0xFF0A1221), Color(0xFF050A13), Color(0xFF070B16)),
)
private val blueAmbientBrush = Brush.radialGradient(
    listOf(Color(0x3A43B7FF), Color.Transparent),
)
private val violetAmbientBrush = Brush.radialGradient(
    listOf(Color(0x308B5CF6), Color.Transparent),
)

@Composable
internal fun BlueprintBackdrop(compact: Boolean) {
    Box(Modifier.fillMaxSize().background(backgroundBrush)) {
        if (!compact) {
            if (LocalReducedMotion.current) StaticAmbientGlows() else AnimatedAmbientGlows()
        }
    }
}

@Composable
internal fun TvModeBackdrop() {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "tv-ambient-backdrop")
    val drift = if (reducedMotion) 0f else transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(36_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tv-ambient-drift",
    ).value
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF111214), Color(0xFF0B0C0E), Color(0xFF090A0B))))
        val radius = size.maxDimension * 0.9f
        drawRect(Brush.radialGradient(
            listOf(Color(0x202F343B), Color(0x10282B30), Color.Transparent),
            center = Offset(size.width * (0.16f + drift * 0.035f), size.height * 0.06f), radius = radius,
        ))
        drawRect(Brush.radialGradient(
            listOf(Color(0x1B33363C), Color(0x10292B30), Color.Transparent),
            center = Offset(size.width * (0.84f - drift * 0.04f), size.height * 0.2f), radius = radius,
        ))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99070809))))
    }
}

@Composable
private fun BoxScope.StaticAmbientGlows() {
    AmbientGlow(Modifier.size(620.dp).offset(x = (-100).dp, y = (-120).dp), blueAmbientBrush)
    AmbientGlow(
        Modifier.size(520.dp).align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp),
        violetAmbientBrush,
    )
}

@Composable
private fun BoxScope.AnimatedAmbientGlows() {
    val transition = rememberInfiniteTransition(label = "ambient-backdrop")
    val bluePulse = transition.animateFloat(
        initialValue = 0.62f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "blue-ambient-pulse",
    ).value
    val violetPulse = transition.animateFloat(
        initialValue = 0.58f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(14_000, easing = FastOutSlowInEasing), RepeatMode.Reverse,
            initialStartOffset = StartOffset(1_800),
        ),
        label = "violet-ambient-pulse",
    ).value
    val drift = transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(32_000), RepeatMode.Reverse),
        label = "ambient-drift",
    ).value
    AmbientGlow(
        Modifier.size(620.dp).offset(x = (-100).dp, y = (-120).dp).graphicsLayer {
            alpha = bluePulse
            translationX = drift * 22.dp.toPx()
            translationY = -drift * 14.dp.toPx()
        },
        blueAmbientBrush,
    )
    AmbientGlow(
        Modifier.size(520.dp).align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp)
            .graphicsLayer {
                alpha = violetPulse
                translationX = -drift * 18.dp.toPx()
                translationY = drift * 12.dp.toPx()
            },
        violetAmbientBrush,
    )
}

@Composable
private fun BoxScope.AmbientGlow(modifier: Modifier, brush: Brush) {
    Box(modifier.background(brush, CircleShape))
}
