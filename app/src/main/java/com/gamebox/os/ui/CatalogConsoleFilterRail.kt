package com.gamebox.os.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.Game

internal data class CatalogConsoleFilterState(
    val discoveryGameCount: Int,
    val authorizedGameCount: Int,
    val consoleCounts: Map<String, Int>,
    val selectedConsole: StoreConsole?,
    val favoritesOnly: Boolean,
    val authorizedGames: List<Game>,
    val genre: String?,
    val region: String?,
    val language: String?,
    val discoveryRegions: List<String>,
    val sortByRating: Boolean,
)

internal data class CatalogConsoleFilterActions(
    val onSelectConsole: (StoreConsole?) -> Unit,
    val onFavoritesOnly: (Boolean) -> Unit,
    val onGenre: (String?) -> Unit,
    val onRegion: (String?) -> Unit,
    val onLanguage: (String?) -> Unit,
    val onToggleSort: () -> Unit,
    val onClearFilters: () -> Unit,
)

@Composable
internal fun CatalogConsoleFilterRail(
    modifier: Modifier = Modifier,
    state: CatalogConsoleFilterState,
    actions: CatalogConsoleFilterActions,
    initialFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
) {
    val consoleFocusRequesters = remember { storeConsoles.map { FocusRequester() } }
    BlueprintRail(modifier) {
        Text("CONSOLES", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        BlueprintRailItem(
            "All", state.discoveryGameCount + state.authorizedGameCount, state.selectedConsole == null,
            Icons.Rounded.GridView,
            focusRequester = initialFocusRequester,
            rightFocusRequester = rightFocusRequester,
            downFocusRequester = consoleFocusRequesters.firstOrNull(),
        ) { actions.onSelectConsole(null) }
        storeConsoles.forEachIndexed { index, console ->
            val count = state.consoleCounts[console.key] ?: 0
            BlueprintRailItem(
                console.cardLabel,
                count,
                state.selectedConsole?.key == console.key,
                console.icon,
                brandKey = console.key,
                rightFocusRequester = rightFocusRequester,
                focusRequester = consoleFocusRequesters[index],
                upFocusRequester = if (index == 0) initialFocusRequester else consoleFocusRequesters[index - 1],
                downFocusRequester = consoleFocusRequesters.getOrNull(index + 1),
                semanticLabel = "Store console filter: ${console.label}, ${quantityLabel(count, "title")}",
            ) { actions.onSelectConsole(console) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
        Text("FILTERS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        BlueprintFilterChip(
            selected = state.favoritesOnly,
            onClick = { actions.onFavoritesOnly(!state.favoritesOnly) },
            label = { Text("Favorites", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Rounded.FavoriteBorder, null, Modifier.size(14.dp)) },
            modifier = Modifier.fillMaxWidth(),
        )
        MetadataFilterControls(
            games = state.authorizedGames,
            genre = state.genre,
            onGenre = actions.onGenre,
            region = state.region,
            onRegion = actions.onRegion,
            language = state.language,
            onLanguage = actions.onLanguage,
            extraRegions = state.discoveryRegions,
        )
        BlueprintTextButton(onClick = actions.onToggleSort, modifier = Modifier.fillMaxWidth().height(36.dp)) {
            Text(if (state.sortByRating) "Sort: Rating ↓" else "Sort: Title A–Z", fontSize = 12.sp)
        }
        BlueprintTextButton(onClick = actions.onClearFilters, modifier = Modifier.fillMaxWidth().height(36.dp)) {
            Text("Clear filters", fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.CloudDone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
        Text("${state.discoveryGameCount} cached titles", fontSize = 11.sp)
        Text("Browse your saved catalog offline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
