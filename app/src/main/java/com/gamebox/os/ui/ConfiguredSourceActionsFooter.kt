package com.gamebox.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.source.GameSourceConfig

@Composable
internal fun ConfiguredSourceActionsFooter(
    sources: List<GameSourceConfig>,
    refreshing: CatalogRefreshState,
    onSearchSource: (GameSourceConfig) -> Unit,
    onRefreshCatalog: () -> Unit,
    firstActionFocusRequester: FocusRequester?,
    firstActionShouldTakeFocus: Boolean,
    focusClaimed: MutableState<Boolean>,
) {
    val source = sources.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Find more games", fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(
            if (source == null) "Connect a game source in Settings to search for titles."
            else "Search a source for titles, then import a copy you own.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (source != null) {
                BlueprintButton(
                    onClick = { onSearchSource(source) },
                    enabled = refreshing != CatalogRefreshState.REFRESHING,
                    modifier = Modifier.weight(1f).claimInitialCatalogActionFocus(
                        enabled = firstActionShouldTakeFocus,
                        focusRequester = firstActionFocusRequester,
                        focusClaimed = focusClaimed,
                    ),
                ) {
                    Text("Search ${source.name}", fontSize = 10.sp, maxLines = 1)
                }
            }
            BlueprintOutlinedButton(
                onClick = onRefreshCatalog,
                enabled = refreshing != CatalogRefreshState.REFRESHING,
                modifier = Modifier.weight(1f).then(
                    if (source == null) Modifier.claimInitialCatalogActionFocus(
                        enabled = firstActionShouldTakeFocus,
                        focusRequester = firstActionFocusRequester,
                        focusClaimed = focusClaimed,
                    ) else Modifier
                ).semantics {
                    contentDescription = catalogRefreshButtonDescription(refreshing)
                    if (refreshing == CatalogRefreshState.REFRESHING) liveRegion = LiveRegionMode.Polite
                },
            ) {
                Text(
                    if (refreshing == CatalogRefreshState.REFRESHING) "Refreshing…" else "Refresh catalog",
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
        }
        if (sources.size > 1) {
            Text("${sources.size} game sources are configured in Settings.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}
