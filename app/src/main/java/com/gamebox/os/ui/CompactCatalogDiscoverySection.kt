package com.gamebox.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.GameSourceConfig

@Composable
internal fun CompactCatalogDiscoverySection(
    selectedConsole: StoreConsole?,
    selectedConsoleKey: String?,
    onSelectConsole: (String?) -> Unit,
    syncing: Boolean,
    sourceMessage: String?,
    configuredSources: List<GameSourceConfig>,
    query: String,
    onUpdateQuery: (String) -> Unit = {},
    configuredResults: List<DiscoverySourceGame>,
    allDiscoveryGames: List<DiscoveryGame>,
    onOpenConfiguredSource: (GameSourceConfig) -> Unit,
    onOpenConfiguredResult: (DiscoverySourceGame) -> Unit,
    returnFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    returnFocusKey: String? = null,
    discoveryGames: List<DiscoveryGame>,
    favoritesOnly: Boolean,
    genre: String?,
    region: String?,
    language: String?,
    onOpenDiscoveryGame: (DiscoveryGame) -> Unit,
) {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Discover games", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Explore titles and configured sources, then import your own copy to play.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
        Text(
            "Automatic metadata and console artwork are added when a reliable match is available.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
    Text("Consoles", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    StoreConsoleFilterRow(
        selectedKey = selectedConsoleKey,
        onSelect = onSelectConsole,
        sectionSemantics = true,
    )
    ConfiguredDiscoverySourcesSection(
        configuredSources = configuredSources,
        query = query,
        onUpdateQuery = onUpdateQuery,
        consoleLabel = selectedConsole?.label,
        syncing = syncing,
        sourceMessage = sourceMessage,
        results = configuredResults,
        allDiscoveryGames = allDiscoveryGames,
        onOpenSource = onOpenConfiguredSource,
        onOpenResult = onOpenConfiguredResult,
        returnFocusRequester = returnFocusRequester,
        returnFocusKey = returnFocusKey,
    )
    Spacer(Modifier.height(10.dp))
    val visibleDiscovery = discoveryGames.filter { game ->
        (!favoritesOnly || game.favorite) && matchesStoreMetadataFilters(
            null,
            game.region,
            null,
            genre,
            region,
            language,
        )
    }
    if (visibleDiscovery.isEmpty()) {
        Text(
            when {
                discoveryGames.isNotEmpty() ->
                    "No discovery games match these filters. Clear filters to show all cached titles."
                selectedConsole?.key == "homebrew" ->
                    "Homebrew is local-first. Add authorized homebrew files through the Library importer."
                else ->
                    "No cached titles for this console yet. Search a configured source below or add one in Settings."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        DiscoveryGameRow(visibleDiscovery, compact = true, open = onOpenDiscoveryGame)
    }
}
