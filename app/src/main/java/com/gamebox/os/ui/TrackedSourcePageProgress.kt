package com.gamebox.os.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun TrackedSourcePageProgress(
    progress: Int,
    loadingDescription: String,
) {
    if (progress >= 100) return
    val fraction = progress.coerceIn(0, 100) / 100f
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth()
            .height(3.dp)
            .semantics {
                contentDescription = loadingDescription
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
