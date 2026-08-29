package com.gamebox.os.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.DownloadRepository
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.primaryAction
import com.gamebox.os.download.AuthorizedDownloadController
import com.gamebox.os.download.AuthorizedDownloadState
import com.gamebox.os.download.RemoteDownloadController
import com.gamebox.os.launch.GameLaunchController
import com.gamebox.os.launch.LaunchUiState
import com.gamebox.os.storage.SaveSafetyController
import com.gamebox.os.storage.ExternalStorageController
import com.gamebox.os.storage.ExternalStorageState
import com.gamebox.os.settings.SettingsRepository
import com.gamebox.os.catalog.validateAuthorizedCatalogUrl
import com.gamebox.os.diagnostics.DiagnosticsDevice
import com.gamebox.os.diagnostics.buildDiagnosticsReport
import kotlinx.coroutines.launch

private enum class Destination(val title: String) {
    HOME("Home"), LIBRARY("Library"), STORE("Store"), DOWNLOADS("Downloads"),
    MEDIA("Media"), PC("PC"), SETTINGS("Settings")
}

@Composable
fun GameBoxApp(
    repository: GameRepository,
    downloadRepository: DownloadRepository,
    authorizedDownloadController: AuthorizedDownloadController,
    remoteDownloadController: RemoteDownloadController,
    gameLaunchController: GameLaunchController,
    saveSafetyController: SaveSafetyController,
    settingsRepository: SettingsRepository
) {
    val games by repository.observeGames().collectAsState()
    var destination by remember { mutableStateOf(Destination.HOME) }
    var selectedGameId by remember { mutableStateOf<GameId?>(null) }
    val focusMemory = remember { GameFocusMemory() }
    val restorableGameId = focusMemory.restore(destination.name, games.map { it.id })
    val rememberGameFocus: (GameId) -> Unit = { focusMemory.remember(destination.name, it) }

    fun moveTab(offset: Int) {
        selectedGameId = null
        val tabs = Destination.entries
        destination = tabs[(destination.ordinal + offset + tabs.size) % tabs.size]
    }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_BUTTON_L1 -> { moveTab(-1); true }
                    AndroidKeyEvent.KEYCODE_BUTTON_R1 -> { moveTab(1); true }
                    AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.KEYCODE_BUTTON_B ->
                        if (selectedGameId != null) { selectedGameId = null; true } else false
                    else -> false
                }
            }
            .background(MaterialTheme.colorScheme.background)
    ) {
        val compact = maxWidth < 600.dp || maxHeight < 480.dp
        Column(
            Modifier.fillMaxSize().padding(
                horizontal = if (compact) 16.dp else 48.dp,
                vertical = if (compact) 12.dp else 28.dp
            )
        ) {
            TopNav(destination, compact) { destination = it; selectedGameId = null }
            Spacer(Modifier.height(if (compact) 14.dp else 28.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val selected = selectedGameId?.let(repository::game)
                if (selected != null) {
                    DetailsScreen(
                        selected,
                        repository,
                        downloadRepository,
                        authorizedDownloadController,
                        remoteDownloadController,
                        gameLaunchController,
                        saveSafetyController,
                        compact = compact,
                        onBack = { selectedGameId = null }
                    )
                } else {
                    when (destination) {
                        Destination.HOME -> HomeScreen(
                            games, restorableGameId, rememberGameFocus, compact
                        ) { selectedGameId = it.id }
                        Destination.LIBRARY -> CollectionScreen(
                            "Your Library", "Installed and ready offline",
                            games.filter { it.state == InstallState.INSTALLED || it.state == InstallState.UPDATE_AVAILABLE },
                            restorableGameId,
                            rememberGameFocus,
                            compact
                        ) { selectedGameId = it.id }
                        Destination.STORE -> CatalogScreen(
                            repository, games, restorableGameId, rememberGameFocus, compact
                        ) { selectedGameId = it.id }
                        Destination.DOWNLOADS -> DownloadsScreen(repository, downloadRepository, remoteDownloadController, compact)
                        Destination.MEDIA -> AppHubScreen(
                            "Media",
                            "Launch your living-room apps and return to GameBox",
                            mediaShortcuts,
                            compact
                        )
                        Destination.PC -> AppHubScreen(
                            "PC Hub",
                            "Streaming, Windows, Linux, files, browser, and desktop tools",
                            pcShortcuts,
                            compact
                        )
                        Destination.SETTINGS -> SettingsScreen(compact, settingsRepository, repository, downloadRepository)
                    }
                }
            }

            if (!compact) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "A Select    B Back    LB/RB Tabs    Menu Options",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TopNav(selected: Destination, compact: Boolean, onSelect: (Destination) -> Unit) {
    if (compact) {
        Column {
            Text(
                "GAMEBOX",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Destination.entries.forEach { item -> NavButton(item, selected, onSelect) }
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "GAMEBOX",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 28.dp, top = 8.dp)
            )
            Destination.entries.forEach { item -> NavButton(item, selected, onSelect) }
        }
    }
}

@Composable
private fun NavButton(item: Destination, selected: Destination, onSelect: (Destination) -> Unit) {
    Button(
        onClick = { onSelect(item) },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (item == selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface
        )
    ) { Text(item.title, maxLines = 1) }
}

@Composable
private fun HomeScreen(
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    open: (Game) -> Unit
) {
    if (games.isEmpty()) {
        Text("Catalog is loading...")
        return
    }
    val hero = games.first()
    val focusTarget = restoreGameId?.takeIf { id -> games.any { it.id == id } } ?: hero.id
    Column {
        Text("Good evening", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Text("Ready to play?", fontSize = if (compact) 30.sp else 42.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        GameCard(
            hero,
            Modifier.fillMaxWidth().height(if (compact) 148.dp else 188.dp),
            hero = true,
            restoreFocus = focusTarget == hero.id,
            onFocused = onFocused
        ) { open(hero) }
        Spacer(Modifier.height(24.dp))
        Text("Recently played", fontSize = if (compact) 20.sp else 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        GameRow(games.filter { it.lastPlayed != null && it.id != hero.id }, focusTarget, onFocused, compact, open)
    }
}

@Composable
private fun CatalogScreen(
    repository: GameRepository,
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    open: (Game) -> Unit
) {
    val refreshState by repository.observeCatalogRefreshState().collectAsState()
    var query by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf<String?>(null) }
    var genre by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    val filtered = filterGames(games, query, platform, genre, favoritesOnly)
    val focusTarget = restoreGameId?.takeIf { id -> filtered.any { it.id == id } }
        ?: filtered.firstOrNull()?.id
    Column {
        if (compact) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text("Authorized Catalog", fontSize = if (compact) 28.sp else 38.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Local-first cache from configured personal sources",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            Button(
                onClick = repository::refreshCatalog,
                enabled = refreshState != CatalogRefreshState.REFRESHING
            ) {
                Text(if (refreshState == CatalogRefreshState.REFRESHING) "Refreshing..." else "Refresh")
            }
        }
        if (refreshState == CatalogRefreshState.ERROR) {
            Text("Refresh failed - cached catalog remains available", color = MaterialTheme.colorScheme.error)
        }
        if (refreshState == CatalogRefreshState.SUCCESS) {
            Text("Catalog refreshed; local install and play state preserved",
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        GameFilterBar(
            games, query, { query = it }, platform, { platform = it },
            genre, { genre = it }, favoritesOnly, { favoritesOnly = it }
        )
        Spacer(Modifier.height(16.dp))
        if (filtered.isEmpty()) Text("No games match these filters")
        else GameRow(filtered, focusTarget, onFocused, compact, open)
    }
}

@Composable
private fun CollectionScreen(
    title: String,
    subtitle: String,
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    open: (Game) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf<String?>(null) }
    var genre by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    val filtered = filterGames(games, query, platform, genre, favoritesOnly)
    val focusTarget = restoreGameId?.takeIf { id -> filtered.any { it.id == id } }
        ?: filtered.firstOrNull()?.id
    Column {
        Text(title, fontSize = if (compact) 28.sp else 38.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Spacer(Modifier.height(14.dp))
        GameFilterBar(
            games, query, { query = it }, platform, { platform = it },
            genre, { genre = it }, favoritesOnly, { favoritesOnly = it }
        )
        Spacer(Modifier.height(14.dp))
        if (filtered.isEmpty()) Text("No games match these filters")
        else GameRow(filtered, focusTarget, onFocused, compact, open)
    }
}

@Composable
private fun GameFilterBar(
    games: List<Game>,
    query: String,
    onQuery: (String) -> Unit,
    platform: String?,
    onPlatform: (String?) -> Unit,
    genre: String?,
    onGenre: (String?) -> Unit,
    favoritesOnly: Boolean,
    onFavoritesOnly: (Boolean) -> Unit
) {
    val platforms = games.map { it.platform }.distinct().sorted()
    val genres = games.map { it.genre }.distinct().sorted()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("Search games") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = platform == null, onClick = { onPlatform(null) }, label = { Text("All platforms") })
            platforms.forEach { value ->
                FilterChip(selected = platform == value, onClick = { onPlatform(value) }, label = { Text(value) })
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = favoritesOnly,
                onClick = { onFavoritesOnly(!favoritesOnly) },
                label = { Text("Favorites") }
            )
            FilterChip(selected = genre == null, onClick = { onGenre(null) }, label = { Text("All genres") })
            genres.forEach { value ->
                FilterChip(selected = genre == value, onClick = { onGenre(value) }, label = { Text(value) })
            }
        }
    }
}

internal fun filterGames(
    games: List<Game>,
    query: String,
    platform: String?,
    genre: String?,
    favoritesOnly: Boolean
): List<Game> {
    val normalized = query.trim()
    return games.filter { game ->
        (normalized.isEmpty() ||
            game.title.contains(normalized, ignoreCase = true) ||
            game.platform.contains(normalized, ignoreCase = true) ||
            game.genre.contains(normalized, ignoreCase = true)) &&
            (platform == null || game.platform == platform) &&
            (genre == null || game.genre == genre) &&
            (!favoritesOnly || game.favorite)
    }
}

@Composable
private fun GameRow(
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    open: (Game) -> Unit
) {
    val listState = rememberLazyListState()
    val restoreIndex = games.indexOfFirst { it.id == restoreGameId }
    LaunchedEffect(restoreIndex) {
        if (restoreIndex >= 0) listState.scrollToItem(restoreIndex)
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(games, key = { it.id.value }) { game ->
            GameCard(
                game,
                Modifier.width(if (compact) 190.dp else 230.dp).height(if (compact) 148.dp else 170.dp),
                restoreFocus = restoreGameId == game.id,
                onFocused = onFocused,
                onClick = { open(game) }
            )
        }
    }
}

@Composable
private fun GameCard(
    game: Game,
    modifier: Modifier,
    hero: Boolean = false,
    restoreFocus: Boolean = false,
    onFocused: (GameId) -> Unit = {},
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "game-focus"
    )
    LaunchedEffect(restoreFocus) {
        if (restoreFocus) focusRequester.requestFocus()
    }
    Surface(
        modifier
            .focusRequester(focusRequester)
            .semantics { contentDescription = GameBoxSemantics.GAME_CARD }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused(game.id)
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (focused) 3.dp else 1.dp, border),
        tonalElevation = if (focused) 8.dp else 2.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(if (hero) "CONTINUE PLAYING" else game.platform.uppercase(),
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text((if (game.favorite) "★ " else "") + game.title, fontSize = if (hero) 32.sp else 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if (hero) "Press A for details" else game.state.displayName(), fontSize = 13.sp)
        }
    }
}

