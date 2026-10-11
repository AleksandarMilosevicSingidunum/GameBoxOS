package com.gamebox.os.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Switch with the shared console focus ring, click feedback, and A-button toggle behavior. */
@Composable
internal fun BlueprintSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val activate = rememberUiClickAction {
        if (enabled) onCheckedChange(!checked)
    }
    Switch(
        checked = checked,
        onCheckedChange = { if (enabled) activate() },
        modifier = modifier
            .bringFocusedControlIntoView()
            .blueprintButtonFocusRing(CircleShape)
            .onBlueprintActivation(activate),
        enabled = enabled,
    )
}
