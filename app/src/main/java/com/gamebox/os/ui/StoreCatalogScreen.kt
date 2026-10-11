@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.CatalogDiscoveryRepository
import com.gamebox.os.importer.AuthorizedRomImporter
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.settings.SettingsRepository
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType
import kotlinx.coroutines.launch

@Composable
internal fun CatalogScreen(
    repository: GameRepository,
    discoveryRepository: CatalogDiscoveryRepository,
    authorizedRomImporter: AuthorizedRomImporter,
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    settingsRepository: SettingsRepository,
    gameSources: List<GameSourceConfig> = emptyList(),
    uiState: GameBoxUiState,
    focusSearchOnEnter: Boolean = false,
    hostWindowHasFocus: Boolean = true,
    onSearchFocusHandled: () -> Unit = {},
    controllerKeyboard: Boolean = false,
    open: (Game) -> Unit,
    onOpenDownloads: () -> Unit = {},
) {
    val context = LocalContext.current
    val screenSettings by settingsRepository.settings.collectAsState(
        initial = com.gamebox.os.settings.GameBoxSettings(),
    )
    val allowCatalogSimulation = context.applicationInfo.flags and
        android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    val liveRefreshState by repository.observeCatalogRefreshState().collectAsState()
    val refreshState = com.gamebox.os.settings.effectiveCatalogRefreshState(
        liveState = liveRefreshState,
        simulation = screenSettings.catalogFailureSimulation,
        allowSimulation = allowCatalogSimulation,
    )
    val scope = rememberCoroutineScope()
    val discoveryActions = remember(context, discoveryRepository, scope) {
        CatalogDiscoveryActions(context, discoveryRepository, scope)
    }
    val controllerActions = LocalControllerActions.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val vimmSourceFocusRequester = remember { FocusRequester() }
    var searchFocusRequestedForStore by remember(uiState) { mutableStateOf(false) }
    var showControllerSearchKeyboard by remember(uiState) { mutableStateOf(false) }
    var searchVimmAfterKeyboard by remember(uiState) { mutableStateOf(false) }
    val firstWideCatalogRailItem = remember { FocusRequester() }
    val firstWideCatalogContent = remember { FocusRequester() }
    val emptyCatalogActionFocusRequester = remember { FocusRequester() }
    LaunchedEffect(focusSearchOnEnter) {
        if (focusSearchOnEnter) {
            searchFocusRequestedForStore = true
            if (controllerKeyboard) showControllerSearchKeyboard = true
            else runCatching { searchFocusRequester.requestFocus() }
            onSearchFocusHandled()
        }
    }
    LaunchedEffect(uiState.destination) {
        if (uiState.destination != Destination.STORE.name) {
            searchFocusRequestedForStore = false
        }
    }
    LaunchedEffect(uiState.destination, compact, hostWindowHasFocus) {
        if (!shouldAutofocusCatalogRail(
                destination = uiState.destination,
                compact = compact,
                hostWindowHasFocus = hostWindowHasFocus,
                searchFocusRequested = searchFocusRequestedForStore ||
                    uiState.hasStoreVimmSearchRequest(),
            )
        ) return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos { }
        if (searchFocusRequestedForStore) return@LaunchedEffect
        runCatching { firstWideCatalogRailItem.requestFocus() }
    }
    val filters = rememberGameFilters(uiState, "store")
    val discoveryPlatforms by discoveryRepository.observePlatforms().collectAsState(initial = emptyList())
    val selection = rememberStoreSelectionState(uiState)
    val selectedConsoleKey = selection.consoleKey
    val selectedConsole = storeConsoles.firstOrNull { it.key == selectedConsoleKey }
    val configuredSources = remember(gameSources, selectedConsole) {
        configuredSourcesForStore(gameSources, selectedConsole)
    }
    val discoveryPlatformId = when {
        selectedConsole == null -> null
        selectedConsole.key == "homebrew" -> "__homebrew__"
        else -> discoveryPlatforms.firstOrNull { platform ->
            storeConsoleMatches(selectedConsole, platform.name)
        }?.id ?: "__pending_${selectedConsole.key}"
    }
    val allDiscoveryGames by discoveryRepository.observeGames(null, "", 250)
        .collectAsState(initial = emptyList())
    val discoveryGames by discoveryRepository.observeGames(discoveryPlatformId, filters.query, 250)
        .collectAsState(initial = emptyList())
    val selectedDiscoveryId = selection.discoveryId
    val configuredSourceResults = discoveryActions.configuredResults
    val selectedConfiguredResult = discoveryActions.selectedConfiguredResult
    val configuredResultReturnFocusRequester = remember { FocusRequester() }
    var configuredResultFocusKey by remember { mutableStateOf<String?>(null) }
    var restoreConfiguredResultFocus by remember { mutableStateOf(false) }
    val selectedDiscovery = selectedDiscoveryId?.let { id ->
        discoveryGames.firstOrNull { it.id == id }
    }
    fun returnToConfiguredResults() {
        discoveryActions.clearSelectedConfiguredResult()
        restoreConfiguredResultFocus = true
    }
    BackHandler(enabled = selectedConfiguredResult != null || selectedDiscovery != null) {
        if (selectedConfiguredResult != null) returnToConfiguredResults()
        else selection.clearDiscovery()
    }
    LaunchedEffect(restoreConfiguredResultFocus, configuredResultFocusKey, configuredSourceResults) {
        if (!restoreConfiguredResultFocus) return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos { }
        kotlinx.coroutines.delay(200)
        runCatching { configuredResultReturnFocusRequester.requestFocus() }
        restoreConfiguredResultFocus = false
    }
    val discoverySyncing = discoveryActions.isSyncing
    val emptyCatalogActionVisible = shouldShowEmptyCatalogAction(
        compact = compact,
        discoveryGamesEmpty = discoveryGames.isEmpty(),
        hasSelectedDiscovery = selectedDiscoveryId != null,
        hasSelectedConfiguredResult = selectedConfiguredResult != null,
        configuredResultsEmpty = configuredSourceResults.isEmpty(),
        syncing = discoverySyncing,
    )
    // In wide/controller mode keep initial focus on the console rail. The
    // empty-state CTA must not clear and steal focus after the rail is placed.
    val focusEmptyCatalogAction = shouldAutofocusCatalogAction(
        emptyCatalog = emptyCatalogActionVisible,
        hasControllerNavigationRail = controllerKeyboard,
    )
    val focusRefreshAction = focusEmptyCatalogAction &&
        refreshState != CatalogRefreshState.REFRESHING
    LaunchedEffect(focusEmptyCatalogAction, focusRefreshAction) {
        if (focusEmptyCatalogAction) {
            androidx.compose.runtime.withFrameNanos { }
            kotlinx.coroutines.delay(300)
            focusManager.clearFocus(force = true)
            runCatching { emptyCatalogActionFocusRequester.requestFocus() }
        }
    }
    val configuredSourceMessage = discoveryActions.message
    val configuredResultNotice = discoveryActions.configuredResultNotice
    fun closeControllerSearchKeyboard() {
        showControllerSearchKeyboard = false
        if (searchVimmAfterKeyboard) {
            searchVimmAfterKeyboard = false
            configuredSources.firstOrNull { it.type == GameSourceProviderType.VIMM_LAIR }
                ?.takeIf { filters.query.isNotBlank() }
                ?.let { discoveryActions.openSource(it, filters.query, selectedConsole) }
        }
    }

    LaunchedEffect(filters.query, selectedConsoleKey) {
        discoveryActions.clearSearchResults()
    }
    DisposableEffect(controllerActions, selectedDiscovery, selectedConfiguredResult) {
        val lease = if (selectedDiscovery == null && selectedConfiguredResult == null) {
            controllerActions?.configure(
                xLabel = "Search",
                onX = {
                    if (controllerKeyboard) showControllerSearchKeyboard = true
                    else runCatching { searchFocusRequester.requestFocus() }
                },
                yLabel = "Filters",
                onY = { filters.favoritesOnly = !filters.favoritesOnly },
                onBack = {
                    if (showControllerSearchKeyboard) {
                        closeControllerSearchKeyboard()
                        true
                    } else false
                },
            )
        } else null
        onDispose {
            val owner = controllerActions
            if (owner != null && lease != null) owner.clear(lease)
        }
    }
    val filtered = rememberFilteredGames(games, filters)
    val focusTarget = restoreGameId?.takeIf { id -> filtered.any { it.id == id } }
        ?: filtered.firstOrNull()?.id
    if (selectedConfiguredResult != null) {
        ConfiguredDiscoveryDetailsRoute(
            result = selectedConfiguredResult,
            allDiscoveryGames = allDiscoveryGames,
            gameSources = gameSources,
            onBack = ::returnToConfiguredResults,
            sourceNotice = configuredResultNotice,
            importer = authorizedRomImporter,
            repository = repository,
            onOpenDownloads = onOpenDownloads,
        )
        return
    }
    if (selectedDiscovery != null) {
        DiscoveryCatalogDetailsRoute(
            game = selectedDiscovery,
            platformName = selectedConsole?.label
                ?: discoveryPlatforms.firstOrNull { it.id == selectedDiscovery.platformId }?.name
                ?: selectedDiscovery.platformId,
            onBack = {
                selection.clearDiscovery()
            },
            onFavorite = {
                scope.launch {
                    discoveryRepository.setFavorite(
                        selectedDiscovery.id,
                        !selectedDiscovery.favorite,
                    )
                }
            },
            importer = authorizedRomImporter,
            repository = repository,
            gameSources = gameSources,
            onOpenDownloads = onOpenDownloads,
        )
        return
    }
    val focusVimmSearchOnEnter = uiState.hasStoreVimmSearchRequest() &&
        uiState.destination == Destination.STORE.name
    LaunchedEffect(focusVimmSearchOnEnter, configuredSources) {
        if (!focusVimmSearchOnEnter) return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos { }
        val vimmConfigured = configuredSources.any { it.type == GameSourceProviderType.VIMM_LAIR }
        runCatching {
            if (vimmConfigured) vimmSourceFocusRequester.requestFocus()
            else searchFocusRequester.requestFocus()
        }
        uiState.consumeStoreVimmSearchRequest()
    }

    fun openConfiguredSource(source: GameSourceConfig) {
        if (source.type == GameSourceProviderType.VIMM_LAIR && filters.query.isBlank()) {
            searchVimmAfterKeyboard = true
            if (controllerKeyboard) showControllerSearchKeyboard = true
            else runCatching { searchFocusRequester.requestFocus() }
            return
        }
        searchVimmAfterKeyboard = false
        discoveryActions.openSource(source, filters.query, selectedConsole)
    }

    fun openConfiguredResult(result: DiscoverySourceGame) {
        configuredResultFocusKey = result.sourceId + ":" + result.externalId
        restoreConfiguredResultFocus = false
        discoveryActions.openResult(result, gameSources)
    }

    if (!compact) {
        BlueprintCatalogScreen(
            state = BlueprintCatalogScreenState(
                authorizedGames = filtered,
                allAuthorizedGames = games,
                filters = filters,
                uiState = uiState,
                discoveryGames = discoveryGames,
                allDiscoveryGames = allDiscoveryGames,
                discoveryPlatformNames = discoveryPlatforms.associate { it.id to it.name },
                selectedConsole = selectedConsole,
                refreshState = refreshState,
                discoverySyncing = discoverySyncing,
                configuredSourceMessage = configuredSourceMessage,
                configuredSources = configuredSources,
                configuredSourceResults = configuredSourceResults,
                controllerKeyboard = controllerKeyboard,
                showControllerSearchKeyboard = showControllerSearchKeyboard,
            ),
            actions = BlueprintCatalogScreenActions(
                onClearPlatform = { filters.platform = null },
                onSelectConsole = { selection.selectConsole(it?.key) },
                onOpenConfiguredSource = ::openConfiguredSource,
                onOpenConfiguredResult = ::openConfiguredResult,
                onRefresh = repository::refreshCatalog,
                openAuthorized = open,
                openDiscovery = { selection.selectDiscovery(it.id) },
                onOpenControllerSearch = { showControllerSearchKeyboard = true },
                onCloseControllerSearch = ::closeControllerSearchKeyboard,
            ),
            focus = BlueprintCatalogScreenFocus(
                search = searchFocusRequester,
                vimmSource = vimmSourceFocusRequester,
                initialRail = firstWideCatalogRailItem,
                railRight = firstWideCatalogContent,
                configuredResultReturn = configuredResultReturnFocusRequester
                    .takeIf { restoreConfiguredResultFocus },
                configuredResultReturnKey = configuredResultFocusKey
                    .takeIf { restoreConfiguredResultFocus },
                emptyAction = emptyCatalogActionFocusRequester,
                refreshAction = focusRefreshAction,
            ),
        )
        return
    }
    CompactCatalogScreen(
        state = CompactCatalogState(
            authorizedGames = games,
            filteredGames = filtered,
            focusTarget = focusTarget,
            filters = filters,
            selectedConsoleKey = selectedConsoleKey,
            selectedConsole = selectedConsole,
            discoveryGames = discoveryGames,
            allDiscoveryGames = allDiscoveryGames,
            refreshState = refreshState,
            sourceBusy = discoverySyncing,
            sourceMessage = configuredSourceMessage,
            configuredSources = configuredSources,
            configuredResults = configuredSourceResults,
        ),
        actions = CompactCatalogActions(
            refresh = repository::refreshCatalog,
            onFocused = onFocused,
            openAuthorized = open,
            selectConsole = { selection.selectConsole(it) },
            openConfiguredSource = ::openConfiguredSource,
            openConfiguredResult = ::openConfiguredResult,
            openDiscoveryGame = { selection.selectDiscovery(it.id) },
        ),
        searchFocusRequester = searchFocusRequester,
        configuredResultReturnFocusRequester = configuredResultReturnFocusRequester
            .takeIf { restoreConfiguredResultFocus },
        configuredResultReturnFocusKey = configuredResultFocusKey
            .takeIf { restoreConfiguredResultFocus },
    )
}
