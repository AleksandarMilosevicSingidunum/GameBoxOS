package com.gamebox.os.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.gamebox.os.ui.theme.GameBoxActionBlue

/** Material actions with one sound, enabled-state guard, and gamepad activation contract. */
@Composable
internal fun BlueprintButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val activate = rememberUiClickAction { if (enabled) onClick() }
    Button(
        onClick = activate,
        modifier = modifier.blueprintButtonFocusRing(ButtonDefaults.shape).onBlueprintActivation(activate),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = GameBoxActionBlue,
            contentColor = Color.White,
        ),
        content = content,
    )
}

@Composable
internal fun BlueprintOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val activate = rememberUiClickAction { if (enabled) onClick() }
    OutlinedButton(
        onClick = activate,
        modifier = modifier.blueprintButtonFocusRing(ButtonDefaults.shape).onBlueprintActivation(activate),
        enabled = enabled,
        content = content,
    )
}

@Composable
internal fun BlueprintTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val activate = rememberUiClickAction { if (enabled) onClick() }
    TextButton(
        onClick = activate,
        modifier = modifier.blueprintButtonFocusRing(ButtonDefaults.shape).onBlueprintActivation(activate),
        enabled = enabled,
        content = content,
    )
}

/** Gives standard actions the same unmistakable controller focus treatment as game cards. */
internal fun Modifier.blueprintButtonFocusRing(shape: Shape): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 2.dp else 0.dp,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 120),
        label = "blueprint-button-focus",
    )
    onFocusChanged { focused = it.isFocused }
        .then(if (borderWidth > 0.1.dp) Modifier.border(borderWidth, blueprintFocusBrush(), shape) else Modifier)
}
