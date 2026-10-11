package com.gamebox.os.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Checkbox with explicit console focus, sound, and A-button activation behavior. */
@Composable
internal fun BlueprintCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val activate = rememberUiClickAction {
        if (enabled) onCheckedChange(!checked)
    }
    Checkbox(
        checked = checked,
        onCheckedChange = { if (enabled) activate() },
        modifier = modifier
            .bringFocusedControlIntoView()
            .blueprintButtonFocusRing(RoundedCornerShape(4.dp))
            .onBlueprintActivation(activate),
        enabled = enabled,
    )
}
