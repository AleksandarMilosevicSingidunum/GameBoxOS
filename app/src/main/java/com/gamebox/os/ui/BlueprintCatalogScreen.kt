package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.Game
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType

internal data class BlueprintCatalogScreenState(
    val authorizedGames: List<Game>,
    val allAuthorizedGames: List<Game>,
    val filters: RememberedGameFilters,
    val uiState: GameBoxUiState,
    val discoveryGames: List<DiscoveryGame>,
    val allDiscoveryGames: List<DiscoveryGame>,
    val discoveryPlatformNames: Map<String, String>,
    val selectedConsole: StoreConsole?,
    val refreshState: CatalogRefreshState,
    val discoverySyncing: Boolean,
    val configuredSourceMessage: String?,
    val configuredSources: List<GameSourceConfig>,
    val configuredSourceResults: List<DiscoverySourceGame>,
    val controllerKeyboard: Boolean,
    val showControllerSearchKeyboard: Boolean,
)

internal data class BlueprintCatalogScreenActions(
    val onClearPlatform: () -> Unit,
    val onSelectConsole: (StoreConsole?) -> Unit,
    val onOpenConfiguredSource: (GameSourceConfig) -> Unit,
    val onOpenConfiguredResult: (DiscoverySourceGame) -> Unit,
    val onRefresh: () -> Unit,
    val openAuthorized: (Game) -> Unit,
    val openDiscovery: (DiscoveryGame) -> Unit,
    val onOpenControllerSearch: () -> Unit,
    val onCloseControllerSearch: () -> Unit,
)

internal data class BlueprintCatalogScreenFocus(
    val search: FocusRequester? = null,
    val vimmSource: FocusRequester? = null,
    val initialRail: FocusRequester? = null,
    val railRight: FocusRequester? = null,
    val configuredResultReturn: FocusRequester? = null,
    val configuredResultReturnKey: String? = null,
    val emptyAction: FocusRequester? = null,
    val refreshAction: Boolean = false,
)

