package com.gamebox.os.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.gamebox.os.launch.GameLaunchController
import com.gamebox.os.launch.LaunchUiState
import com.gamebox.os.storage.SaveSafetyController

private enum class Destination(val title: String) {
    HOME("Home"), LIBRARY("Library"), STORE("Store"), DOWNLOADS("Downloads")
}

@Composable
fun GameBoxApp(
    repository: GameRepository,
    downloadRepository: DownloadRepository,
    authorizedDownloadController: AuthorizedDownloadController,
    gameLaunchController: GameLaunchController,
    saveSafetyController: SaveSafetyController
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

    Column(
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
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        TopNav(destination) { destination = it; selectedGameId = null }
        Spacer(Modifier.height(28.dp))

        val selected = selectedGameId?.let(repository::game)
        if (selected != null) {
            DetailsScreen(
                selected,
                repository,
                downloadRepository,
                authorizedDownloadController,
                gameLaunchController,
                saveSafetyController,
                onBack = { selectedGameId = null }
            )
        } else {
            when (destination) {
                Destination.HOME -> HomeScreen(games, restorableGameId, rememberGameFocus) { selectedGameId = it.id }
                Destination.LIBRARY -> CollectionScreen(
                    "Your Library", "Installed and ready offline",
                    games.filter { it.state == InstallState.INSTALLED || it.state == InstallState.UPDATE_AVAILABLE },
                    restorableGameId,
                    rememberGameFocus
                ) { selectedGameId = it.id }
                Destination.STORE -> CatalogScreen(repository, games, restorableGameId, rememberGameFocus) { selectedGameId = it.id }
                Destination.DOWNLOADS -> DownloadsScreen(repository, downloadRepository)
            }
        }

        Spacer(Modifier.weight(1f))
        Text("A Select    B Back    LB/RB Tabs    Menu Options",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 14.sp)
    }
}

@Composable
private fun TopNav(selected: Destination, onSelect: (Destination) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("GAMEBOX", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black,
            fontSize = 28.sp, modifier = Modifier.padding(end = 28.dp, top = 8.dp))
        Destination.entries.forEach { item ->
            Button(
                onClick = { onSelect(item) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item == selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
            ) { Text(item.title) }
        }
    }
}

@Composable
private fun HomeScreen(
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
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
        Text("Ready to play?", fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        GameCard(
            hero,
            Modifier.fillMaxWidth().height(188.dp),
            hero = true,
            restoreFocus = focusTarget == hero.id,
            onFocused = onFocused
        ) { open(hero) }
        Spacer(Modifier.height(24.dp))
        Text("Recently played", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        GameRow(games.filter { it.lastPlayed != null && it.id != hero.id }, focusTarget, onFocused, open)
    }
}

@Composable
private fun CatalogScreen(
    repository: GameRepository,
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    open: (Game) -> Unit
) {
    val refreshState by repository.observeCatalogRefreshState().collectAsState()
    val focusTarget = restoreGameId?.takeIf { id -> games.any { it.id == id } }
        ?: games.firstOrNull()?.id
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Authorized Catalog", fontSize = 38.sp, fontWeight = FontWeight.Bold)
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
        Spacer(Modifier.height(24.dp))
        GameRow(games, focusTarget, onFocused, open)
    }
}

@Composable
private fun CollectionScreen(
    title: String,
    subtitle: String,
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    open: (Game) -> Unit
) {
    val focusTarget = restoreGameId?.takeIf { id -> games.any { it.id == id } }
        ?: games.firstOrNull()?.id
    Column {
        Text(title, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Spacer(Modifier.height(24.dp))
        GameRow(games, focusTarget, onFocused, open)
    }
}

@Composable
private fun GameRow(
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
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
                Modifier.width(230.dp).height(170.dp),
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
            Text(game.title, fontSize = if (hero) 32.sp else 21.sp, fontWeight = FontWeight.Bold)
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
    gameLaunchController: GameLaunchController,
    saveSafetyController: SaveSafetyController,
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
    Column {
        Text(game.platform.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(game.title, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text(game.genre + "  |  " + game.year + "  |  " + game.sizeMb + " MB")
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
                                        downloadRepository.enqueue(game)
                                    InstallState.QUEUED, InstallState.DOWNLOADING, InstallState.VERIFYING,
                                    InstallState.INSTALLING -> downloadRepository.advance(game.id)
                                    InstallState.PAUSED -> downloadRepository.resume(game.id)
                                    InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE ->
                                        gameLaunchController.launch(game)
                                }
                                repository.advanceInstall(game.id)
                            }
                        },
                        enabled = !isAuthorizedTest || !workerActive
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
                if (isAuthorizedTest && saveSafetyState.operationMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        saveSafetyState.operationMessage,
                        color = if (saveSafetyState.operationSuccessful)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
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
private fun DownloadsScreen(repository: GameRepository, downloadRepository: DownloadRepository) {
    val jobs by downloadRepository.observeJobs().collectAsState()
    Column {
        Text("Downloads", fontSize = 38.sp, fontWeight = FontWeight.Bold)
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
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        if (job.status !in setOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)) {
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

private fun DownloadStatus.displayName() = name.lowercase().replace('_', ' ')

private fun InstallState.displayName() = name.lowercase().replace('_', ' ')
