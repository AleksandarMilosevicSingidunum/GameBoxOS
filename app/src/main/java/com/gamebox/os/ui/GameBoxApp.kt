package com.gamebox.os.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

private enum class Destination(val title: String) {
    HOME("Home"), LIBRARY("Library"), STORE("Store"), DOWNLOADS("Downloads")
}

@Composable
fun GameBoxApp(repository: GameRepository, downloadRepository: DownloadRepository) {
    val games by repository.observeGames().collectAsState()
    var destination by remember { mutableStateOf(Destination.HOME) }
    var selectedGameId by remember { mutableStateOf<GameId?>(null) }

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
            DetailsScreen(selected, repository, downloadRepository, onBack = { selectedGameId = null })
        } else {
            when (destination) {
                Destination.HOME -> HomeScreen(games) { selectedGameId = it.id }
                Destination.LIBRARY -> CollectionScreen(
                    "Your Library", "Installed and ready offline",
                    games.filter { it.state == InstallState.INSTALLED || it.state == InstallState.UPDATE_AVAILABLE }
                ) { selectedGameId = it.id }
                Destination.STORE -> CatalogScreen(repository, games) { selectedGameId = it.id }
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
private fun HomeScreen(games: List<Game>, open: (Game) -> Unit) {
    val hero = games.first()
    Column {
        Text("Good evening", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Text("Ready to play?", fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        GameCard(hero, Modifier.fillMaxWidth().height(188.dp), true) { open(hero) }
        Spacer(Modifier.height(24.dp))
        Text("Recently played", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        GameRow(games.filter { it.lastPlayed != null }, open)
    }
}

@Composable
private fun CatalogScreen(repository: GameRepository, games: List<Game>, open: (Game) -> Unit) {
    val refreshState by repository.observeCatalogRefreshState().collectAsState()
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
        GameRow(games, open)
    }
}

@Composable
private fun CollectionScreen(title: String, subtitle: String, games: List<Game>, open: (Game) -> Unit) {
    Column {
        Text(title, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Spacer(Modifier.height(24.dp))
        GameRow(games, open)
    }
}

@Composable
private fun GameRow(games: List<Game>, open: (Game) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(games, key = { it.id.value }) { game ->
            GameCard(game, Modifier.width(230.dp).height(170.dp), onClick = { open(game) })
        }
    }
}

@Composable
private fun GameCard(game: Game, modifier: Modifier, hero: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "game-focus"
    )
    Surface(
        modifier.onFocusChanged { focused = it.isFocused }.clickable(onClick = onClick).focusable(),
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
private fun DetailsScreen(game: Game, repository: GameRepository, downloadRepository: DownloadRepository, onBack: () -> Unit) {
    Column {
        Text(game.platform.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(game.title, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text(game.genre + "  |  " + game.year + "  |  " + game.sizeMb + " MB")
        Spacer(Modifier.height(24.dp))
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Install state", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                Text(game.state.displayName(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(onClick = {
                        when (game.state) {
                            InstallState.NOT_INSTALLED, InstallState.FAILED, InstallState.MISSING_FILES ->
                                downloadRepository.enqueue(game)
                            InstallState.QUEUED, InstallState.DOWNLOADING, InstallState.VERIFYING,
                            InstallState.INSTALLING -> downloadRepository.advance(game.id)
                            InstallState.PAUSED -> downloadRepository.resume(game.id)
                            else -> Unit
                        }
                        repository.advanceInstall(game.id)
                    }) {
                        Text(game.state.primaryAction())
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
            }
        }
    }
}

@Composable
private fun DownloadsScreen(repository: GameRepository, downloadRepository: DownloadRepository) {
    val jobs by downloadRepository.observeJobs().collectAsState()
    Column {
        Text("Downloads", fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("Durable queue prototype - no network or filesystem writes yet",
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
