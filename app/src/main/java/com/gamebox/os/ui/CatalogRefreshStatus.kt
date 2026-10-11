package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.gamebox.os.domain.CatalogRefreshState

internal fun catalogRefreshButtonDescription(state: CatalogRefreshState): String =
    if (state == CatalogRefreshState.REFRESHING) "Refreshing catalog" else "Refresh authorized catalog"

@Composable
internal fun CatalogRefreshStatus(state: CatalogRefreshState) {
    when (state) {
        CatalogRefreshState.OFFLINE_FALLBACK -> Text(
            "Offline - bundled catalog loaded; installed games remain playable",
            modifier = Modifier.semantics {
                contentDescription = "Offline; bundled catalog loaded; installed games remain playable"
                liveRegion = LiveRegionMode.Polite
            },
            color = MaterialTheme.colorScheme.tertiary,
        )
        CatalogRefreshState.REMOTE_FALLBACK -> Text(
            "Provider unavailable - bundled catalog loaded. Choose Refresh to retry.",
            modifier = Modifier.semantics {
                contentDescription = "Catalog provider unavailable; bundled catalog loaded; choose Refresh to retry"
                liveRegion = LiveRegionMode.Assertive
            },
            color = MaterialTheme.colorScheme.tertiary,
        )
        CatalogRefreshState.ERROR -> Text(
            "Refresh failed - cached catalog remains available. Choose Refresh to retry.",
            modifier = Modifier.semantics {
                contentDescription = "Catalog refresh failed; cached catalog remains available; choose Refresh to retry"
                liveRegion = LiveRegionMode.Assertive
            },
            color = MaterialTheme.colorScheme.error,
        )
        CatalogRefreshState.SUCCESS -> Text(
            "Catalog refreshed; local install and play state preserved",
            modifier = Modifier.semantics {
                contentDescription = "Catalog refresh succeeded; local install and play state preserved"
                liveRegion = LiveRegionMode.Polite
            },
            color = MaterialTheme.colorScheme.primary,
        )
        else -> Unit
    }
}
