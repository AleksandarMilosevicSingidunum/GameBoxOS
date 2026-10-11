package com.gamebox.os.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Keep interactive TV content above persistent OEM navigation/taskbar overlays. */
internal fun Modifier.externalDisplaySystemOverlayInsets(
    externalDisplay: Boolean,
    bottomInset: Dp,
): Modifier = if (externalDisplay) {
    padding(bottom = bottomInset.coerceAtLeast(0.dp))
} else {
    this
}

/** Convert a persistent navigation/taskbar inset from Android pixels to Compose dp. */
internal fun externalDisplayBottomInsetDp(
    externalDisplay: Boolean,
    bottomInsetPx: Int,
    density: Float,
): Float = if (externalDisplay && bottomInsetPx > 0 && density.isFinite() && density > 0f) {
    bottomInsetPx / density
} else {
    0f
}