@Composable
private fun DetailsScreen(
    game: Game,
    repository: GameRepository,
    downloadRepository: DownloadRepository,
    authorizedDownloadController: AuthorizedDownloadController,
    remoteDownloadController: RemoteDownloadController,
    gameLaunchController: GameLaunchController,
    saveSafetyController: SaveSafetyController,
    compact: Boolean,
    onBack: () -> Unit
) {
    val isAuthorizedTest = game.id.value == "retro-test"
    val authorizedState by authorizedDownloadController.observeState().collectAsState()
    val launchState by gameLaunchController.observeState().collectAsState()
    val saveSafetyState by saveSafetyController.observeState().collectAsState()
    val workerActive = authorizedState.status == AuthorizedDownloadState.Status.QUEUED ||
        authorizedState.status == AuthorizedDownloadState.Status.RUNNING
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let(saveSafetyController::exportBackup) }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(saveSafetyController::importBackup) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(game.platform.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(game.title, fontSize = if (compact) 32.sp else 44.sp, fontWeight = FontWeight.Bold)
        Text(game.genre + "  |  " + game.year + "  |  " + game.sizeMb + " MB")
        GameSettingsPanel(game = game, repository = repository)
        Spacer(Modifier.height(24.dp))
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Install state", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                Text(game.state.displayName(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (isAuthorizedTest && authorizedState.status != AuthorizedDownloadState.Status.IDLE) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Verified asset worker: " + authorizedState.status.name.lowercase(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { authorizedState.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    authorizedState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(
                        onClick = {
                            if (isAuthorizedTest) {
                                authorizedDownloadController.install()
                            } else {
                                when (game.state) {
                                    InstallState.NOT_INSTALLED, InstallState.FAILED, InstallState.MISSING_FILES ->
                                        if (game.sourceUrl != null && game.expectedSha256 != null) {
                                            remoteDownloadController.install(game)
                                        } else {
                                            repository.setInstallState(game.id, InstallState.FAILED)
                                        }
                                    InstallState.QUEUED, InstallState.DOWNLOADING, InstallState.VERIFYING,
                                    InstallState.INSTALLING, InstallState.PAUSED -> Unit
                                    InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE ->
                                        gameLaunchController.launch(game)
                                }
                            }
                        },
                        enabled = when {
                            isAuthorizedTest -> !workerActive
                            game.state in setOf(
                                InstallState.NOT_INSTALLED,
                                InstallState.FAILED,
                                InstallState.MISSING_FILES
                            ) -> game.sourceUrl != null && game.expectedSha256 != null
                            else -> true
                        }
                    ) {
                        Text(
                            if (!isAuthorizedTest) game.state.primaryAction()
                            else if (authorizedState.status == AuthorizedDownloadState.Status.SUCCEEDED)
                                "Reinstall verified test"
                            else "Install verified test"
                        )
                    }
                    if (isAuthorizedTest && workerActive) {
                        OutlinedButton(onClick = authorizedDownloadController::cancel) {
                            Text("Cancel")
                        }
                    }
                    if (isAuthorizedTest && (game.state == InstallState.INSTALLED ||
                        game.state == InstallState.UPDATE_AVAILABLE)
                    ) {
                        Button(onClick = { gameLaunchController.launch(game) }) {
                            Text("Play")
                        }
                        OutlinedButton(onClick = saveSafetyController::uninstallTestContent) {
                            Text("Uninstall content")
                        }
                    }
                    if (isAuthorizedTest && !saveSafetyState.saveRecordPresent) {
                        OutlinedButton(onClick = saveSafetyController::createTestSaveRecord) {
                            Text("Create test save")
                        }
                    }
                    if (game.state == InstallState.DOWNLOADING || game.state == InstallState.PAUSED) {
                        OutlinedButton(onClick = { if (game.state == InstallState.PAUSED) downloadRepository.resume(game.id)
                            else downloadRepository.pause(game.id)
                            repository.pauseOrResume(game.id) }) {
                            Text(if (game.state == InstallState.PAUSED) "Resume" else "Pause")
                        }
                    }
                    OutlinedButton(
                        onClick = { repository.setFavorite(game.id, !game.favorite) }
                    ) { Text(if (game.favorite) "Remove favorite" else "Favorite") }
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }
                if (isAuthorizedTest && saveSafetyState.saveRecordPresent) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Save retained: " + saveSafetyState.relativePath +
                            " (" + saveSafetyState.sizeBytes + " bytes)",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = saveSafetyController::backupSave) {
                            Text(if (saveSafetyState.backupPresent) "Update backup" else "Back up save")
                        }
                        if (saveSafetyState.backupPresent) {
                            OutlinedButton(onClick = saveSafetyController::restoreSave) {
                                Text("Restore backup")
                            }
                            OutlinedButton(onClick = {
                                exportBackupLauncher.launch("gamebox-retro-test-save.dat")
                            }) {
                                Text("Export")
                            }
                        }
                        OutlinedButton(onClick = {
                            importBackupLauncher.launch(arrayOf("application/octet-stream"))
                        }) {
                            Text("Import")
                        }
                    }
                }
                if (isAuthorizedTest) {
                    saveSafetyState.operationMessage?.let { message ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            message,
                            color = if (saveSafetyState.operationSuccessful)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (launchState.gameId == game.id &&
                    launchState.status != LaunchUiState.Status.IDLE
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        launchState.message ?: when (launchState.status) {
                            LaunchUiState.Status.LAUNCHED -> "Emulator launched"
                            LaunchUiState.Status.RETURNED -> "Returned safely; play session recorded"
                            else -> launchState.status.name.lowercase().replace('_', ' ')
                        },
                        color = if (launchState.status in setOf(
                            LaunchUiState.Status.EMULATOR_UNAVAILABLE,
                            LaunchUiState.Status.UNSUPPORTED,
                            LaunchUiState.Status.NOT_INSTALLED,
                            LaunchUiState.Status.CONTENT_MISSING,
                            LaunchUiState.Status.VERIFICATION_FAILED,
                            LaunchUiState.Status.HANDOFF_REJECTED
                        )) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadsScreen(repository: GameRepository, downloadRepository: DownloadRepository, remoteDownloadController: RemoteDownloadController, compact: Boolean) {
    val jobs by downloadRepository.observeJobs().collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Downloads", fontSize = if (compact) 28.sp else 38.sp, fontWeight = FontWeight.Bold)
        Text("Durable queue plus verified app-private asset installation",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Spacer(Modifier.height(20.dp))
        if (jobs.isEmpty()) Text("No active downloads")
        jobs.forEach { job ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(job.title, fontWeight = FontWeight.Bold)
                        Text(job.status.displayName())
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { job.progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatDownloadBytes(job.downloadedBytes) + " of " +
                            formatDownloadBytes(job.totalBytes),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 12.sp
                    )
                    job.errorReason?.let { reason ->
                        Text(reason, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    val remoteGame = repository.game(job.gameId)?.takeIf { it.sourceUrl != null }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (remoteGame != null) {
                            if (job.status == DownloadStatus.DOWNLOADING) {
                                OutlinedButton(onClick = { remoteDownloadController.pause(remoteGame) }) {
                                    Text("Pause")
                                }
                            }
                            if (job.status == DownloadStatus.PAUSED) {
                                Button(onClick = { remoteDownloadController.resume(remoteGame) }) {
                                    Text("Resume")
                                }
                            }
                            if (job.status in setOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED)) {
                                Button(onClick = { remoteDownloadController.install(remoteGame) }) {
                                    Text("Retry")
                                }
                            }
                            if (job.status !in setOf(
                                    DownloadStatus.COMPLETED,
                                    DownloadStatus.FAILED,
                                    DownloadStatus.CANCELLED
                                )
                            ) {
                                OutlinedButton(onClick = { remoteDownloadController.cancel(remoteGame) }) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            if (job.status == DownloadStatus.DOWNLOADING) {
                                OutlinedButton(onClick = {
                                    downloadRepository.pause(job.gameId)
                                    repository.pauseOrResume(job.gameId)
                                }) { Text("Pause") }
                            }
                            if (job.status == DownloadStatus.PAUSED) {
                                OutlinedButton(onClick = {
                                    downloadRepository.resume(job.gameId)
                                    repository.pauseOrResume(job.gameId)
                                }) { Text("Resume") }
                            }
                            if (job.status !in setOf(
                                    DownloadStatus.COMPLETED,
                                    DownloadStatus.FAILED,
                                    DownloadStatus.CANCELLED
                                )
                            ) {
                                Button(onClick = {
                                    downloadRepository.advance(job.gameId)
                                    repository.advanceInstall(job.gameId)
                                }) { Text("Next test stage") }
                                OutlinedButton(onClick = {
                                    downloadRepository.cancel(job.gameId)
                                    repository.cancelInstall(job.gameId)
                                }) { Text("Cancel") }
                            }
                        }
                    }
                }
            }
        }
    }
}


private data class AppShortcut(
    val title: String,
    val description: String,
    val packageName: String
)

private val mediaShortcuts = listOf(
    AppShortcut("YouTube", "Video", "com.google.android.youtube"),
    AppShortcut("Netflix", "Streaming", "com.netflix.mediaclient"),
    AppShortcut("Kodi", "Media center", "org.xbmc.kodi"),
    AppShortcut("Jellyfin", "Personal media", "org.jellyfin.mobile"),
    AppShortcut("Plex", "Personal media", "com.plexapp.android"),
    AppShortcut("Spotify", "Music", "com.spotify.music"),
    AppShortcut("VLC", "Local media", "org.videolan.vlc"),
    AppShortcut("Twitch", "Live streams", "tv.twitch.android.app")
)

private val pcShortcuts = listOf(
    AppShortcut("Moonlight", "PC game streaming", "com.limelight"),
    AppShortcut("Winlator", "Windows applications", "com.winlator"),
    AppShortcut("Termux", "Linux terminal", "com.termux"),
    AppShortcut("Files", "Android document manager", "com.google.android.documentsui"),
    AppShortcut("Chrome", "Web browser", "com.android.chrome")
)

@Composable
private fun AppHubScreen(
    title: String,
    subtitle: String,
    shortcuts: List<AppShortcut>,
    compact: Boolean
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(title, fontSize = if (compact) 28.sp else 38.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Spacer(Modifier.height(18.dp))
        shortcuts.chunked(if (compact) 1 else 3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { shortcut ->
                    val launchIntent = remember(shortcut.packageName) {
                        context.packageManager.getLaunchIntentForPackage(shortcut.packageName)
                    }
                    ShortcutCard(
                        shortcut = shortcut,
                        installed = launchIntent != null,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (launchIntent == null) {
                                message = shortcut.title + " is not installed"
                            } else {
                                try {
                                    context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    message = "Opened " + shortcut.title
                                } catch (_: ActivityNotFoundException) {
                                    message = "Unable to open " + shortcut.title
                                }
                            }
                        }
                    )
                }
                repeat((if (compact) 1 else 3) - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ShortcutCard(
    shortcut: AppShortcut,
    installed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "shortcut-focus"
    )
    Surface(
        modifier.height(112.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (focused) 3.dp else 1.dp, border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(shortcut.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(shortcut.description, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
            Spacer(Modifier.weight(1f))
            Text(
                if (installed) "Ready" else "Not installed",
                color = if (installed) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    compact: Boolean,
    settingsRepository: SettingsRepository,
    gameRepository: GameRepository,
    downloadRepository: DownloadRepository
) {
    val context = LocalContext.current
    val currentSettings by settingsRepository.settings.collectAsState(initial = com.gamebox.os.settings.GameBoxSettings())
    val diagnosticGames by gameRepository.observeGames().collectAsState()
    val diagnosticDownloads by downloadRepository.observeJobs().collectAsState()
    val scope = rememberCoroutineScope()
    val externalStorageController = remember(context, settingsRepository) {
        ExternalStorageController(context, settingsRepository)
    }
    var catalogUrl by remember(currentSettings.catalogUrl) { mutableStateOf(currentSettings.catalogUrl) }
    var catalogMessage by remember { mutableStateOf<String?>(null) }
    val externalStorageStatus = externalStorageController.inspect(currentSettings.externalLibraryUri)
    val externalTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching { externalStorageController.adoptTree(uri) }
                catalogMessage = result.fold(
                    onSuccess = { it.message },
                    onFailure = { it.message ?: "External library could not be selected" }
                )
            }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        catalogMessage = if (granted) "Download notifications enabled"
        else "Notification permission was not granted"
    }
    val storageRoot = context.filesDir
    val diagnosticsReport = buildDiagnosticsReport(
        device = DiagnosticsDevice(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            sdk = Build.VERSION.SDK_INT,
            appVersion = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName ?: "unknown",
            usableBytes = storageRoot.usableSpace,
            totalBytes = storageRoot.totalSpace
        ),
        settings = currentSettings,
        games = diagnosticGames,
        downloads = diagnosticDownloads
    )
    val diagnosticsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use {
                    it.write(diagnosticsReport)
                } ?: error("Unable to open diagnostics destination")
            }
            catalogMessage = if (result.isSuccess) "Sanitized diagnostics exported"
            else "Diagnostics export failed"
        }
    }
    val totalStorage = storageRoot.totalSpace
    val usableStorage = storageRoot.usableSpace
    val settings = listOf(
        "Storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        "Controllers" to Settings.ACTION_BLUETOOTH_SETTINGS,
        "Display" to Settings.ACTION_DISPLAY_SETTINGS,
        "Audio" to Settings.ACTION_SOUND_SETTINGS,
        "Network" to Settings.ACTION_WIRELESS_SETTINGS,
        "System" to Settings.ACTION_SETTINGS
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Settings", fontSize = if (compact) 28.sp else 38.sp, fontWeight = FontWeight.Bold)
        Text(
            "GameBox configuration and safe Android system shortcuts",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "App storage: " + formatBytes(usableStorage) + " free of " + formatBytes(totalStorage),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(18.dp))
        Text("External game library", fontWeight = FontWeight.Bold)
        Text(
            externalStorageStatus.displayName ?: externalStorageStatus.message,
            color = when (externalStorageStatus.state) {
                ExternalStorageState.AVAILABLE_READ_WRITE -> MaterialTheme.colorScheme.primary
                ExternalStorageState.AVAILABLE_READ_ONLY -> MaterialTheme.colorScheme.tertiary
                ExternalStorageState.NOT_CONFIGURED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                else -> MaterialTheme.colorScheme.error
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { externalTreeLauncher.launch(null) }) {
                Text(if (currentSettings.externalLibraryUri.isBlank()) "Choose folder" else "Change folder")
            }
            if (currentSettings.externalLibraryUri.isNotBlank()) {
                OutlinedButton(onClick = {
                    scope.launch {
                        externalStorageController.forgetTree(currentSettings.externalLibraryUri)
                        catalogMessage = "External library permission removed"
                    }
                }) { Text("Forget") }
            }
        }
        Text(
            "GameBox will not move or delete files until a future migration step is explicitly confirmed.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(
                onClick = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text("Enable download notifications", modifier = Modifier.fillMaxWidth())
            }
        }
        settings.forEach { (title, action) ->
            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(Intent(action))
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(title, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { diagnosticsLauncher.launch("gamebox-diagnostics.txt") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export sanitized diagnostics", modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(18.dp))
        Text("Authorized catalog provider", fontWeight = FontWeight.Bold)
        Text(
            "Leave blank to use the bundled offline fixture. Remote catalogs must use HTTPS.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
        OutlinedTextField(
            value = catalogUrl,
            onValueChange = { catalogUrl = it },
            label = { Text("HTTPS catalog URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val trimmed = catalogUrl.trim()
                val validationError = if (trimmed.isEmpty()) null else
                    runCatching { validateAuthorizedCatalogUrl(trimmed) }.exceptionOrNull()
                if (validationError != null) {
                    catalogMessage = validationError.message ?: "Invalid catalog URL"
                } else {
                    scope.launch {
                        settingsRepository.setCatalogUrl(trimmed)
                        catalogMessage = if (trimmed.isEmpty())
                            "Bundled offline catalog selected"
                        else "Catalog URL saved. Open Store and choose Refresh."
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Save catalog source") }
        catalogMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(12.dp))
        Text(
            "Downloads, emulator profiles, saves, cloud providers, and developer diagnostics " +
                "will be added here as their subsystems become configurable.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
    }
}

private fun formatDownloadBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    val mib = safe.toDouble() / (1024.0 * 1024.0)
    return if (mib >= 1024.0) {
        String.format(java.util.Locale.US, "%.1f GB", mib / 1024.0)
    } else {
        String.format(java.util.Locale.US, "%.1f MB", mib)
    }
}

private fun formatBytes(bytes: Long): String {
    val gib = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return String.format(java.util.Locale.US, "%.1f GB", gib)
}

private fun DownloadStatus.displayName() = name.lowercase().replace('_', ' ')

private fun InstallState.displayName() = name.lowercase().replace('_', ' ')