@Composable
internal fun BlueprintCatalogScreen(
    state: BlueprintCatalogScreenState,
    actions: BlueprintCatalogScreenActions,
    focus: BlueprintCatalogScreenFocus = BlueprintCatalogScreenFocus(),
) {
    val authorizedGames = state.authorizedGames
    val allAuthorizedGames = state.allAuthorizedGames
    val allDiscoveryGames = state.allDiscoveryGames
    val discoveryGames = state.discoveryGames
    val discoveryPlatformNames = state.discoveryPlatformNames
    val filters = state.filters
    val genreFilter = filters.genre
    val onGenre: (String?) -> Unit = { filters.genre = it }
    val regionFilter = filters.region
    val onRegion: (String?) -> Unit = { filters.region = it }
    val languageFilter = filters.language
    val onLanguage: (String?) -> Unit = { filters.language = it }
    val favoritesOnly = filters.favoritesOnly
    val onFavoritesOnly: (Boolean) -> Unit = { filters.favoritesOnly = it }
    val query = filters.query
    val onQuery: (String) -> Unit = { filters.query = it }
    val uiState = state.uiState
    val selectedConsole = state.selectedConsole
    val refreshState = state.refreshState
    val discoverySyncing = state.discoverySyncing
    val configuredSourceMessage = state.configuredSourceMessage
    val configuredSources = state.configuredSources
    val configuredSourceResults = state.configuredSourceResults
    val controllerKeyboard = state.controllerKeyboard
    val showControllerSearchKeyboard = state.showControllerSearchKeyboard
    val onClearPlatform = actions.onClearPlatform
    val onSelectConsole = actions.onSelectConsole
    val onOpenConfiguredSource = actions.onOpenConfiguredSource
    val onOpenConfiguredResult = actions.onOpenConfiguredResult
    val onRefresh = actions.onRefresh
    val openAuthorized = actions.openAuthorized
    val openDiscovery = actions.openDiscovery
    val onOpenControllerSearch = actions.onOpenControllerSearch
    val onCloseControllerSearch = actions.onCloseControllerSearch
    val searchFocusRequester = focus.search
    val vimmSourceFocusRequester = focus.vimmSource
    val firstConfiguredResultFocusRequester = remember { FocusRequester() }
    val initialRailFocusRequester = focus.initialRail
    val railRightFocusRequester = focus.railRight
    val emptyCatalogActionFocusRequester = focus.emptyAction
    val focusRefreshAction = focus.refreshAction
    var installedOnly by remember(uiState) { mutableStateOf(uiState.screenValue("store.installed") == "true") }
    var sortByRating by remember(uiState) { mutableStateOf(uiState.screenValue("store.rating") == "true") }
    val emptyCatalogActionFocusClaimed = remember { mutableStateOf(false) }
    LaunchedEffect(installedOnly, sortByRating) {
        uiState.rememberScreenValue("store.installed", installedOnly.toString())
        uiState.rememberScreenValue("store.rating", sortByRating.toString())
    }
    val consoleGames = remember(authorizedGames, selectedConsole) {
        authorizedGames.filter { game ->
            selectedConsole == null || storeConsoleMatches(selectedConsole, game.platform) ||
                (selectedConsole.key == "homebrew" && isHomebrewStorePlatform(game.platform))
        }
    }
    val visibleAuthorized = remember(
        consoleGames, favoritesOnly, installedOnly, genreFilter, regionFilter, languageFilter,
    ) {
        selectVisibleAuthorizedStoreGames(
            consoleGames,
            favoritesOnly,
            installedOnly,
            genreFilter,
            regionFilter,
            languageFilter,
        )
    }
    val visibleDiscovery = remember(
        discoveryGames, favoritesOnly, sortByRating, genreFilter, regionFilter, languageFilter,
    ) {
        selectVisibleDiscoveryStoreGames(
            discoveryGames,
            favoritesOnly,
            sortByRating,
            genreFilter,
            regionFilter,
            languageFilter,
        )
    }
    val consoleCounts = remember(allDiscoveryGames, authorizedGames, discoveryPlatformNames) {
        storeConsoles.associate { console ->
            console.key to (
                allDiscoveryGames.count {
                    storeConsoleMatches(console, discoveryPlatformNames[it.platformId].orEmpty())
                } + authorizedGames.count {
                    storeConsoleMatches(console, it.platform) ||
                        (console.key == "homebrew" && isHomebrewStorePlatform(it.platform))
                }
            )
        }
    }
    val featuredDiscovery = visibleDiscovery.firstOrNull().takeUnless { installedOnly }
    val featuredAuthorized = visibleAuthorized.firstOrNull()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val railWidth = (maxWidth * 0.15f).coerceIn(124.dp, 166.dp)
        val sidebarWidth = (maxWidth * 0.18f).coerceIn(160.dp, 206.dp)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CatalogConsoleFilterRail(
                modifier = Modifier.width(railWidth),
                state = CatalogConsoleFilterState(
                    discoveryGameCount = allDiscoveryGames.size,
                    authorizedGameCount = authorizedGames.size,
                    consoleCounts = consoleCounts,
                    selectedConsole = selectedConsole,
                    favoritesOnly = favoritesOnly,
                    authorizedGames = allAuthorizedGames,
                    genre = genreFilter,
                    region = regionFilter,
                    language = languageFilter,
                    discoveryRegions = allDiscoveryGames.mapNotNull { it.region },
                    sortByRating = sortByRating,
                ),
                actions = CatalogConsoleFilterActions(
                    onSelectConsole = onSelectConsole,
                    onFavoritesOnly = onFavoritesOnly,
                    onGenre = onGenre,
                    onRegion = onRegion,
                    onLanguage = onLanguage,
                    onToggleSort = { sortByRating = !sortByRating },
                    onClearFilters = {
                        onFavoritesOnly(false)
                        installedOnly = false
                        sortByRating = false
                        onGenre(null)
                        onRegion(null)
                        onLanguage(null)
                        onQuery("")
                        onSelectConsole(null)
                        onClearPlatform()
                    },
                ),
                initialFocusRequester = initialRailFocusRequester,
                rightFocusRequester = railRightFocusRequester,
            )

            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CatalogFeaturedHero(
                        discoveryGame = featuredDiscovery,
                        authorizedGame = featuredAuthorized,
                        modifier = Modifier.weight(1f),
                        onOpenDiscovery = openDiscovery,
                        onOpenAuthorized = openAuthorized,
                    )
                    Column(Modifier.width(sidebarWidth), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            BlueprintFilterChip(selected = !installedOnly, onClick = { installedOnly = false }, label = { Text("All", fontSize = 11.sp) })
                            BlueprintFilterChip(selected = installedOnly, onClick = { installedOnly = true }, label = { Text("Installed", fontSize = 11.sp) })
                        }
                        Text(selectedConsole?.label ?: "All consoles", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${if (installedOnly) visibleAuthorized.size else visibleDiscovery.size + visibleAuthorized.size} matching titles", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        if (controllerKeyboard) {
                            ControllerSearchField(
                                query = query,
                                onOpenKeyboard = onOpenControllerSearch,
                                modifier = Modifier.fillMaxWidth(),
                                focusRequester = searchFocusRequester ?: railRightFocusRequester,
                            )
                        } else {
                            OutlinedTextField(
                                value = query, onValueChange = onQuery,
                                leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(16.dp)) },
                                placeholder = { Text("Search games", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().then(
                                    searchFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                                ).then(railRightFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                        Text("Metadata includes artwork and details. Open a title to import a copy or view its sources.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp)
                        if (configuredSources.isNotEmpty()) {
                            Text("CONFIGURED SOURCES", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            configuredSources.take(4).forEach { source ->
                                ConfiguredDiscoverySourceButton(
                                    source = source,
                                    enabled = !discoverySyncing,
                                    onClick = { onOpenConfiguredSource(source) },
                                    downFocusRequester = firstConfiguredResultFocusRequester
                                        .takeIf { configuredSourceResults.isNotEmpty() && !installedOnly },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    iconSize = 14.dp,
                                    labelFontSize = 10.sp,
                                    labelModifier = Modifier.padding(start = 5.dp).weight(1f),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).then(
                                        if (source.type == GameSourceProviderType.VIMM_LAIR) {
                                            vimmSourceFocusRequester?.let(Modifier::focusRequester) ?: Modifier
                                        } else Modifier
                                    ),
                                )
                            }
                            if (configuredSources.size > 4) {
                                Text("+${configuredSources.size - 4} more in Settings", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                    }
                }
                CatalogRefreshStatus(refreshState)
                ConfiguredSourceStatus(configuredSourceMessage, discoverySyncing)
                if (!installedOnly) {
                    ConfiguredDiscoveryResultsRail(
                        results = configuredSourceResults,
                        allDiscoveryGames = allDiscoveryGames,
                        onOpenResult = onOpenConfiguredResult,
                        cardWidth = 156.dp,
                        cardHeight = 132.dp,
                        query = query,
                        onUseSearchSuggestion = onQuery,
                        headingFontSize = 13,
                        firstResultFocusRequester = firstConfiguredResultFocusRequester
                            .takeIf { configuredSourceResults.isNotEmpty() },
                        returnFocusRequester = focus.configuredResultReturn,
                        returnFocusKey = focus.configuredResultReturnKey,
                    )
                }
                if (!installedOnly) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (sortByRating) "Top Rated" else "Discover Games", Modifier.semantics { heading() }, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${visibleDiscovery.size} titles", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    if (visibleDiscovery.isEmpty()) {
                        BlueprintPanel(Modifier.fillMaxWidth()) {
                            Text(
                                catalogDiscoveryEmptyMessage(
                                    favoritesOnly = favoritesOnly,
                                    query = query,
                                    hasConfiguredJsonSource = configuredSources.any {
                                        it.type == GameSourceProviderType.GAMEBOX_JSON
                                    },
                                ),
                                fontSize = 11.sp,
                            )
                        }
                    } else {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val cardWidth = ((maxWidth - 50.dp) / 6f).coerceIn(108.dp, 156.dp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(visibleDiscovery, key = { it.id.value }) { game ->
                                    DiscoveryGameCard(game, Modifier.width(cardWidth).height(cardWidth * 1.4f)) { openDiscovery(game) }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (installedOnly) "Installed Games" else "Your Game Sources", Modifier.semantics { heading() }, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${visibleAuthorized.size} titles", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                if (visibleAuthorized.isEmpty()) {
                    Text(
                        if (installedOnly) "No installed games match this console. Open a discovered title to import your game."
                        else "No connected game sources match these filters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visibleAuthorized, key = { it.id.value }) { game ->
                            BlueprintGameTile(game, false, { _ -> }, openAuthorized)
                        }
                    }
                }
                ConfiguredSourceActionsFooter(
                    sources = configuredSources,
                    refreshing = refreshState,
                    onSearchSource = onOpenConfiguredSource,
                    onRefreshCatalog = onRefresh,
                    firstActionFocusRequester = emptyCatalogActionFocusRequester,
                    firstActionShouldTakeFocus = focusRefreshAction,
                    focusClaimed = emptyCatalogActionFocusClaimed,
                )
            }
        }
    }
    if (controllerKeyboard && showControllerSearchKeyboard) {
        ControllerSearchKeyboardDialog(query, onQuery, onCloseControllerSearch)
    }
}
