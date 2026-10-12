package com.gamebox.os.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType

@Composable
internal fun ConfiguredDiscoverySourcesSection(
    configuredSources: List<GameSourceConfig>,
    query: String,
    onUseSearchSuggestion: (String) -> Unit = {},
    consoleLabel: String?,
    syncing: Boolean,
    sourceMessage: String? = null,
    results: List<DiscoverySourceGame>,
    allDiscoveryGames: List<DiscoveryGame>,
    onOpenSource: (GameSourceConfig) -> Unit,
    onOpenResult: (DiscoverySourceGame) -> Unit,
    returnFocusRequester: FocusRequester? = null,
    returnFocusKey: String? = null,
) {
    val firstResultFocusRequester = androidx.compose.runtime.remember { FocusRequester() }
    if (configuredSources.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        Text("Configured sources", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        val hasJsonSource = configuredSources.any { it.type == GameSourceProviderType.GAMEBOX_JSON }
        val hasVimmSource = configuredSources.any { it.type == GameSourceProviderType.VIMM_LAIR }
        Text(
            when {
                hasVimmSource && query.isBlank() ->
                    "Enter a title, choose a console, then search configured discovery sources."
                hasVimmSource ->
                    "Search Vimm's Lair for “$query”, sync JSON feeds, or open web sources."
                hasJsonSource && query.isBlank() ->
                    "Sync JSON metadata feeds or open web sources for ${consoleLabel ?: "all consoles"}."
                hasJsonSource ->
                    "Sync JSON metadata feeds or search web sources for “$query”."
                query.isBlank() ->
                    "Open a web source for ${consoleLabel ?: "all consoles"}."
                else ->
                    "Search configured web sources for “$query”."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            configuredSources.forEach { source ->
                ConfiguredDiscoverySourceButton(
                    source = source,
                    enabled = !syncing,
                    onClick = { onOpenSource(source) },
                    downFocusRequester = firstResultFocusRequester.takeIf { results.isNotEmpty() },
                )
            }
        }
    }

    ConfiguredSourceStatus(sourceMessage, syncing, Modifier.padding(top = 8.dp))

    ConfiguredDiscoveryResultsRail(
        results = results,
        allDiscoveryGames = allDiscoveryGames,
        onOpenResult = onOpenResult,
        cardWidth = 180.dp,
        cardHeight = 130.dp,
        query = query,
        onUseSearchSuggestion = onUseSearchSuggestion,
        modifier = Modifier.padding(top = if (results.isEmpty()) 0.dp else 10.dp),
        firstResultFocusRequester = firstResultFocusRequester.takeIf { results.isNotEmpty() },
        returnFocusRequester = returnFocusRequester,
        returnFocusKey = returnFocusKey,
    )
}
