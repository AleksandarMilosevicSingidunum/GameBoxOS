package com.gamebox.os.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class ConfiguredSourceStatusKind { INFO, SUCCESS, ERROR }

internal fun configuredSourceStatusKind(message: String): ConfiguredSourceStatusKind {
    val normalized = message.trim().lowercase()
    return when {
        "failed" in normalized || "invalid" in normalized || "no browser" in normalized ->
            ConfiguredSourceStatusKind.ERROR
        normalized.startsWith("found ") || normalized.startsWith("synced ") ||
            normalized.startsWith("loaded ") -> ConfiguredSourceStatusKind.SUCCESS
        else -> ConfiguredSourceStatusKind.INFO
    }
}

/** Keeps asynchronous source feedback visible and readable in both Store layouts. */
@Composable
internal fun ConfiguredSourceStatus(
    message: String?,
    syncing: Boolean,
    modifier: Modifier = Modifier,
) {
    val visibleMessage = message?.takeIf(String::isNotBlank)
        ?: if (syncing) "Working with configured source…" else return
    val kind = configuredSourceStatusKind(visibleMessage)
    val containerColor = when (kind) {
        ConfiguredSourceStatusKind.ERROR -> MaterialTheme.colorScheme.errorContainer
        ConfiguredSourceStatusKind.SUCCESS -> MaterialTheme.colorScheme.secondaryContainer
        ConfiguredSourceStatusKind.INFO -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (kind) {
        ConfiguredSourceStatusKind.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        ConfiguredSourceStatusKind.SUCCESS -> MaterialTheme.colorScheme.onSecondaryContainer
        ConfiguredSourceStatusKind.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.fillMaxWidth().semantics {
            contentDescription = "Configured source status: $visibleMessage"
            liveRegion = if (kind == ConfiguredSourceStatusKind.ERROR) {
                LiveRegionMode.Assertive
            } else {
                LiveRegionMode.Polite
            }
        },
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = when (kind) {
                        ConfiguredSourceStatusKind.ERROR -> Icons.Rounded.ErrorOutline
                        ConfiguredSourceStatusKind.SUCCESS -> Icons.Rounded.CheckCircle
                        ConfiguredSourceStatusKind.INFO -> Icons.Rounded.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    visibleMessage,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = if (kind == ConfiguredSourceStatusKind.ERROR) FontWeight.Medium else FontWeight.Normal,
                )
            }
            if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
