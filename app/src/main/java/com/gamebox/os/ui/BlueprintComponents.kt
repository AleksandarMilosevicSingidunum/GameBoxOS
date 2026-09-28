package com.gamebox.os.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import com.gamebox.os.settings.DeveloperLayoutMode

@Stable
internal class ControllerActionRegistry {
    var xLabel by mutableStateOf("Store")
        private set
    var yLabel by mutableStateOf("Settings")
        private set
    private var xAction: (() -> Unit)? = null
    private var yAction: (() -> Unit)? = null

    fun configure(xLabel: String, onX: () -> Unit, yLabel: String, onY: () -> Unit) {
        this.xLabel = xLabel
        this.yLabel = yLabel
        xAction = onX
        yAction = onY
    }

    fun clear() {
        xLabel = "Store"
        yLabel = "Settings"
        xAction = null
        yAction = null
    }

    fun invokeX(): Boolean = xAction?.let { it(); true } ?: false
    fun invokeY(): Boolean = yAction?.let { it(); true } ?: false
}

internal val LocalControllerActions = staticCompositionLocalOf<ControllerActionRegistry?> { null }
internal val LocalGameBoxUiState = staticCompositionLocalOf<GameBoxUiState?> { null }
internal val LocalReducedMotion = staticCompositionLocalOf { false }

internal fun effectiveTvSafeAreaPercent(
    widthDp: Float,
    heightDp: Float,
    requestedPercent: Float,
): Float {
    if (!widthDp.isFinite() || !heightDp.isFinite() || widthDp <= 0f || heightDp <= 0f) return 0f
    val wideLivingRoomViewport = widthDp >= 900f && widthDp / heightDp >= 1.5f
    return if (wideLivingRoomViewport) requestedPercent.coerceIn(0f, 0.1f) else 0f
}

/**
 * Keep 16:9 living-room surfaces on the Blueprint wide composition even when a
 * high-density external display reports a small height in dp.
 */
internal fun shouldUseCompactLayout(
    mode: DeveloperLayoutMode,
    widthDp: Float,
    heightDp: Float,
): Boolean = when (mode) {
    DeveloperLayoutMode.WIDE -> false
    DeveloperLayoutMode.COMPACT -> true
    DeveloperLayoutMode.AUTO -> {
        if (!widthDp.isFinite() || !heightDp.isFinite() || widthDp <= 0f || heightDp <= 0f) {
            true
        } else {
            widthDp < 700f || (widthDp < 900f && widthDp / heightDp < 1.5f)
        }
    }
}

/**
 * Fit the Blueprint's 1280×720 design surface to a wide TV without shrinking
 * compact phone layouts. A 1920×1080 panel at 160 dpi is 1200×675 dp, so a
 * small reduction is required to keep the complete wide composition inside it.
 * High-density landscape surfaces can have much less dp-height than a physical
 * TV, so wide layouts must still be allowed to shrink far enough to fit both
 * dimensions instead of clipping the collection and recent-game rails.
 */
internal fun blueprintViewportScale(widthDp: Float, heightDp: Float): Float {
    if (!widthDp.isFinite() || !heightDp.isFinite() || widthDp <= 0f || heightDp <= 0f) return 1f
    val wideLivingRoomViewport = widthDp >= 900f && widthDp / heightDp >= 1.5f
    val baseScale = minOf(widthDp / 1280f, heightDp / 720f)
    return baseScale.coerceIn(if (wideLivingRoomViewport) 0.5f else 1f, 2f)
}

/** Keep the dashboard's proportions on large DeX displays without shrinking phone text. */
@Composable
internal fun BlueprintViewport(
    safeAreaPercent: Float = 0f,
    reducedMotion: Boolean = false,
    focusDebugOverlayEnabled: Boolean = false,
    controllerActions: ControllerActionRegistry? = null,
    uiState: GameBoxUiState? = null,
    runtimeDeviceStatus: RuntimeDeviceStatus = RuntimeDeviceStatus(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val focusDebugRegistry = remember { FocusDebugRegistry() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val inset = effectiveTvSafeAreaPercent(
            widthDp = maxWidth.value,
            heightDp = maxHeight.value,
            requestedPercent = safeAreaPercent,
        )
        val horizontalInset = maxWidth * inset
        val verticalInset = maxHeight * inset
        val contentWidth = maxWidth - horizontalInset * 2
        val contentHeight = maxHeight - verticalInset * 2
        val scale = blueprintViewportScale(contentWidth.value, contentHeight.value)
        Box(
            Modifier.fillMaxSize().padding(
                horizontal = horizontalInset,
                vertical = verticalInset,
            )
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * scale, density.fontScale),
                LocalReducedMotion provides reducedMotion,
                LocalControllerActions provides controllerActions,
                LocalGameBoxUiState provides uiState,
                LocalRuntimeDeviceStatus provides runtimeDeviceStatus,
                LocalFocusDebugRegistry provides focusDebugRegistry.takeIf { focusDebugOverlayEnabled },
            ) {
                content()
            }
            if (focusDebugOverlayEnabled) FocusDebugOverlay(focusDebugRegistry)
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
        targetValue = if (reducedMotion) 1f else if (pressed) .97f else if (hovered || focused) 1.018f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "blueprint-action",
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .focusDebugTarget()
        .onFocusChanged { focused = it.isFocused }
        .hoverable(source)
        .clickable(interactionSource = source, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
}
