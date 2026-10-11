@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.isPlayableLocally
import com.gamebox.os.domain.summarizeLibrary

@Composable
internal fun CollectionScreen(
    title: String,
    subtitle: String,
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    uiState: GameBoxUiState,
    controllerKeyboard: Boolean = false,
    onStore: () -> Unit,
    open: (Game) -> Unit
) {
    val controllerActions = LocalControllerActions.current
    val searchFocusRequester = remember { FocusRequester() }
    var showControllerSearchKeyboard by remember(uiState) { mutableStateOf(false) }
    val filters = rememberGameFilters(uiState, "library")
    DisposableEffect(controllerActions) {
        val lease = controllerActions?.configure(
            xLabel = "Search",
            onX = {
                if (controllerKeyboard) showControllerSearchKeyboard = true
                else runCatching { searchFocusRequester.requestFocus() }
            },
            yLabel = "Favorites",
            onY = { filters.favoritesOnly = !filters.favoritesOnly },
            onBack = {
                if (showControllerSearchKeyboard) {
                    showControllerSearchKeyboard = false
                    true
                } else false
            },
        )
        onDispose {
            val owner = controllerActions
            if (owner != null && lease != null) owner.clear(lease)
        }
    }
    val filtered = rememberFilteredGames(games, filters)
    val focusTarget = restoreGameId?.takeIf { id -> filtered.any { it.id == id } }
        ?: filtered.firstOrNull()?.id
    val summary = remember(games) { summarizeLibrary(games) }
    if (!compact) {
        BlueprintLibraryScreen(
            games = games,
            filtered = filtered,
            filters = filters,
            focusTarget = focusTarget,
            onFocused = onFocused,
            open = open,
            searchFocusRequester = searchFocusRequester,
            onStore = onStore,
            controllerKeyboard = controllerKeyboard,
            showControllerSearchKeyboard = showControllerSearchKeyboard,
            onOpenControllerSearch = { showControllerSearchKeyboard = true },
            onCloseControllerSearch = { showControllerSearchKeyboard = false },
        )
        return
    }
    Column(Modifier.verticalScroll(restoredScrollState("library"))) {
        Text(title, Modifier.semantics { heading() }, fontSize = if (compact) 28.sp else 38.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        LibrarySummaryCard(summary)
        Spacer(Modifier.height(14.dp))
        GameFilterBar(
            games, filters.query, { filters.query = it },
            filters.platform, { filters.platform = it },
            filters.genre, { filters.genre = it },
            filters.favoritesOnly, { filters.favoritesOnly = it },
            showMetadataFilters = true,
            region = filters.region, onRegion = { filters.region = it },
            language = filters.language, onLanguage = { filters.language = it },
            searchFocusRequester = searchFocusRequester,
        )
        Spacer(Modifier.height(14.dp))
        if (filtered.isEmpty()) {
            Text("No games match these filters")
        } else if (compact) {
            HomeGameSection("All games", filtered, focusTarget, onFocused, compact, open)
        } else {
            HomeGameSection(
                "Recently played",
                filtered.filter { it.lastPlayed != null },
                focusTarget,
                onFocused,
                compact,
                open
            )
            Spacer(Modifier.height(16.dp))
            HomeGameSection("All games", filtered, focusTarget, onFocused, compact, open)
            Spacer(Modifier.height(16.dp))
            HomeGameSection(
                "Ready to resume",
                filtered.filter { it.lastPlayed != null },
                focusTarget,
                onFocused,
                compact,
                open
            )
        }
    }
}
@Composable
internal fun BlueprintLibraryScreen(
    games: List<Game>,
    filtered: List<Game>,
    filters: RememberedGameFilters,
    focusTarget: GameId?,
    onFocused: (GameId) -> Unit,
    open: (Game) -> Unit,
    onStore: () -> Unit,
    searchFocusRequester: FocusRequester? = null,
    controllerKeyboard: Boolean = false,
    showControllerSearchKeyboard: Boolean = false,
    onOpenControllerSearch: () -> Unit = {},
    onCloseControllerSearch: () -> Unit = {},
) {
    val summary = summarizeLibrary(games)
    val platforms = games.groupingBy { it.platform }.eachCount().toList().sortedBy { it.first }
    val context = LocalContext.current
    val storageCapacity = rememberStorageCapacitySnapshot(context)
    val totalBytes = storageCapacity?.totalBytes ?: 0L
    val freeBytes = storageCapacity?.availableBytes ?: 0L
    val saveGames = games.filter { it.savePresent }
    var alphabetical by remember { mutableStateOf(false) }
    val emptyLibraryFocusRequester = remember { FocusRequester() }
    var emptyLibraryFocusClaimed by remember { mutableStateOf(false) }
    LaunchedEffect(filtered.isEmpty(), games.isEmpty()) {
        if (filtered.isEmpty() && games.isEmpty()) {
            androidx.compose.runtime.withFrameNanos { }
            kotlinx.coroutines.delay(300)
            runCatching { emptyLibraryFocusRequester.requestFocus() }
        }
    }
    val sortedGames = if (alphabetical) filtered.sortedBy { it.title.lowercase() }
        else filtered.sortedWith(compareByDescending<Game> { it.lastPlayed }.thenBy { it.title.lowercase() })
    val history = filtered.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed }
    val readyToResume = history.filter { it.savePresent && it.state.isPlayableLocally() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val railWidth = (maxWidth * 0.15f).coerceIn(124.dp, 166.dp)
    val sidebarWidth = (maxWidth * 0.19f).coerceIn(155.dp, 200.dp)
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BlueprintRail(Modifier.width(railWidth)) {
            Text("LIBRARY", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            BlueprintRailItem("All", games.size, filters.platform == null && !filters.favoritesOnly, Icons.Rounded.GridView) {
                filters.platform = null
                filters.favoritesOnly = false
            }
            platforms.forEach { (name, count) ->
                BlueprintRailItem(
                    gameCardConsoleLabel(name),
                    count,
                    filters.platform == name,
                    Icons.Rounded.SportsEsports,
                    brandKey = name,
                    semanticLabel = "$name, ${quantityLabel(count, "game")}",
                ) {
                    filters.platform = name
                    filters.favoritesOnly = false
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            BlueprintRailItem("Favorites", summary.favorites, filters.favoritesOnly, Icons.Rounded.Favorite) {
                filters.favoritesOnly = !filters.favoritesOnly
            }
            MetadataFilterControls(
                games = games,
                genre = filters.genre,
                onGenre = { filters.genre = it },
                region = filters.region,
                onRegion = { filters.region = it },
                language = filters.language,
                onLanguage = { filters.language = it },
            )
            BlueprintTextButton(onClick = {
                filters.query = ""
                filters.platform = null
                filters.favoritesOnly = false
                filters.genre = null
                filters.region = null
                filters.language = null
            }) { Text("Clear filters", fontSize = 12.sp) }
            Spacer(Modifier.weight(1f))
            Text("Sort by", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            BlueprintTextButton(onClick = { alphabetical = !alphabetical }, modifier = Modifier.fillMaxWidth()) {
                Text(if (alphabetical) "Title A–Z  ↓" else "Most Recent  ↓", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Available offline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(summary.installedGames.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your Library", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).semantics { heading() })
            if (controllerKeyboard) {
                ControllerSearchField(
                    query = filters.query,
                    onOpenKeyboard = onOpenControllerSearch,
                    modifier = Modifier.widthIn(max = 230.dp).weight(1f),
                    focusRequester = searchFocusRequester,
                )
            } else {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = { filters.query = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(17.dp)) },
                    placeholder = { Text("Search games", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.widthIn(max = 230.dp).weight(1f).then(
                        searchFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                    ),
                    shape = RoundedCornerShape(8.dp),
                )
            }
            }
            if (filtered.isEmpty()) {
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text(
                        if (games.isEmpty()) "Your library is empty" else "No games match these filters",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (games.isEmpty()) {
                        Text(
                            "Browse the Store to find authorized metadata and import a game.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                        BlueprintButton(
                            onClick = onStore,
                            modifier = Modifier
                                .focusRequester(emptyLibraryFocusRequester)
                                .onGloballyPositioned {
                                    if (!emptyLibraryFocusClaimed) {
                                        val focused = runCatching { emptyLibraryFocusRequester.requestFocus() }.getOrDefault(false)
                                        if (focused) emptyLibraryFocusClaimed = true
                                    }
                                },
                        ) { Text("Browse Store") }
                    }
                }
            } else {
                if (history.isNotEmpty()) {
                BlueprintLibrarySection(
                    title = "Recently Played",
                    games = history,
                    focusTarget = focusTarget,
                    onFocused = onFocused,
                    open = open,
                )
                }
                BlueprintLibrarySection(
                    title = "All Games",
                    games = sortedGames,
                    focusTarget = focusTarget.takeIf { history.none { game -> game.id == it } },
                    onFocused = onFocused,
                    open = open,
                )
                if (readyToResume.isNotEmpty()) {
                BlueprintLibrarySection(
                    title = "Ready to Resume",
                    games = readyToResume,
                    focusTarget = null,
                    onFocused = onFocused,
                    open = open,
                    landscape = true,
                )
                }
            }
        }

        LibraryOverviewSidebar(
            modifier = Modifier.width(sidebarWidth).fillMaxHeight(),
            summary = summary,
            totalStorageBytes = totalBytes,
            freeStorageBytes = freeBytes,
            gamesWithSaves = saveGames,
        )
    }
    if (controllerKeyboard && showControllerSearchKeyboard) {
        ControllerSearchKeyboardDialog(filters.query, { filters.query = it }, onCloseControllerSearch)
    }
    }
}
@Composable
private fun BlueprintLibrarySection(
    title: String,
    games: List<Game>,
    focusTarget: GameId?,
    onFocused: (GameId) -> Unit,
    open: (Game) -> Unit,
    landscape: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, Modifier.semantics { heading() }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(quantityLabel(games.size, "game"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val tileWidth = if (landscape) {
        ((maxWidth - 32.dp) / 3f).coerceIn(108.dp, 220.dp)
    } else {
        libraryPosterTileWidth(maxWidth)
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(games, key = { it.id.value }) { game ->
            GameCard(
                game = game,
                modifier = Modifier.width(tileWidth).height(if (landscape) 98.dp else libraryPosterTileHeight(tileWidth)),
                poster = !landscape,
                restoreFocus = focusTarget == game.id,
                onFocused = onFocused,
            ) { open(game) }
        }
    }
    }
}
