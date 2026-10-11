package com.gamebox.os.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** FilterChip with the same animated focus, click feedback, and gamepad-A behavior as other actions. */
@Composable
internal fun BlueprintFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val activate = rememberUiClickAction { if (enabled) onClick() }
    FilterChip(
        selected = selected,
        onClick = activate,
        label = label,
        modifier = modifier
            .bringFocusedControlIntoView()
            .blueprintButtonFocusRing(FilterChipDefaults.shape)
            .onBlueprintActivation(activate),
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
    )
}
