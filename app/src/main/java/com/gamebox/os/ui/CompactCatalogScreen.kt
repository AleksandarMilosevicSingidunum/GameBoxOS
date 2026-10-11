@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.GameSourceConfig

internal data class CompactCatalogState(
    val authorizedGames: List<Game>,
    val filteredGames: List<Game>,
    val focusTarget: GameId?,
    val filters: RememberedGameFilters,
    val selectedConsoleKey: String?,
    val selectedConsole: StoreConsole?,
    val discoveryGames: List<DiscoveryGame>,
    val allDiscoveryGames: List<DiscoveryGame>,
    val refreshState: CatalogRefreshState,
    val sourceBusy: Boolean,
    val sourceMessage: String?,
    val configuredSources: List<GameSourceConfig>,
    val configuredResults: List<DiscoverySourceGame>,
)

internal data class CompactCatalogActions(
    val refresh: () -> Unit,
    val onFocused: (GameId) -> Unit,
    val openAuthorized: (Game) -> Unit,
    val selectConsole: (String?) -> Unit,
    val openConfiguredSource: (GameSourceConfig) -> Unit,
    val openConfiguredResult: (DiscoverySourceGame) -> Unit,
    val openDiscoveryGame: (DiscoveryGame) -> Unit,
)

@Composable
internal fun CatalogRefreshAction(
    state: CatalogRefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BlueprintButton(
        onClick = onRefresh,
        enabled = state != CatalogRefreshState.REFRESHING,
        modifier = modifier.semantics {
            contentDescription = catalogRefreshButtonDescription(state)
            if (state == CatalogRefreshState.REFRESHING) liveRegion = LiveRegionMode.Polite
        },
    ) {
        Text(if (state == CatalogRefreshState.REFRESHING) "Refreshing..." else "Refresh")
    }
}

@Composable
internal fun CompactCatalogScreen(
    state: CompactCatalogState,
    actions: CompactCatalogActions,
    searchFocusRequester: FocusRequester,
    configuredResultReturnFocusRequester: FocusRequester? = null,
    configuredResultReturnFocusKey: String? = null,
) {
    val filters = state.filters
    Column(Modifier.fillMaxSize().verticalScroll(restoredScrollState("store"))) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text("Store", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Browse authorized games, metadata, and configured sources",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            }
            CatalogRefreshAction(state.refreshState, actions.refresh)
        }
        Text(
            "Consoles",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "Store console filters" },
        )
        StoreConsoleFilterRow(
            selectedKey = state.selectedConsoleKey,
            onSelect = actions.selectConsole,
            sectionSemantics = true,
        )
        CatalogRefreshStatus(state.refreshState)
        Spacer(Modifier.height(16.dp))
        GameFilterBar(
            state.authorizedGames,
            filters.query,
            { filters.query = it },
            filters.platform,
            { filters.platform = it },
            filters.genre,
            { filters.genre = it },
            filters.favoritesOnly,
            { filters.favoritesOnly = it },
            showMetadataFilters = true,
            extraRegions = state.allDiscoveryGames.mapNotNull { it.region },
            region = filters.region,
            onRegion = { filters.region = it },
            language = filters.language,
            onLanguage = { filters.language = it },
            searchFocusRequester = searchFocusRequester,
        )
        Spacer(Modifier.height(16.dp))
        if (state.filteredGames.isEmpty()) Text("No authorized games match these filters")
        else GameRow(
            state.filteredGames,
            state.focusTarget,
            actions.onFocused,
            compact = true,
            open = actions.openAuthorized,
        )

        CompactCatalogDiscoverySection(
            selectedConsole = state.selectedConsole,
            selectedConsoleKey = state.selectedConsoleKey,
            onSelectConsole = actions.selectConsole,
            syncing = state.sourceBusy,
            sourceMessage = state.sourceMessage,
            configuredSources = state.configuredSources,
            query = filters.query,
            onUpdateQuery = { filters.query = it },
            configuredResults = state.configuredResults,
            allDiscoveryGames = state.allDiscoveryGames,
            onOpenConfiguredSource = actions.openConfiguredSource,
            onOpenConfiguredResult = actions.openConfiguredResult,
            returnFocusRequester = configuredResultReturnFocusRequester,
            returnFocusKey = configuredResultReturnFocusKey,
            discoveryGames = state.discoveryGames,
            favoritesOnly = filters.favoritesOnly,
            genre = filters.genre,
            region = filters.region,
            language = filters.language,
            onOpenDiscoveryGame = actions.openDiscoveryGame,
        )
    }
}
