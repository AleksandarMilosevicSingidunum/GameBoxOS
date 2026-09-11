package com.gamebox.os.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

internal val LocalReducedMotion = staticCompositionLocalOf { false }

/** Keep the dashboard's proportions on large DeX displays without shrinking phone text. */
@Composable
internal fun BlueprintViewport(
    safeAreaPercent: Float = 0f,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useTvSafeArea = maxWidth >= 900.dp && maxWidth / maxHeight >= 1.5f
        val inset = safeAreaPercent.coerceIn(0f, 0.1f)
        val horizontalInset = if (useTvSafeArea) maxWidth * inset else 0.dp
        val verticalInset = if (useTvSafeArea) maxHeight * inset else 0.dp
        val contentWidth = maxWidth - horizontalInset * 2
        val contentHeight = maxHeight - verticalInset * 2
        val scale = minOf(contentWidth.value / 1280f, contentHeight.value / 720f).coerceIn(1f, 2f)
        Box(
            Modifier.fillMaxSize().padding(
                horizontal = horizontalInset,
                vertical = verticalInset,
            )
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * scale, density.fontScale),
                LocalReducedMotion provides reducedMotion,
            ) {
                content()
            }
        }
    }
}

internal fun blueprintFocusBrush(): Brush = Brush.linearGradient(
    listOf(Color(0xFF459BFF), Color(0xFF7979F6), Color(0xFFBD71FF))
)

/** Mouse, touch and controller share one interaction source, including the press animation. */
internal fun Modifier.blueprintClick(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        if (reducedMotion) 1f else if (pressed) .97f else if (hovered || focused) 1.018f else 1f,
        label = "blueprint-action",
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .onFocusChanged { focused = it.isFocused }
        .hoverable(source)
        .clickable(interactionSource = source, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
}
