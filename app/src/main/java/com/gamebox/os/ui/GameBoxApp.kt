@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.provider.OpenableColumns
import android.view.KeyEvent as AndroidKeyEvent
import android.view.InputDevice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.DownloadRepository
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.CatalogDiscoveryRepository
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.importer.AuthorizedRomImporter
import com.gamebox.os.importer.RomImportPolicy
import com.gamebox.os.importer.RomImportResult
import com.gamebox.os.importer.RomImportSetResult
import com.gamebox.os.importer.RomImportSource
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.belongsToLibrary
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.LocalContentFile
import com.gamebox.os.domain.primaryAction
import com.gamebox.os.domain.summarizeLibrary
import com.gamebox.os.domain.normalizeCatalogTitle
import com.gamebox.os.download.AuthorizedDownloadController
import com.gamebox.os.download.AuthorizedDownloadState
import com.gamebox.os.download.RemoteDownloadController
import com.gamebox.os.download.DownloadTelemetryTracker
import com.gamebox.os.download.assessDownloadCapacity
import com.gamebox.os.download.formatCapacityWarning
import com.gamebox.os.download.formatDownloadTelemetry
import com.gamebox.os.launch.GameLaunchController
import com.gamebox.os.launch.MoonlightConnectivity
import com.gamebox.os.launch.MoonlightStatus
import com.gamebox.os.launch.addRecentMoonlightSession
import com.gamebox.os.launch.classifyMoonlightConnectivity
import com.gamebox.os.launch.LaunchUiState
import com.gamebox.os.storage.SaveSafetyController
import com.gamebox.os.storage.ExternalStorageController
import com.gamebox.os.storage.ExternalStorageState
import com.gamebox.os.settings.SettingsRepository
import com.gamebox.os.catalog.validateAuthorizedCatalogUrl
import com.gamebox.os.catalog.CatalogSyncResult
import com.gamebox.os.catalog.legalSourceLinks
import com.gamebox.os.save.CloudSaveEndpointPolicy
import com.gamebox.os.save.CloudSaveProvider
import com.gamebox.os.diagnostics.DiagnosticsDevice
import com.gamebox.os.diagnostics.DiagnosticEventCollector
import com.gamebox.os.diagnostics.buildDiagnosticsReport
import com.gamebox.os.diagnostics.buildDiagnosticsRecoveryBundle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal enum class Destination(val title: String) {
    HOME("Home"), LIBRARY("Library"), STORE("Store"), DOWNLOADS("Downloads"),
    MEDIA("Media"), PC("PC"), SETTINGS("Settings")
}

private data class StoreConsole(
    val key: String,
    val label: String,
    val theGamesDbName: String?,
    val aliases: Set<String>,
    val icon: ImageVector,
)

private val storeConsoles = listOf(
    StoreConsole("ps2", "PS2", "Sony Playstation 2", setOf("sonyplaystation2", "playstation2", "ps2"), Icons.Rounded.SportsEsports),
    StoreConsole("gamecube", "GameCube", "Nintendo GameCube", setOf("nintendogamecube", "gamecube"), Icons.Rounded.SportsEsports),
    StoreConsole("wii", "Wii", "Nintendo Wii", setOf("nintendowii", "wii"), Icons.Rounded.SportsEsports),
    StoreConsole("psp", "PSP", "Sony Playstation Portable", setOf("sonyplaystationportable", "playstationportable", "psp"), Icons.Rounded.SportsEsports),
    StoreConsole("dreamcast", "Dreamcast", "Sega Dreamcast", setOf("segadreamcast", "dreamcast"), Icons.Rounded.SportsEsports),
    StoreConsole("3ds", "3DS", "Nintendo 3DS", setOf("nintendo3ds", "3ds"), Icons.Rounded.SportsEsports),
    StoreConsole("switch", "Switch", "Nintendo Switch", setOf("nintendoswitch", "switch"), Icons.Rounded.SportsEsports),
    StoreConsole("homebrew", "Homebrew", null, emptySet(), Icons.Rounded.Code),
)

private fun storeConsoleMatches(console: StoreConsole, platformName: String): Boolean =
    normalizeCatalogTitle(platformName) in console.aliases

@Composable
fun GameBoxApp(
    repository: GameRepository,
    downloadRepository: DownloadRepository,
    authorizedDownloadController: AuthorizedDownloadController,
    remoteDownloadController: RemoteDownloadController,
    gameLaunchController: GameLaunchController,
    saveSafetyController: SaveSafetyController,
    settingsRepository: SettingsRepository,
    catalogDiscoveryRepository: CatalogDiscoveryRepository,
    authorizedRomImporter: AuthorizedRomImporter,
    managedSaveDiscovery: com.gamebox.os.storage.ManagedSaveDiscovery,
    saveControllerFactory: ((GameId, kotlinx.coroutines.CoroutineScope) -> SaveSafetyController)? = null
) {
    val games by repository.observeGames().collectAsState()
    val uiState = rememberGameBoxUiState()
    val destination = runCatching { Destination.valueOf(uiState.destination) }
        .getOrDefault(Destination.HOME)
    val selectedGameId = uiState.selectedGameId?.let(::GameId)
    val restorableGameId = uiState.restoreFocus(destination.name, games.map { it.id.value })?.let(::GameId)
    val rememberGameFocus: (GameId) -> Unit = { uiState.rememberFocus(destination.name, it.value) }

    fun moveTab(offset: Int) {
        val tabs = Destination.entries
        val target = tabs[(destination.ordinal + offset + tabs.size) % tabs.size]
        uiState.openDestination(target.name)
    }

    BlueprintViewport {
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_BUTTON_L1 -> { moveTab(-1); true }
                    AndroidKeyEvent.KEYCODE_BUTTON_R1 -> { moveTab(1); true }
                    AndroidKeyEvent.KEYCODE_BUTTON_X -> { uiState.openDestination(Destination.STORE.name); true }
                    AndroidKeyEvent.KEYCODE_BUTTON_Y -> { uiState.openDestination(Destination.SETTINGS.name); true }
                    AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.KEYCODE_BUTTON_B ->
                        if (selectedGameId != null) { uiState.clearSelection(); true } else false
                    else -> false
                }
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF060B12),
                        Color(0xFF03080D),
                        Color(0xFF050A10),
                    )
                )
            )
    ) {
        val compact = maxWidth < 900.dp || maxHeight < 440.dp
        if (!compact) {
            Box(
                Modifier
                    .size(620.dp)
                    .offset(x = (-260).dp, y = (-360).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0x125B8CFF), Color.Transparent)
                        ),
                        CircleShape,
                    )
            )
            Box(
                Modifier
                    .size(520.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 260.dp, y = 260.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0x108B5CF6), Color.Transparent)
                        ),
                        CircleShape,
                    )
            )
        }
        Column(
            Modifier.fillMaxSize().padding(
                horizontal = if (compact) 16.dp else 24.dp,
                vertical = if (compact) 10.dp else 14.dp
            )
        ) {
            TopNav(destination, compact) { uiState.openDestination(it.name) }
            SessionRecoveryBanner(gameLaunchController)
            Spacer(Modifier.height(if (compact) 14.dp else 12.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                BlueprintScreenTransition(destination.name + ":" + (selectedGameId?.value ?: "root")) {
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
                            managedSaveDiscovery,
                            importer = authorizedRomImporter,
                            saveControllerFactory = saveControllerFactory,
                            compact = compact,
                            onDownloads = { uiState.openDestination(Destination.DOWNLOADS.name) },
                            onBack = uiState::clearSelection
                        )
                    } else {
                        when (destination) {
                        Destination.HOME -> HomeScreen(
                            games, restorableGameId, rememberGameFocus, compact,
                            openPc = { uiState.openDestination(Destination.PC.name) }
                        ) { uiState.openGame(it.id.value) }
                        Destination.LIBRARY -> CollectionScreen(
                            "Your Library", "Installed games and retained imports",
                            games.filter { it.belongsToLibrary() },
                            restorableGameId,
                            rememberGameFocus,
                            compact
                        ) { uiState.openGame(it.id.value) }
                        Destination.STORE -> CatalogScreen(
                            repository, catalogDiscoveryRepository, authorizedRomImporter, games, restorableGameId, rememberGameFocus, compact
                        ) { uiState.openGame(it.id.value) }
                        Destination.DOWNLOADS -> DownloadsScreen(repository, downloadRepository, remoteDownloadController, compact)
                        Destination.MEDIA -> AppHubScreen(
                            "Media",
                            "Launch your living-room apps and return to GameBox",
                            mediaShortcuts,
                            compact,
                            settingsRepository
                        )
                        Destination.PC -> AppHubScreen(
                            "PC Hub",
                            "Streaming, Windows, Linux, files, browser, and desktop tools",
                            pcShortcuts,
                            compact,
                            settingsRepository
                        )
                        Destination.SETTINGS -> SettingsScreen(compact, settingsRepository, repository, downloadRepository)
                        }
                    }
                }
            }

            if (!compact) {
                Spacer(Modifier.height(8.dp))
                ControllerFooter()
            }
        }
    }
}
}

@Composable
private fun BlueprintScreenTransition(screenKey: String, content: @Composable () -> Unit) {
    key(screenKey) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 20 },
        ) {
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}

@Composable
private fun TopNav(selected: Destination, compact: Boolean, onSelect: (Destination) -> Unit) {
    var now by remember { mutableStateOf(java.time.LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = java.time.LocalDateTime.now()
            kotlinx.coroutines.delay(30_000)
        }
    }
    if (compact) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameBoxLogo(selected)
                Text(
                    now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Destination.entries.forEach { item -> NavButton(item, selected, onSelect) }
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameBoxLogo(selected)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Destination.entries.forEach { item -> NavButton(item, selected, onSelect) }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(onClick = { onSelect(Destination.STORE) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Search, contentDescription = "Browse and search games", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onSelect(Destination.DOWNLOADS) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Download, contentDescription = "Open download activity", modifier = Modifier.size(18.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        now.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    Text(
                        now.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameBoxLogo(selected: Destination) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GameBoxBrandMark(
            Modifier.size(30.dp).semantics { contentDescription = "GameBox logo" }
        )
        Spacer(Modifier.width(9.dp))
        Column {
            Text("GameBox", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text(
                "GAMEBOX OS / ${selected.title.uppercase()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
internal fun NavButton(item: Destination, selected: Destination, onSelect: (Destination) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val emphasized = item == selected || hovered || focused
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else if (hovered || focused) 1.025f else 1f,
        label = "nav-scale"
    )
    val border by animateColorAsState(
        if (emphasized) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "nav-border",
    )
    val fill by animateColorAsState(
        if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
        label = "nav-fill",
    )
    Surface(
        color = fill,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (emphasized) blueprintFocusBrush() else Brush.linearGradient(listOf(border, border))),
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(interactionSource)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null) { onSelect(item) }
            .semantics {
                contentDescription = item.title + " tab"
                role = Role.Tab
                this.selected = item == selected
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                item.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                maxLines = 1, fontSize = 12.sp,
                fontWeight = if (item == selected) FontWeight.Bold else FontWeight.Medium,
                color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item == selected) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp)
                    .width(26.dp).height(3.dp).clip(RoundedCornerShape(50)).background(blueprintFocusBrush()))
            }
        }
    }
}

private fun destinationIcon(item: Destination): ImageVector = when (item) {
    Destination.HOME -> Icons.Rounded.Home
    Destination.LIBRARY -> Icons.Rounded.VideoLibrary
    Destination.STORE -> Icons.Rounded.Storefront
    Destination.DOWNLOADS -> Icons.Rounded.Download
    Destination.MEDIA -> Icons.Rounded.Movie
    Destination.PC -> Icons.Rounded.DesktopWindows
    Destination.SETTINGS -> Icons.Rounded.Settings
}

@Composable
private fun ControllerFooter() {
    Row(
        Modifier.fillMaxWidth().height(30.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ControllerHint("A", "Select", Color(0xFF78D64B))
            ControllerHint("B", "Back", Color(0xFFFF4D5E))
            ControllerHint("X", "Store", Color(0xFF3C8DFF))
            ControllerHint("Y", "Settings", Color(0xFFFFC43D))
            Text("LB/RB  Change tab", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(25.dp),
            )
            Column {
                Text("Local player", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(connectedControllerLabel(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ControllerHint(letter: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, color), modifier = Modifier.size(17.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(letter, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
private fun HomeScreen(
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    openPc: () -> Unit,
    open: (Game) -> Unit
) {
    if (games.isEmpty()) {
BlueprintPanel(Modifier.fillMaxWidth()) {
            Text("Welcome to GameBox", fontWeight = FontWeight.Bold)
            Text("Open Store to browse your catalog, then install an authorized title or import your own game.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val context = LocalContext.current
    val installed = games.filter { it.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE) }
    val history = installed.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed }
    val hero = history.firstOrNull() ?: installed.firstOrNull()
        ?: games.firstOrNull { it.id.value == "galaxy-patrol" } ?: games.first()
    val focusTarget = restoreGameId?.takeIf { id -> games.any { it.id == id } } ?: hero.id
    val networkService = context.getSystemService(ConnectivityManager::class.java)
    val network = networkService?.getNetworkCapabilities(networkService.activeNetwork)
    val networkLabel = if (network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) "Connected" else "Offline"
    val storage = context.filesDir
    val usedPercent = if (storage.totalSpace > 0L) ((storage.totalSpace - storage.usableSpace) * 100L / storage.totalSpace).toInt() else 0

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (history.isEmpty()) "Ready to play" else "Continue Playing",
            fontSize = if (compact) 22.sp else 16.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GameCard(
                hero, Modifier.weight(1f).height(if (compact) 224.dp else 212.dp),
                hero = true, restoreFocus = focusTarget == hero.id, onFocused = onFocused
            ) { open(hero) }
            if (!compact) HomeStatusPanel(networkLabel, usedPercent, Build.MODEL, connectedControllerLabel())
        }
        if (compact) {
            HomeGameSection("In your library", installed, focusTarget.takeIf { it != hero.id }, onFocused, true, open)
            HomeQuickLaunchRow(openPc)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(1.65f)) {
                    Text("Your collection", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (installed.isEmpty()) {
                        Text("Install a title from Store to start your collection.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(3.dp)) {
                            items(installed.take(8), key = { it.id.value }) { game ->
                                GameCard(game, Modifier.width(116.dp).height(140.dp), poster = true,
                                    restoreFocus = focusTarget == game.id && game.id != hero.id,
                                    onFocused = onFocused) { open(game) }
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) { HomeQuickLaunchRow(openPc, compact = false) }
            }
        }
        HomeGameSection(if (history.isEmpty()) "Explore games" else "Recently played",
            if (history.isEmpty()) games else history,
            focusTarget.takeIf { id -> id != hero.id && (if (compact) installed else installed.take(8)).none { it.id == id } },
            onFocused, compact, open)
    }
}

@Composable
private fun HomeGameSection(
    title: String, games: List<Game>, focusTarget: GameId?, onFocused: (GameId) -> Unit,
    compact: Boolean, open: (Game) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = if (compact) 18.sp else 15.sp, fontWeight = FontWeight.SemiBold)
            Text("${games.size} games", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (games.isEmpty()) Text("Your games will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        else GameRow(games, focusTarget, onFocused, compact, open)
    }
}

@Composable
internal fun HomeQuickLaunchRow(openPc: () -> Unit, compact: Boolean = true) {
    Text("Quick launch", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(11.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "Desktop" to Color(0xFF6159D7),
            "Steam" to Color(0xFF18558E),
            "Xbox" to Color(0xFF16722E),
            "Epic Games" to Color(0xFF252B37),
        ).forEach { (label, accent) ->
            Surface(
                color = accent, shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .12f)),
                modifier = Modifier.weight(1f).heightIn(min = if (compact) 140.dp else 100.dp).blueprintClick(openPc)
                    .semantics { contentDescription = "Quick launch $label; opens PC Hub" }
            ) {
                Column(
                    Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .40f)))).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AppBrandMark(label, Modifier.size(32.dp))
                    Spacer(Modifier.height(if (compact) 18.dp else 8.dp))
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("PC Hub", color = Color.White.copy(alpha = .7f), fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun HomeStatusPanel(networkLabel: String, storagePercent: Int, deviceModel: String, controllerLabel: String) {
    Column(Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeStatusItem(Icons.Rounded.Storage, "Storage", "$storagePercent% used", storagePercent / 100f)
        HomeStatusItem(Icons.Rounded.Wifi, "Network", networkLabel)
        HomeStatusItem(Icons.Rounded.SportsEsports, "Controller", controllerLabel)
        HomeStatusItem(Icons.Rounded.Smartphone, "Device", deviceModel)
    }
}

@Composable
private fun HomeStatusItem(icon: ImageVector, label: String, value: String, progress: Float? = null) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .80f), shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Column {
                    Text(label, fontSize = 10.sp, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (progress != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(3.dp))
            }
        }
    }
}

@Composable
private fun CatalogScreen(
    repository: GameRepository,
    discoveryRepository: CatalogDiscoveryRepository,
    authorizedRomImporter: AuthorizedRomImporter,
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    open: (Game) -> Unit
) {
    val refreshState by repository.observeCatalogRefreshState().collectAsState()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val discoveryPlatforms by discoveryRepository.observePlatforms().collectAsState(initial = emptyList())
    var selectedConsoleKey by remember { mutableStateOf<String?>(null) }
    val selectedConsole = storeConsoles.firstOrNull { it.key == selectedConsoleKey }
    val discoveryPlatformId = when {
        selectedConsole == null -> null
        selectedConsole.theGamesDbName == null -> "__homebrew__"
        else -> discoveryPlatforms.firstOrNull { platform ->
            storeConsoleMatches(selectedConsole, platform.name)
        }?.id ?: "__pending_${selectedConsole.key}"
    }
val allDiscoveryGames by discoveryRepository.observeGames(null, "", 250).collectAsState(initial = emptyList())
    val discoveryGames by discoveryRepository.observeGames(discoveryPlatformId, query, 250)
        .collectAsState(initial = emptyList())
    var selectedDiscoveryId by remember { mutableStateOf<GameId?>(null) }
    val selectedDiscovery = selectedDiscoveryId?.let { id ->
        discoveryGames.firstOrNull { it.id == id }
    }
    var discoverySyncMessage by remember { mutableStateOf<String?>(null) }
    var discoverySyncing by remember { mutableStateOf(false) }
    var discoverySyncProgress by remember { mutableStateOf(0f) }
    var platform by remember { mutableStateOf<String?>(null) }
    var genre by remember { mutableStateOf<String?>(null) }
    var region by remember { mutableStateOf<String?>(null) }
    var language by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    val filtered = filterGames(
        games, query, platform, genre, favoritesOnly,
        region = region, language = language,
    )
    val focusTarget = restoreGameId?.takeIf { id -> filtered.any { it.id == id } }
        ?: filtered.firstOrNull()?.id
    if (selectedDiscovery != null) {
        DiscoveryDetailsScreen(
            game = selectedDiscovery,
            platformName = selectedConsole?.label
                ?: discoveryPlatforms.firstOrNull { it.id == selectedDiscovery.platformId }?.name
                ?: selectedDiscovery.platformId,
            onBack = { selectedDiscoveryId = null },
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
        )
        return
    }
    fun syncDiscovery() {
        discoverySyncing = true
        discoverySyncProgress = 0f
        val targets = selectedConsole?.let { listOf(it) }
            ?: storeConsoles.filter { it.theGamesDbName != null }
        scope.launch {
            var totalGames = 0
            var message = ""
            targets.forEachIndexed { index, console ->
                val providerName = console.theGamesDbName
                if (providerName != null) {
                    discoverySyncMessage = "Syncing " + console.label + " (" + (index + 1) + "/" + targets.size + ")…"
                    when (val result = discoveryRepository.syncPlatform(providerName)) {
                        is CatalogSyncResult.Success -> totalGames += result.games
                        CatalogSyncResult.MissingApiKey -> message = "Add your TheGamesDB API key in Settings"
                        is CatalogSyncResult.PlatformNotFound -> message = console.label + " was not found in TheGamesDB"
                        is CatalogSyncResult.Failed -> message = console.label + " sync failed: " + result.reason
                    }
                }
                discoverySyncProgress = (index + 1).toFloat() / targets.size.coerceAtLeast(1).toFloat()
            }
            discoverySyncMessage = if (message.isNotEmpty()) message
            else "Cached " + totalGames + " games (up to 20 per console)"
            discoverySyncing = false
        }
    }
    if (!compact) {
        BlueprintCatalogScreen(
            authorizedGames = filtered,
            discoveryGames = discoveryGames,
            allDiscoveryGames = allDiscoveryGames,
            discoveryPlatformNames = discoveryPlatforms.associate { it.id to it.name },
            query = query,
            onQuery = { query = it },
            selectedConsole = selectedConsole,
            onSelectConsole = { selectedConsoleKey = it?.key },
            refreshState = refreshState,
            discoverySyncing = discoverySyncing,
            discoverySyncProgress = discoverySyncProgress,
            discoverySyncMessage = discoverySyncMessage,
            onRefresh = repository::refreshCatalog,
            onSync = ::syncDiscovery,
            openAuthorized = open,
            openDiscovery = { selectedDiscoveryId = it.id },
        )
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text("Authorized Catalog", fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
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
        }
        when (refreshState) {
            CatalogRefreshState.OFFLINE_FALLBACK -> Text(
                "Offline - bundled catalog loaded; installed games remain playable",
                modifier = Modifier.semantics {
                    contentDescription = "Offline; bundled catalog loaded; installed games remain playable"
                    liveRegion = LiveRegionMode.Polite
                },
                color = MaterialTheme.colorScheme.tertiary
            )
            CatalogRefreshState.REMOTE_FALLBACK -> Text(
                "Provider unavailable - bundled catalog loaded. Choose Refresh to retry.",
                modifier = Modifier.semantics {
                    contentDescription = "Catalog provider unavailable; bundled catalog loaded; choose Refresh to retry"
                    liveRegion = LiveRegionMode.Assertive
                },
                color = MaterialTheme.colorScheme.tertiary
            )
            CatalogRefreshState.ERROR -> Text(
                "Refresh failed - cached catalog remains available. Choose Refresh to retry.",
                modifier = Modifier.semantics {
                    contentDescription = "Catalog refresh failed; cached catalog remains available; choose Refresh to retry"
                    liveRegion = LiveRegionMode.Assertive
                },
                color = MaterialTheme.colorScheme.error
            )
            else -> Unit
        }
        if (refreshState == CatalogRefreshState.SUCCESS) {
            Text("Catalog refreshed; local install and play state preserved",
                modifier = Modifier.semantics {
                    contentDescription = "Catalog refresh succeeded; local install and play state preserved"
                    liveRegion = LiveRegionMode.Polite
                },
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        GameFilterBar(
            games, query, { query = it }, platform, { platform = it },
            genre, { genre = it }, favoritesOnly, { favoritesOnly = it },
            showMetadataFilters = true,
            region = region,
            onRegion = { region = it },
            language = language,
            onLanguage = { language = it },
        )
        Spacer(Modifier.height(16.dp))
        if (filtered.isEmpty()) Text("No authorized games match these filters")
        else GameRow(filtered, focusTarget, onFocused, compact, open)

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column {
                Text("Discover with TheGamesDB", fontSize = if (compact) 22.sp else 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Metadata, box art and screenshots — import an authorized copy to play",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            }
            Button(
                enabled = !discoverySyncing && (selectedConsole == null || selectedConsole.theGamesDbName != null),
                onClick = ::syncDiscovery,
            ) {
                Text(
                    when {
                        discoverySyncing -> "Syncing…"
                        selectedConsole?.theGamesDbName == null && selectedConsole != null -> "Local homebrew"
                        selectedConsole == null -> "Sync all consoles"
                        else -> "Sync " + selectedConsole.label
                    }
                )
            }
        }
        if (discoverySyncing) {
            LinearProgressIndicator(
                progress = { discoverySyncProgress },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        discoverySyncMessage?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = message
                },
            )
        }
        Text("Consoles", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedConsole == null,
                onClick = { selectedConsoleKey = null },
                label = { Text("All") },
            )
            storeConsoles.forEach { console ->
                FilterChip(
                    selected = selectedConsoleKey == console.key,
                    onClick = { selectedConsoleKey = console.key },
                    leadingIcon = {
                        ConsoleBrandMark(
                            key = console.key,
                            selected = selectedConsoleKey == console.key,
                            modifier = Modifier.size(17.dp),
                        )
                    },
                    label = { Text(console.label) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (discoveryGames.isEmpty()) {
            Text(
                if (selectedConsole?.theGamesDbName == null) {
                    "Homebrew is local-first. Add authorized homebrew files through the Library importer."
                } else {
                    "No cached games for this console. Add an API key in Settings, then sync up to 20 titles."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            DiscoveryGameRow(discoveryGames, compact) { selectedDiscoveryId = it.id }
        }
    }
}

@Composable
private fun BlueprintCatalogScreen(
    authorizedGames: List<Game>,
    discoveryGames: List<DiscoveryGame>,
    query: String,
    onQuery: (String) -> Unit,
    selectedConsole: StoreConsole?,
    onSelectConsole: (StoreConsole?) -> Unit,
    refreshState: CatalogRefreshState,
    discoverySyncing: Boolean,
    discoverySyncProgress: Float,
    discoverySyncMessage: String?,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    openAuthorized: (Game) -> Unit,
    openDiscovery: (DiscoveryGame) -> Unit,
    allDiscoveryGames: List<DiscoveryGame> = discoveryGames,
    discoveryPlatformNames: Map<String, String> = emptyMap(),
) {
    var favoritesOnly by remember { mutableStateOf(false) }
    var installedOnly by remember { mutableStateOf(false) }
    var sortByRating by remember { mutableStateOf(false) }
    var genreFilter by remember { mutableStateOf<String?>(null) }
    var regionFilter by remember { mutableStateOf<String?>(null) }
    var languageFilter by remember { mutableStateOf<String?>(null) }
    val genres = authorizedGames.map { it.genre }.filter(String::isNotBlank).distinct().sorted()
    val regions = (authorizedGames.mapNotNull { it.region } + allDiscoveryGames.mapNotNull { it.region })
        .filter(String::isNotBlank).distinct().sorted()
    val languages = authorizedGames.mapNotNull { it.language }.filter(String::isNotBlank).distinct().sorted()
    fun cycle(current: String?, values: List<String>): String? {
        if (values.isEmpty()) return null
        val index = current?.let(values::indexOf) ?: -1
        return if (index < 0) values.first() else values.getOrNull(index + 1)
    }
    val consoleGames = authorizedGames.filter { game ->
        selectedConsole == null || storeConsoleMatches(selectedConsole, game.platform) ||
            (selectedConsole.key == "homebrew" && normalizeCatalogTitle(game.platform) in setOf("homebrew", "retro", "android", "sourceport"))
    }
    val visibleAuthorized = consoleGames.filter { game ->
        (!favoritesOnly || game.favorite) &&
            (!installedOnly || game.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE)) &&
            matchesStoreMetadataFilters(
                game.genre, game.region, game.language,
                genreFilter, regionFilter, languageFilter,
            )
    }.sortedBy { it.title.lowercase() }
    val visibleDiscovery = discoveryGames.filter { game ->
        (!favoritesOnly || game.favorite) &&
            matchesStoreMetadataFilters(
                genre = null, region = game.region, language = null,
                genreFilter, regionFilter, languageFilter,
            )
    }.let { games ->
        if (sortByRating) games.sortedByDescending { it.rating ?: -1.0 } else games.sortedBy { it.title.lowercase() }
    }
    val featuredDiscovery = visibleDiscovery.firstOrNull().takeUnless { installedOnly }
    val featuredAuthorized = visibleAuthorized.firstOrNull()
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val railWidth = (maxWidth * 0.15f).coerceIn(124.dp, 166.dp)
    val sidebarWidth = (maxWidth * 0.18f).coerceIn(160.dp, 206.dp)
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BlueprintRail(Modifier.width(railWidth)) {
            Text("CONSOLES", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            BlueprintRailItem("All", allDiscoveryGames.size + authorizedGames.size, selectedConsole == null, Icons.Rounded.GridView) {
                onSelectConsole(null)
            }
            storeConsoles.forEach { console ->
                BlueprintRailItem(
                    console.label,
                    allDiscoveryGames.count { storeConsoleMatches(console, discoveryPlatformNames[it.platformId].orEmpty()) } +
                        authorizedGames.count { storeConsoleMatches(console, it.platform) ||
                            (console.key == "homebrew" && normalizeCatalogTitle(it.platform) in setOf("homebrew", "retro", "android", "sourceport")) },
                    selectedConsole?.key == console.key,
                    console.icon,
                    brandKey = console.key,
                ) {
                    onSelectConsole(console)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            Text("FILTERS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            FilterChip(
                selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly },
                label = { Text("Favorites", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Rounded.FavoriteBorder, null, Modifier.size(14.dp)) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = { genreFilter = cycle(genreFilter, genres) },
                enabled = genres.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(30.dp),
            ) { Text("Genre: " + (genreFilter ?: "All"), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            TextButton(
                onClick = { regionFilter = cycle(regionFilter, regions) },
                enabled = regions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(30.dp),
            ) { Text("Region: " + (regionFilter ?: "All"), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            TextButton(
                onClick = { languageFilter = cycle(languageFilter, languages) },
                enabled = languages.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(30.dp),
            ) { Text("Language: " + (languageFilter ?: "All"), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            TextButton(onClick = { sortByRating = !sortByRating }, modifier = Modifier.fillMaxWidth().height(30.dp)) {
                Text(if (sortByRating) "Sort: Rating ↓" else "Sort: Title A–Z", fontSize = 10.sp)
            }
            TextButton(onClick = {
                favoritesOnly = false; installedOnly = false; sortByRating = false
                genreFilter = null; regionFilter = null; languageFilter = null
                onQuery(""); onSelectConsole(null)
            }, modifier = Modifier.fillMaxWidth().height(30.dp)) {
                Text("Clear filters", fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.CloudDone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            Text("${allDiscoveryGames.size} cached titles", fontSize = 10.sp)
            Text("Browse your saved catalog offline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }

        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.weight(1f).height(204.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
            ) {
                Box(Modifier.fillMaxSize()) {
                    RemoteArtwork(featuredDiscovery?.backgroundUrl ?: featuredDiscovery?.coverUrl ?: featuredAuthorized?.artworkUrl, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF2040711), Color(0xB0040711), Color(0x18040711)))))
                    Column(Modifier.fillMaxSize().padding(18.dp)) {
                        Text("FEATURED", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(featuredDiscovery?.title ?: featuredAuthorized?.title ?: "Find your next adventure", fontSize = 27.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(
                            featuredDiscovery?.description?.take(110)
                                ?: featuredAuthorized?.description?.take(110)
                                ?: "Browse local-first metadata and authorized sources.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 2,
                            modifier = Modifier.widthIn(max = 430.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(enabled = featuredDiscovery != null || featuredAuthorized != null, onClick = {
                            if (featuredDiscovery != null) openDiscovery(featuredDiscovery)
                            else if (featuredAuthorized != null) openAuthorized(featuredAuthorized)
                        }) { Text("View Details  ›", fontSize = 10.sp) }
                    }
                }
            }
            Column(Modifier.width(sidebarWidth), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilterChip(selected = !installedOnly, onClick = { installedOnly = false }, label = { Text("All", fontSize = 10.sp) })
                    FilterChip(selected = installedOnly, onClick = { installedOnly = true }, label = { Text("Installed", fontSize = 10.sp) })
                }
                Text(selectedConsole?.label ?: "All consoles", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("${if (installedOnly) visibleAuthorized.size else visibleDiscovery.size + visibleAuthorized.size} matching titles", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                OutlinedTextField(
                    value = query, onValueChange = onQuery,
                    leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(16.dp)) },
                    placeholder = { Text("Search games", fontSize = 11.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
                Text("Metadata includes artwork and details. Open a title to import a copy or view its sources.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 14.sp)
            }
            }
            if (!installedOnly) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (sortByRating) "Top Rated" else "Discover Games", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${visibleDiscovery.size} titles", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
            if (visibleDiscovery.isEmpty()) {
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text(if (favoritesOnly || query.isNotBlank()) "No discovered games match these filters." else "Sync this console to add games, box art and screenshots from TheGamesDB.", fontSize = 11.sp)
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
                Text(if (installedOnly) "Installed Games" else "Your Game Sources", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${visibleAuthorized.size} titles", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
            if (visibleAuthorized.isEmpty()) {
                Text(if (installedOnly) "No installed games match this console. Open a discovered title to import your game." else "No connected game sources match these filters.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visibleAuthorized, key = { it.id.value }) { game ->
                        BlueprintGameTile(game, false, { _ -> }, openAuthorized)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onSync,
                enabled = !discoverySyncing && (selectedConsole == null || selectedConsole.theGamesDbName != null),
                modifier = Modifier.weight(1f),
            ) { Text(if (discoverySyncing) "Syncing…" else "Sync ${selectedConsole?.label ?: "all consoles"}", fontSize = 10.sp) }
            OutlinedButton(
                onClick = onRefresh,
                enabled = refreshState != CatalogRefreshState.REFRESHING,
                modifier = Modifier.weight(1f),
            ) { Text(if (refreshState == CatalogRefreshState.REFRESHING) "Refreshing…" else "Refresh authorized catalog", fontSize = 10.sp) }
            }
            if (discoverySyncing) {
                LinearProgressIndicator(progress = { discoverySyncProgress }, modifier = Modifier.fillMaxWidth())
            }
            discoverySyncMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp) }
        }
    }
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
    val summary = summarizeLibrary(games)
    if (!compact) {
        BlueprintLibraryScreen(
            games = games,
            filtered = filtered,
            query = query,
            onQuery = { query = it },
            platform = platform,
            onPlatform = { platform = it },
            favoritesOnly = favoritesOnly,
            onFavoritesOnly = { favoritesOnly = it },
            focusTarget = focusTarget,
            onFocused = onFocused,
            open = open,
        )
        return
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(title, fontSize = if (compact) 28.sp else 38.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).semantics {
                contentDescription = summary.totalGames.toString() + " games, " +
                    summary.installedGames + " installed, " + summary.favorites + " favorites, " +
                    summary.totalHoursPlayed + " hours played"
            }
        ) {
            Column(Modifier.padding(if (compact) 14.dp else 18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(summary.totalGames.toString() + " games", fontWeight = FontWeight.Bold)
                    Text(summary.installedGames.toString() + " installed")
                    Text(summary.favorites.toString() + " favorites")
                }
                Text(
                    summary.totalHoursPlayed.toString() + "h " +
                        summary.remainingMinutes.toString().padStart(2, '0') + "m total playtime",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontSize = 12.sp
                )
                summary.resumeGame?.let { resume ->
                    Text(
                        "Ready to resume: " + resume.title,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        GameFilterBar(
            games, query, { query = it }, platform, { platform = it },
            genre, { genre = it }, favoritesOnly, { favoritesOnly = it }
        )
        Spacer(Modifier.height(14.dp))
        if (filtered.isEmpty()) {
            Text("No games match these filters")
        } else if (compact) {
            HomeGameSection("Installed games", filtered, focusTarget, onFocused, compact, open)
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
            HomeGameSection("Installed games", filtered, focusTarget, onFocused, compact, open)
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
private fun BlueprintLibraryScreen(
    games: List<Game>,
    filtered: List<Game>,
    query: String,
    onQuery: (String) -> Unit,
    platform: String?,
    onPlatform: (String?) -> Unit,
    favoritesOnly: Boolean,
    onFavoritesOnly: (Boolean) -> Unit,
    focusTarget: GameId?,
    onFocused: (GameId) -> Unit,
    open: (Game) -> Unit,
) {
    val summary = summarizeLibrary(games)
    val platforms = games.groupingBy { it.platform }.eachCount().toList().sortedBy { it.first }
    val context = LocalContext.current
    val totalBytes = context.filesDir.totalSpace
    val freeBytes = context.filesDir.usableSpace
    val saveGames = games.filter { it.savePresent }
    var alphabetical by remember { mutableStateOf(false) }
    val sortedGames = if (alphabetical) filtered.sortedBy { it.title.lowercase() }
        else filtered.sortedWith(compareByDescending<Game> { it.lastPlayed }.thenBy { it.title.lowercase() })
    val history = filtered.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val railWidth = (maxWidth * 0.15f).coerceIn(124.dp, 166.dp)
    val sidebarWidth = (maxWidth * 0.19f).coerceIn(155.dp, 200.dp)
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BlueprintRail(Modifier.width(railWidth)) {
            Text("LIBRARY", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            BlueprintRailItem("All", games.size, platform == null && !favoritesOnly, Icons.Rounded.GridView) {
                onPlatform(null); onFavoritesOnly(false)
            }
            platforms.forEach { (name, count) ->
                BlueprintRailItem(
                    name,
                    count,
                    platform == name,
                    Icons.Rounded.SportsEsports,
                    brandKey = name,
                ) {
                    onPlatform(name); onFavoritesOnly(false)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            BlueprintRailItem("Favorites", summary.favorites, favoritesOnly, Icons.Rounded.Favorite) {
                onFavoritesOnly(!favoritesOnly)
            }
            Spacer(Modifier.weight(1f))
            Text("Sort by", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            TextButton(onClick = { alphabetical = !alphabetical }, modifier = Modifier.fillMaxWidth()) {
                Text(if (alphabetical) "Title A–Z  ↓" else "Most Recent  ↓", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("Available offline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }

        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your Library", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(17.dp)) },
                placeholder = { Text("Search games", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.widthIn(max = 230.dp).weight(1f),
                shape = RoundedCornerShape(8.dp),
            )
            }
            if (filtered.isEmpty()) {
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text("No games match these filters", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    title = "Installed Games",
                    games = sortedGames,
                    focusTarget = focusTarget.takeIf { history.none { game -> game.id == it } },
                    onFocused = onFocused,
                    open = open,
                )
                if (history.any { it.savePresent }) {
                BlueprintLibrarySection(
                    title = "Ready to Resume",
                    games = history.filter { it.savePresent },
                    focusTarget = null,
                    onFocused = onFocused,
                    open = open,
                    landscape = true,
                )
                }
            }
        }

        Column(
            Modifier.width(sidebarWidth).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BlueprintPanel(Modifier.fillMaxWidth()) {
                Text("Storage", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Text("${formatBytes(freeBytes)} free", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("of ${formatBytes(totalBytes)} device storage", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                LinearProgressIndicator(
                    progress = { if (totalBytes <= 0L) 0f else ((totalBytes - freeBytes).toDouble() / totalBytes).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                )
            }
            BlueprintPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Local Saves", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(Icons.Rounded.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                }
                Text("${saveGames.size} games with saves", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text(if (saveGames.isEmpty()) "Save information appears after it is detected." else "${formatBytes(saveGames.sumOf { it.saveSizeBytes })} of detected save data", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Text("Open a game to manage its backups.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
            BlueprintPanel(Modifier.fillMaxWidth()) {
                Text("Library Overview", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                HomeStatusItem(Icons.Rounded.VideoLibrary, "Games", summary.totalGames.toString())
                HomeStatusItem(Icons.Rounded.Favorite, "Favorites", summary.favorites.toString())
                HomeStatusItem(Icons.Rounded.Schedule, "Play time", "${summary.totalHoursPlayed}h")
            }
        }
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
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("${games.size} games", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val tileWidth = ((maxWidth - 32.dp) / if (landscape) 3f else 5f).coerceIn(108.dp, if (landscape) 220.dp else 148.dp)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(games, key = { it.id.value }) { game ->
            GameCard(
                game = game,
                modifier = Modifier.width(tileWidth).height(if (landscape) 98.dp else tileWidth * 1.25f),
                poster = !landscape,
                restoreFocus = focusTarget == game.id,
                onFocused = onFocused,
            ) { open(game) }
        }
    }
    }
}

@Composable
private fun GameAvailabilityBadge(game: Game, modifier: Modifier = Modifier) {
    val availableFromSource = game.state == InstallState.NOT_INSTALLED && !game.sourceUrl.isNullOrBlank()
    val (label, icon) = when {
        availableFromSource -> "Available" to Icons.Rounded.CloudDownload
        game.state == InstallState.NOT_INSTALLED -> "Import copy" to Icons.Rounded.FolderOpen
        game.state == InstallState.INSTALLED -> "Installed" to Icons.Rounded.CheckCircle
        game.state == InstallState.UPDATE_AVAILABLE -> "Update" to Icons.Rounded.SystemUpdate
        game.state == InstallState.QUEUED -> "Queued" to Icons.Rounded.Schedule
        game.state == InstallState.DOWNLOADING -> "Downloading" to Icons.Rounded.Downloading
        game.state == InstallState.PAUSED -> "Paused" to Icons.Rounded.PauseCircle
        game.state == InstallState.VERIFYING -> "Verifying" to Icons.Rounded.Verified
        game.state == InstallState.INSTALLING -> "Installing" to Icons.Rounded.InstallMobile
        game.state == InstallState.MISSING_FILES -> "Missing files" to Icons.Rounded.FolderOff
        else -> "Retry" to Icons.Rounded.ErrorOutline
    }
    val accent = when {
        game.state == InstallState.INSTALLED -> Color(0xFF58D68D)
        game.state in setOf(InstallState.FAILED, InstallState.MISSING_FILES) -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = modifier.semantics { contentDescription = "Availability: $label" },
        shape = RoundedCornerShape(5.dp),
        color = Color(0xDC07101A),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.82f)),
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(11.dp))
            Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BlueprintGameTile(
    game: Game,
    restoreFocus: Boolean,
    onFocused: (GameId) -> Unit,
    open: (Game) -> Unit,
) {
    GameCard(
        game = game,
        modifier = Modifier.width(124.dp).height(168.dp),
        poster = true,
        restoreFocus = restoreFocus,
        onFocused = onFocused,
    ) { open(game) }
}

@Composable
private fun BlueprintRail(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun BlueprintRailItem(
    label: String,
    count: Int,
    selected: Boolean,
    icon: ImageVector,
    brandKey: String? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Surface(
        modifier = Modifier.fillMaxWidth().hoverable(interactionSource).blueprintClick(onClick)
            .semantics { this.selected = selected },
        color = if (selected || hovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = if (selected || hovered) BorderStroke(1.dp, blueprintFocusBrush()) else null,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (brandKey == null) {
                Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
            } else {
                ConsoleBrandMark(brandKey, selected, Modifier.size(16.dp))
            }
            Text(label, modifier = Modifier.weight(1f).padding(start = 8.dp), fontSize = 11.sp, maxLines = 1)
            Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

@Composable
private fun BlueprintPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.68f)),
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
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
    onFavoritesOnly: (Boolean) -> Unit,
    showMetadataFilters: Boolean = false,
    region: String? = null,
    onRegion: (String?) -> Unit = {},
    language: String? = null,
    onLanguage: (String?) -> Unit = {},
) {
    val platforms = games.map { it.platform }.distinct().sorted()
    val genres = games.map { it.genre }.distinct().sorted()
    val regions = games.mapNotNull { it.region }.filter(String::isNotBlank).distinct().sorted()
    val languages = games.mapNotNull { it.language }.filter(String::isNotBlank).distinct().sorted()
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
        if (showMetadataFilters && (regions.isNotEmpty() || languages.isNotEmpty())) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (regions.isNotEmpty()) {
                    FilterChip(selected = region == null, onClick = { onRegion(null) }, label = { Text("All regions") })
                    regions.forEach { value ->
                        FilterChip(selected = region == value, onClick = { onRegion(value) }, label = { Text(value) })
                    }
                }
                if (languages.isNotEmpty()) {
                    FilterChip(selected = language == null, onClick = { onLanguage(null) }, label = { Text("All languages") })
                    languages.forEach { value ->
                        FilterChip(selected = language == value, onClick = { onLanguage(value) }, label = { Text(value) })
                    }
                }
            }
        }
    }
}

internal fun matchesStoreMetadataFilters(
    genre: String?,
    region: String?,
    language: String?,
    genreFilter: String?,
    regionFilter: String?,
    languageFilter: String?,
): Boolean =
    (genreFilter == null || genre.equals(genreFilter, ignoreCase = true)) &&
        (regionFilter == null || region.equals(regionFilter, ignoreCase = true)) &&
        (languageFilter == null || language.equals(languageFilter, ignoreCase = true))

internal fun filterGames(
    games: List<Game>,
    query: String,
    platform: String?,
    genre: String?,
    favoritesOnly: Boolean,
    region: String? = null,
    language: String? = null,
): List<Game> {
    val normalized = query.trim()
    return games.filter { game ->
        (normalized.isEmpty() ||
            game.title.contains(normalized, ignoreCase = true) ||
            game.platform.contains(normalized, ignoreCase = true) ||
            game.genre.contains(normalized, ignoreCase = true)) &&
            (platform == null || game.platform == platform) &&
            matchesStoreMetadataFilters(
                game.genre, game.region, game.language,
                genre, region, language,
            ) &&
            (!favoritesOnly || game.favorite)
    }
}

@Composable
private fun DiscoveryDetailsScreen(
    game: DiscoveryGame,
    platformName: String,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    importer: AuthorizedRomImporter,
    repository: GameRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val legalSources = remember(game.title, game.platformId) {
        legalSourceLinks(game.title, game.platformId)
    }
    val importPlatformLabel = remember(platformName) {
        RomImportPolicy.profileLabel(platformName)
    }
    val importFormats = remember(platformName) {
        RomImportPolicy.supportedExtensionsLabel(platformName)
    }
    var importing by remember(game.id) { mutableStateOf(false) }
    var importMessage by remember(game.id) { mutableStateOf<String?>(null) }
    fun queryDisplayName(uri: Uri): String = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull() ?: uri.lastPathSegment ?: "game.rom"
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            importMessage = "No file selected"
        } else {
            val displayName = queryDisplayName(uri)
            importing = true
            importMessage = "Importing and verifying " + displayName + "…"
            scope.launch {
                importMessage = when (val result = importer.import(game.id, uri, displayName, platformName)) {
                    is RomImportResult.Imported -> runCatching {
                        repository.registerImportedGame(
                            ImportedGameRegistration(
                                id = game.id,
                                title = game.title,
                                platform = importPlatformLabel,
                                year = game.releaseDate?.let { releaseDate ->
                                    Regex("""(?:19|20)\d{2}""").find(releaseDate)?.value?.toIntOrNull()
                                } ?: 0,
                                sizeBytes = result.hashes.sizeBytes,
                                relativePath = RomImportPolicy.importRootRelativePath(game.id, result.relativePath),
                                sha256 = result.hashes.sha256,
                                mimeType = RomImportPolicy.mimeType(displayName),
                                favorite = game.favorite,
                                artworkUrl = game.coverUrl,
                                description = game.description,
                                players = game.players,
                            )
                        )
                        "$importPlatformLabel copy verified and added to Library. SHA-256 " +
                            result.hashes.sha256.take(12) + "…"
                    }.getOrElse { error ->
                        "The copy was stored, but Library registration failed: " +
                            (error.message?.take(160) ?: "unknown error")
                    }
                    RomImportResult.SourceUnavailable ->
                        "The selected file could not be opened"
                    is RomImportResult.Rejected ->
                        "Import rejected: " + result.reason
                    is RomImportResult.Failed ->
                        "Import failed: " + result.reason
                }
                importing = false
            }
        }
    }
    val importSetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) {
            importMessage = "No files selected"
        } else {
            val sources = uris.map { uri -> RomImportSource(uri, queryDisplayName(uri)) }
            importing = true
            importMessage = "Importing and verifying ${sources.size} disc-set files…"
            scope.launch {
                importMessage = when (val result = importer.importSet(game.id, sources, platformName)) {
                    is RomImportSetResult.Imported -> runCatching {
                        val importedFiles = result.files.map { file ->
                            LocalContentFile(
                                relativePath = RomImportPolicy.importRootRelativePath(game.id, file.relativePath),
                                sha256 = file.hashes.sha256,
                                mimeType = file.mimeType,
                            )
                        }
                        val launchPath = RomImportPolicy.importRootRelativePath(
                            game.id,
                            result.launchFile.relativePath,
                        )
                        val launchFile = requireNotNull(importedFiles.firstOrNull {
                            it.relativePath == launchPath
                        })
                        repository.registerImportedGame(
                            ImportedGameRegistration(
                                id = game.id,
                                title = game.title,
                                platform = importPlatformLabel,
                                year = game.releaseDate?.let { releaseDate ->
                                    Regex("""(?:19|20)\d{2}""").find(releaseDate)?.value?.toIntOrNull()
                                } ?: 0,
                                sizeBytes = result.files.sumOf { it.hashes.sizeBytes },
                                relativePath = launchFile.relativePath,
                                sha256 = launchFile.sha256,
                                mimeType = launchFile.mimeType,
                                favorite = game.favorite,
                                artworkUrl = game.coverUrl,
                                description = game.description,
                                players = game.players,
                                additionalFiles = importedFiles.filterNot { it.relativePath == launchPath },
                            )
                        )
                        "${result.files.size}-file disc set verified and added to Library"
                    }.getOrElse { error ->
                        "The disc set was stored, but Library registration failed: " +
                            (error.message?.take(160) ?: "unknown error")
                    }
                    RomImportSetResult.SourceUnavailable -> "One of the selected files could not be opened"
                    is RomImportSetResult.Rejected -> "Import rejected: " + result.reason
                    is RomImportSetResult.Failed -> "Import failed: " + result.reason
                }
                importing = false
            }
        }
    }

    var selectedScreenshot by remember(game.id) { mutableStateOf<String?>(null) }
    selectedScreenshot?.let { screenshot ->
        AlertDialog(
            onDismissRequest = { selectedScreenshot = null },
            title = { Text(game.title, maxLines = 2) },
            text = { RemoteArtwork(screenshot, Modifier.fillMaxWidth().height(300.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit) },
            confirmButton = { TextButton(onClick = { selectedScreenshot = null }) { Text("Close") } },
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val narrow = maxWidth < 700.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(Modifier.fillMaxWidth().height(if (narrow) 320.dp else 300.dp),
                shape = RoundedCornerShape(9.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Box(Modifier.fillMaxSize()) {
                    RemoteArtwork(game.backgroundUrl ?: game.screenshots.firstOrNull() ?: game.coverUrl, Modifier.fillMaxSize(), fallbackKey = game.title)
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF5030810), Color(0xA5030810), Color(0x30030810)))))
                    Column(Modifier.fillMaxSize().padding(if (narrow) 18.dp else 24.dp), verticalArrangement = Arrangement.Bottom) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConsoleBrandMark(platformName, true, Modifier.size(23.dp))
                            Text(platformName.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(game.title, fontSize = if (narrow) 28.sp else 38.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(if (narrow) 1f else .75f))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            game.releaseDate?.let { DetailMetric(Icons.Rounded.CalendarMonth, it.take(4)) }
                            game.players?.let { DetailMetric(Icons.Rounded.Groups, "$it players") }
                            game.rating?.let { DetailMetric(Icons.Rounded.Star, it.toString()) }
                        }
                        Text(game.description ?: "Discover this title and import your own copy to play.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 540.dp).padding(top = 9.dp))
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(enabled = !importing, onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                                Icon(Icons.Rounded.FileOpen, null, Modifier.size(16.dp))
                                Text(if (importing) "Importing…" else "Import your copy", Modifier.padding(start = 6.dp), fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = onFavorite) {
                                Icon(if (game.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, Modifier.size(16.dp))
                                Text(if (game.favorite) "Favorited" else "Favorite", Modifier.padding(start = 6.dp), fontSize = 12.sp)
                            }
                            TextButton(onClick = onBack) { Text("Back", fontSize = 12.sp) }
                        }
                    }
                }
            }
            importMessage?.let { message ->
                Text(message, color = if (message.startsWith("Import failed") || message.startsWith("Import rejected")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { contentDescription = message; liveRegion = LiveRegionMode.Polite })
            }
            if (game.screenshots.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Screenshots", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Select to enlarge · ${game.screenshots.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(3.dp)) {
                    items(game.screenshots.distinct(), key = { it }) { screenshot ->
                        Surface(Modifier.width(if (narrow) 236.dp else 260.dp).height(146.dp)
                            .blueprintClick { selectedScreenshot = screenshot }.semantics { contentDescription = "Enlarge screenshot of ${game.title}" },
                            shape = RoundedCornerShape(7.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            RemoteArtwork(screenshot, Modifier.fillMaxSize(), fallbackKey = game.title)
                        }
                    }
                }
            }
            BlueprintPanel(Modifier.fillMaxWidth()) {
                Text("Bring your game", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Accepted for $importPlatformLabel: $importFormats", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                Text("TheGamesDB supplies metadata and artwork, not game files. Import copies and verifies your selected file into Library. You still need a compatible emulator and any required firmware or keys.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                if (RomImportPolicy.supportsMultiFile(platformName)) {
                    OutlinedButton(enabled = !importing, onClick = { importSetLauncher.launch(arrayOf("*/*")) }) { Text("Import multi-file disc set", fontSize = 11.sp) }
                }
            }
            if (legalSources.isNotEmpty()) {
                Text("Find a legal copy", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    legalSources.forEach { source ->
                        OutlinedButton(onClick = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) }
                            catch (_: ActivityNotFoundException) { importMessage = "No browser is available to open ${source.label}" }
                        }) {
                            Icon(Icons.Rounded.OpenInNew, null, Modifier.size(15.dp))
                            Text(source.label, Modifier.padding(start = 7.dp), fontSize = 11.sp)
                        }
                    }
                }
            }
            game.description?.takeIf { it.isNotBlank() }?.let {
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text("About this game", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DiscoveryGameRow(
    games: List<DiscoveryGame>,
    compact: Boolean,
    open: (DiscoveryGame) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(games, key = { it.id.value }) { game ->
            DiscoveryGameCard(
                game = game,
                modifier = Modifier
                    .width(if (compact) 190.dp else 230.dp)
                    .height(if (compact) 190.dp else 220.dp),
                onClick = { open(game) },
            )
        }
    }
}

@Composable
private fun DiscoveryGameCard(game: DiscoveryGame, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else if (focused || hovered) 1.018f else 1f, label = "discovery-scale")
    Surface(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }.hoverable(source)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .semantics { contentDescription = game.title + ", " + game.platformId + ", discover only, import an authorized copy to play"; role = Role.Button },
        shape = RoundedCornerShape(7.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (focused || hovered) 2.dp else 1.dp,
            if (focused || hovered) blueprintFocusBrush() else Brush.linearGradient(listOf(Color(0xFF283443), Color(0xFF17212C)))),
    ) {
        Box(Modifier.fillMaxSize()) {
            RemoteArtwork(game.coverUrl, Modifier.fillMaxSize(), fallbackKey = game.title)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, .48f to Color.Transparent, 1f to Color(0xFF040810))))
            if (game.favorite) Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(9.dp)) {
                Text(game.title, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Discover · ${game.releaseDate?.take(4) ?: "Import your copy"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
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
                Modifier.width(if (compact) 184.dp else 198.dp).height(if (compact) 126.dp else 106.dp),
                restoreFocus = restoreGameId == game.id,
                onFocused = onFocused,
                onClick = { open(game) }
            )
        }
    }
}

@Composable
internal fun GameCard(
    game: Game,
    modifier: Modifier,
    hero: Boolean = false,
    restoreFocus: Boolean = false,
    onFocused: (GameId) -> Unit = {},
    poster: Boolean = false,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val emphasized = focused || hovered
    val scale by animateFloatAsState(if (pressed) .975f else if (emphasized) 1.018f else 1f, label = "game-scale")
    LaunchedEffect(restoreFocus) { if (restoreFocus) focusRequester.requestFocus() }
    Surface(
        modifier.focusRequester(focusRequester)
            .semantics { contentDescription = GameBoxSemantics.gameCardDescription(game, hero); role = Role.Button }
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused(game.id) }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(source)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (emphasized) 2.dp else 1.dp,
            if (emphasized) blueprintFocusBrush() else Brush.linearGradient(listOf(Color(0xFF283443), Color(0xFF17212C)))),
        shadowElevation = if (emphasized) 8.dp else 0.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cardWidth = maxWidth
            RemoteArtwork(game.artworkUrl, Modifier.fillMaxSize(), fallbackKey = game.title)
            Box(Modifier.fillMaxSize().background(
                if (hero) Brush.horizontalGradient(listOf(Color(0xF0060B12), Color(0xB0060B12), Color.Transparent))
                else Brush.verticalGradient(0f to Color.Transparent, .48f to Color.Transparent, 1f to Color(0xFA040911))
            ))
            if (hero) {
                Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Bottom) {
                    Text(game.platform.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(game.title, fontSize = if (cardWidth < 450.dp) 28.sp else 38.sp,
                        lineHeight = if (cardWidth < 450.dp) 32.sp else 44.sp,
                        fontWeight = FontWeight.Bold, maxLines = if (cardWidth < 450.dp) 2 else 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(.84f))
                    Text(game.description ?: game.genre, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 380.dp).padding(top = 6.dp))
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(6.dp)) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(15.dp))
                                Text("View game", Modifier.padding(start = 5.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(game.state.displayName(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }
            } else {
                GameAvailabilityBadge(game, Modifier.align(Alignment.TopStart).padding(7.dp))
                if (game.favorite) Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp))
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(if (poster) 9.dp else 11.dp)) {
                    Text(game.title, fontSize = if (poster || cardWidth < 170.dp) 12.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 16.sp, overflow = TextOverflow.Ellipsis)
                    Text(if (poster) game.platform + " · " + game.state.displayName() else game.state.displayName(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
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
    managedSaveDiscovery: com.gamebox.os.storage.ManagedSaveDiscovery,
    importer: AuthorizedRomImporter,
    saveControllerFactory: ((GameId, kotlinx.coroutines.CoroutineScope) -> SaveSafetyController)?,
    compact: Boolean,
    onDownloads: () -> Unit,
    onBack: () -> Unit
) {
    val isAuthorizedFixture = game.id.value == "galaxy-patrol"
    val parentSaveScope = rememberCoroutineScope()
    val saveScope = remember(game.id, saveControllerFactory) {
        kotlinx.coroutines.CoroutineScope(parentSaveScope.coroutineContext +
            kotlinx.coroutines.SupervisorJob(parentSaveScope.coroutineContext[kotlinx.coroutines.Job]))
    }
    DisposableEffect(saveScope) {
        onDispose { saveScope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
    }
    val gameSaveController = remember(game.id, saveControllerFactory) {
        if (game.id.value == "galaxy-patrol") null else saveControllerFactory?.invoke(game.id, saveScope)
    }
    val discoveredSaves by managedSaveDiscovery.state.collectAsState()
    val saveSummary = discoveredSaves[game.id.value]
    val saveDescription = when (saveSummary?.presence) {
        com.gamebox.os.storage.SavePresence.PRESENT ->
            "${saveSummary.artifactCount} managed save files · ${formatDownloadBytes(saveSummary.totalBytes)}"
        com.gamebox.os.storage.SavePresence.NONE -> "No managed save files found"
        com.gamebox.os.storage.SavePresence.ERROR -> "Managed saves could not be inspected"
        null -> "Inspecting managed saves…"
    }
    val authorizedState by authorizedDownloadController.observeState().collectAsState()
    val launchState by gameLaunchController.observeState().collectAsState()
    val saveSafetyState by saveSafetyController.observeState().collectAsState()
    var showUninstallConfirmation by remember(game.id) { mutableStateOf(false) }
    var showGameSettings by remember(game.id) { mutableStateOf(false) }
    val workerActive = authorizedState.status == AuthorizedDownloadState.Status.QUEUED ||
        authorizedState.status == AuthorizedDownloadState.Status.RUNNING
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let(saveSafetyController::exportBackup) }
    val initialSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(saveSafetyController::importInitialSave) }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(saveSafetyController::importBackup) }
    if (showUninstallConfirmation) {
        ContentRemovalDialog(game, saveSafetyController) { showUninstallConfirmation = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ImportedGameReimportCard(game, importer, repository,
            enabled = launchState.status !in setOf(LaunchUiState.Status.PREPARING,
                LaunchUiState.Status.LAUNCHED, LaunchUiState.Status.SESSION_ERROR))
        gameSaveController?.let { controller ->
            PerGameSaveCard(game, controller, enabled = launchState.status !in setOf(
                LaunchUiState.Status.PREPARING, LaunchUiState.Status.LAUNCHED, LaunchUiState.Status.SESSION_ERROR))
        }
        Surface(
            modifier = Modifier.fillMaxWidth().height(if (compact) 252.dp else 292.dp),
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 8.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                RemoteArtwork(game.artworkUrl, Modifier.fillMaxSize(), fallbackKey = game.title)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(Color(0xF4050812), Color(0xA1050812), Color(0x22050812))
                        )
                    )
                )
                Column(
                    Modifier.fillMaxHeight().fillMaxWidth(if (compact) 0.92f else 0.64f)
                        .padding(if (compact) 18.dp else 26.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(game.platform.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        game.title,
                        fontSize = if (compact) 29.sp else 39.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                    )
                    Text(game.genre, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailMetric(Icons.Rounded.CalendarMonth, game.year.toString())
                        DetailMetric(Icons.Rounded.Groups, game.players ?: "1 player")
                        DetailMetric(Icons.Rounded.Storage, game.sizeMb.toString() + " MB")
                    }
                }
                if (!compact) {
                    Surface(
                        modifier = Modifier.align(Alignment.CenterEnd).width(238.dp).padding(end = 18.dp),
                        color = Color(0xD9151C2C),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                    ) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("INSTALLATION", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Status", fontSize = 10.sp)
                                Text(game.state.displayName(), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Game size", fontSize = 10.sp)
                                Text(game.sizeMb.toString() + " MB", fontSize = 10.sp)
                            }
                            if (game.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE)) {
                                Button(
                                    onClick = { gameLaunchController.launch(game) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Play Now", modifier = Modifier.padding(start = 6.dp), fontSize = 10.sp)
                                }
                            } else {
                                Text("Install this authorized title below to make it ready to play.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showGameSettings = !showGameSettings }) {
            Icon(Icons.Rounded.Tune, null, Modifier.size(16.dp))
            Text(if (showGameSettings) "Hide game settings" else "Game settings & emulator", Modifier.padding(start = 7.dp), fontSize = 12.sp)
        }
        AnimatedVisibility(visible = showGameSettings) {
            Column {
                GameSettingsPanel(game = game, repository = repository)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(9.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Game content · " + game.state.displayName(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (game.platform.equals("Retro", ignoreCase = true) && game.id.value == "galaxy-patrol") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Required core: Nintendo - NES / Famicom (FCEUmm)",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics {
                            contentDescription = "Required emulator core: Nintendo NES Famicom FCEUmm"
                        }
                    )
                }
                if (game.state !in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "How to play: install this authorized game, install the approved emulator shown in Game settings, then press Play after verification.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        modifier = Modifier.semantics {
                            contentDescription = "How to play: install the game, install the approved emulator, then press Play after verification"
                        }
                    )
                }
                if (isAuthorizedFixture && authorizedState.status != AuthorizedDownloadState.Status.IDLE) {
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            if (isAuthorizedFixture) {
                                authorizedDownloadController.install()
                            } else {
                                when (game.state) {
                                    InstallState.NOT_INSTALLED, InstallState.FAILED, InstallState.MISSING_FILES ->
                                        if (game.sourceUrl != null && game.expectedSha256 != null) {
                                            remoteDownloadController.install(game)
                                        } else {
                                            repository.setInstallState(game.id, InstallState.FAILED)
                                        }
                                    InstallState.PAUSED ->
                                        if (game.sourceUrl != null && game.expectedSha256 != null) remoteDownloadController.resume(game)
                                        else onDownloads()
                                    InstallState.QUEUED, InstallState.DOWNLOADING, InstallState.VERIFYING,
                                    InstallState.INSTALLING -> onDownloads()
                                    InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE ->
                                        gameLaunchController.launch(game)
                                }
                            }
                        },
                        enabled = when {
                            isAuthorizedFixture -> !workerActive
                            game.state in setOf(
                                InstallState.NOT_INSTALLED,
                                InstallState.FAILED,
                                InstallState.MISSING_FILES
                            ) -> game.sourceUrl != null && game.expectedSha256 != null
                            else -> true
                        }
                    ) {
                        Text(
                            if (!isAuthorizedFixture) game.state.primaryAction()
                            else if (authorizedState.status == AuthorizedDownloadState.Status.SUCCEEDED)
                                "Reinstall game"
                            else "Install game"
                        )
                    }
                    if (isAuthorizedFixture && workerActive) {
                        OutlinedButton(onClick = authorizedDownloadController::cancel) {
                            Text("Cancel")
                        }
                    }
                    if (isAuthorizedFixture && (game.state == InstallState.INSTALLED ||
                        game.state == InstallState.UPDATE_AVAILABLE)
                    ) {
                        Button(onClick = { gameLaunchController.launch(game) }) {
                            Text("Play")
                        }
                        OutlinedButton(onClick = { showUninstallConfirmation = true },
                            enabled = launchState.status !in setOf(LaunchUiState.Status.PREPARING,
                                LaunchUiState.Status.LAUNCHED, LaunchUiState.Status.SESSION_ERROR)) {
                            Text("Uninstall content")
                        }
                    }
                    if (isAuthorizedFixture && !saveSafetyState.saveRecordPresent) {
                        OutlinedButton(onClick = { initialSaveLauncher.launch(arrayOf("*/*")) }) {
                            Text("Import save file")
                        }
                    }
                    if (!isAuthorizedFixture && game.state in setOf(InstallState.INSTALLED,
                        InstallState.UPDATE_AVAILABLE, InstallState.MISSING_FILES)) {
                        OutlinedButton(onClick = { showUninstallConfirmation = true },
                            enabled = launchState.status !in setOf(LaunchUiState.Status.PREPARING,
                                LaunchUiState.Status.LAUNCHED, LaunchUiState.Status.SESSION_ERROR)) {
                            Text("Uninstall content")
                        }
                    }
                    if (!isAuthorizedFixture && game.state == InstallState.DOWNLOADING &&
                        game.sourceUrl != null && game.expectedSha256 != null
                    ) {
                        OutlinedButton(onClick = { remoteDownloadController.pause(game) }) {
                            Text("Pause")
                        }
                    }
                    OutlinedButton(
                        onClick = { repository.setFavorite(game.id, !game.favorite) }
                    ) { Text(if (game.favorite) "Remove favorite" else "Favorite") }
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = saveDescription
                    }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Managed save data", fontWeight = FontWeight.Bold)
                        Text(
                            saveDescription,
                            color = if (game.savePresent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                        Text("Emulator-owned saves require a configured connection; they are not included in this scan.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
                        OutlinedButton(onClick = managedSaveDiscovery::refresh) { Text("Refresh saves") }
                    }
                }
                if (isAuthorizedFixture && saveSafetyState.saveRecordPresent) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Managed save path: " + saveSafetyState.relativePath,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = saveSafetyController::backupSave) {
                            Text(if (saveSafetyState.backupPresent) "Update backup" else "Back up save")
                        }
                        if (saveSafetyState.backupPresent) {
                            OutlinedButton(onClick = saveSafetyController::restoreSave) {
                                Text("Restore backup")
                            }
                            OutlinedButton(onClick = {
                                exportBackupLauncher.launch("gamebox-galaxy-patrol-save.dat")
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
                if (isAuthorizedFixture) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            enabled = saveSafetyState.saveRecordPresent,
                            onClick = saveSafetyController::uploadCloudSave,
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Upload cloud copy", modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(onClick = saveSafetyController::downloadCloudSave) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Restore cloud copy", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    Text(
                        "Cloud credentials and endpoint are configured in Settings. A conflicting local copy is preserved before restore.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (isAuthorizedFixture) {
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
                    if (launchState.status == LaunchUiState.Status.PREPARING) {
                        OutlinedButton(onClick = gameLaunchController::cancelPreparation) {
                            Text("Cancel preparation")
                        }
                    }
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
        if (game.description != null || game.players != null || game.language != null || game.region != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About this game", fontWeight = FontWeight.Bold)
                    game.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)) }
                    val metadata = listOfNotNull(
                        game.players?.let { "Players: $it" },
                        game.language?.let { "Language: $it" },
                        game.region?.let { "Region: $it" }
                    )
                    if (metadata.isNotEmpty()) {
                        Text(metadata.joinToString("  •  "), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
internal fun DownloadProgressIndicator(job: com.gamebox.os.domain.DownloadJob, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { job.progress },
        modifier = modifier.semantics {
            contentDescription = "Download progress for " + job.title
            stateDescription = GameBoxSemantics.downloadProgressDescription(job)
            progressBarRangeInfo = ProgressBarRangeInfo(job.progress, 0f..1f)
        }
    )
}

@Composable
private fun DownloadsScreen(repository: GameRepository, downloadRepository: DownloadRepository, remoteDownloadController: RemoteDownloadController, compact: Boolean) {
    val jobs by downloadRepository.observeJobs().collectAsState()
    val context = LocalContext.current
    val telemetryTracker = remember { DownloadTelemetryTracker() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(if (compact) "Downloads" else "Download Manager", fontSize = if (compact) 28.sp else 17.sp, fontWeight = FontWeight.Bold)
        Text("Manage active transfers, completed games, and verified installation",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Spacer(Modifier.height(14.dp))
        if (!compact) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DownloadMetricCard(
                    Icons.Rounded.Downloading,
                    jobs.count { it.status == DownloadStatus.DOWNLOADING }.toString(),
                    "Active downloads",
                    Modifier.weight(1f),
                )
                DownloadMetricCard(
                    Icons.Rounded.Schedule,
                    jobs.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.PAUSED }.toString(),
                    "Queued or paused",
                    Modifier.weight(1f),
                )
                DownloadMetricCard(
                    Icons.Rounded.Verified,
                    jobs.count { it.status == DownloadStatus.COMPLETED }.toString(),
                    "Completed",
                    Modifier.weight(1f),
                )
                DownloadMetricCard(
                    Icons.Rounded.Storage,
                    formatBytes(context.filesDir.usableSpace),
                    "Free space",
                    Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        val downloadListContent: @Composable ColumnScope.() -> Unit = {
        if (jobs.isEmpty()) {
            BlueprintPanel(Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Downloading, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("No active downloads", fontWeight = FontWeight.Bold)
                Text("Games you queue from authorized sources will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        jobs.forEach { job ->
            val telemetry = telemetryTracker.sample(job, System.currentTimeMillis())
            val capacityWarning = assessDownloadCapacity(job, context.filesDir.usableSpace)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    .semantics {
                        contentDescription = GameBoxSemantics.downloadDescription(job)
                        if (job.status == DownloadStatus.FAILED) liveRegion = LiveRegionMode.Assertive
                    }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(job.title, fontWeight = FontWeight.Bold)
                        Text(job.status.displayName())
                    }
                    Spacer(Modifier.height(8.dp))
                    DownloadProgressIndicator(job, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatDownloadBytes(job.downloadedBytes) + " of " +
                            formatDownloadBytes(job.totalBytes),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 12.sp
                    )
                    telemetry?.let {
                        Text(
                            formatDownloadTelemetry(it),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "Download speed and time remaining: " +
                                    formatDownloadTelemetry(it)
                            }
                        )
                    }
                    capacityWarning?.let {
                        Text(
                            formatCapacityWarning(it),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.semantics {
                                contentDescription = formatCapacityWarning(it)
                            }
                        )
                    }
                    job.errorReason?.let { reason ->
                        Text(reason, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    val remoteGame = repository.game(job.gameId)?.takeIf { it.sourceUrl != null && it.expectedSha256 != null }
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
                        } else if (job.status !in setOf(
                            DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED
                        )) {
                            Column {
                                Text("This transfer has no available source. Open the game to choose a source or import a copy.",
                                    color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                OutlinedButton(onClick = {
                                    downloadRepository.cancel(job.gameId)
                                    if (repository.game(job.gameId)?.state in setOf(
                                        InstallState.QUEUED, InstallState.DOWNLOADING, InstallState.PAUSED,
                                        InstallState.VERIFYING, InstallState.INSTALLING
                                    )) repository.setInstallState(job.gameId, InstallState.FAILED)
                                }) {
                                    Text("Dismiss unavailable transfer")
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        if (compact) {
            downloadListContent()
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), content = downloadListContent)
                Column(Modifier.width(198.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    BlueprintPanel(Modifier.fillMaxWidth()) {
                        Text("Network", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        HomeStatusItem(Icons.Rounded.Wifi, "Connection", "Automatic")
                        Text("Bandwidth is shared with streaming and catalog sync.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                    BlueprintPanel(Modifier.fillMaxWidth()) {
                        Text("Storage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(formatBytes(context.filesDir.usableSpace) + " free", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        LinearProgressIndicator(
                            progress = {
                                val total = context.filesDir.totalSpace
                                if (total > 0L) 1f - context.filesDir.usableSpace.toFloat() / total.toFloat() else 0f
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    BlueprintPanel(Modifier.fillMaxWidth()) {
                        Text("Recent Completed", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val completed = jobs.filter { it.status == DownloadStatus.COMPLETED }.takeLast(3).asReversed()
                        if (completed.isEmpty()) Text("Nothing completed yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        completed.forEach { job -> HomeStatusItem(Icons.Rounded.CheckCircle, job.title, "Ready") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadMetricCard(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.70f)),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}


internal data class AppShortcut(
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
    AppShortcut("Chrome", "Web browser", "com.android.chrome"),
    AppShortcut("Android desktop", "Return to the DeX or Android home screen", "__android_home__")
)

@Composable
private fun AppHubScreen(
    title: String,
    subtitle: String,
    shortcuts: List<AppShortcut>,
    compact: Boolean,
    settingsRepository: SettingsRepository
) {
    val context = LocalContext.current
    val currentSettings by settingsRepository.settings.collectAsState(
        initial = com.gamebox.os.settings.GameBoxSettings()
    )
    val launchIntents = remember(shortcuts) {
        shortcuts.associate { shortcut ->
            shortcut.packageName to if (shortcut.packageName == "__android_home__") {
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                context.packageManager.getLaunchIntentForPackage(shortcut.packageName)
            }
        }
    }
    val visiblePackages = visibleShortcutPackageNames(
        shortcuts.map { it.packageName },
        launchIntents.filterValues { it != null }.keys,
        currentSettings.showUnavailableShortcuts
    ).toSet()
    val visibleShortcuts = shortcuts.filter { it.packageName in visiblePackages }
    var message by remember { mutableStateOf<String?>(null) }
    var recentMoonlightSessions by remember { mutableStateOf(emptyList<String>()) }
    val moonlightStatus = remember(launchIntents, recentMoonlightSessions) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager?.activeNetwork
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
        MoonlightStatus(
            connectivity = classifyMoonlightConnectivity(
                hasNetwork = capabilities != null,
                hasLocalTransport = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
            ),
            moonlightInstalled = launchIntents["com.limelight"] != null,
            recentSessions = recentMoonlightSessions
        )
    }
    fun launchShortcut(shortcut: AppShortcut) {
        val launchIntent = launchIntents[shortcut.packageName]
        if (launchIntent == null) {
            message = shortcut.title + " is not installed"
        } else {
            try {
                context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                if (shortcut.title == "Moonlight") {
                    recentMoonlightSessions = addRecentMoonlightSession(
                        recentMoonlightSessions,
                        "Moonlight session " + java.time.LocalTime.now().withNano(0)
                    )
                }
                message = "Opened " + shortcut.title
            } catch (_: ActivityNotFoundException) {
                message = "Unable to open " + shortcut.title
            }
        }
    }
    if (!compact) {
        BlueprintAppHubScreen(
            title = title,
            subtitle = subtitle,
            shortcuts = visibleShortcuts,
            installedPackages = launchIntents.filterValues { it != null }.keys,
            moonlightStatus = moonlightStatus,
            message = message,
            onLaunch = ::launchShortcut,
        )
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(title, fontSize = if (compact) 28.sp else 38.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        if (title == "PC Hub") {
            MoonlightStatusPanel(moonlightStatus, compact)
            Spacer(Modifier.height(12.dp))
            MoonlightHostProbePanel(Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
        } else {
            Spacer(Modifier.height(18.dp))
        }
        if (visibleShortcuts.isEmpty()) {
            Text(
                "No installed shortcuts are available. Enable unavailable shortcuts in Settings to see setup guidance.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
        visibleShortcuts.chunked(if (compact) 1 else 3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { shortcut ->
                    val launchIntent = launchIntents[shortcut.packageName]
                    ShortcutCard(
                        shortcut = shortcut,
                        installed = launchIntent != null,
                        modifier = Modifier.weight(1f),
                        onClick = { launchShortcut(shortcut) }
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
private fun BlueprintAppHubScreen(
    title: String, subtitle: String, shortcuts: List<AppShortcut>, installedPackages: Set<String>,
    moonlightStatus: MoonlightStatus, message: String?, onLaunch: (AppShortcut) -> Unit,
) {
    val isPc = title == "PC Hub"
    val context = LocalContext.current
    val audio = context.getSystemService(android.media.AudioManager::class.java)
    val volume = audio?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0
    val volumeMax = audio?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 1
    val networkLabel = when (moonlightStatus.connectivity) {
        MoonlightConnectivity.OFFLINE -> "Offline"
        MoonlightConnectivity.LOCAL_NETWORK -> "Local network available"
        MoonlightConnectivity.INTERNET -> "Internet available"
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isPc) "PC Hub" else "Your Media Hub", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            if (shortcuts.isEmpty()) {
                BlueprintPanel(Modifier.fillMaxWidth()) { Text("No shortcuts available. Enable unavailable shortcuts in Settings for setup guidance.", fontSize = 12.sp) }
            } else if (isPc) {
                val moonlight = shortcuts.firstOrNull { it.title == "Moonlight" }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (moonlight != null) BlueprintPcHero(moonlight, moonlight.packageName in installedPackages,
                        { onLaunch(moonlight) }, Modifier.weight(1.25f).height(272.dp))
                    Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        shortcuts.filterNot { it.title == "Moonlight" }.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { shortcut ->
                                    BlueprintShortcutTile(shortcut, shortcut.packageName in installedPackages,
                                        Modifier.weight(1f).height(131.dp)) { onLaunch(shortcut) }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                Text("Recent sessions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    if (moonlightStatus.recentSessions.isEmpty()) Text("No streaming sessions opened during this visit.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    moonlightStatus.recentSessions.take(3).forEach { Text(it, fontSize = 11.sp) }
                }
                Text("Connection setup", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                MoonlightHostProbePanel(Modifier.fillMaxWidth())
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(3.dp)) {
                    items(shortcuts, key = { it.packageName }) { shortcut ->
                        BlueprintShortcutTile(shortcut, shortcut.packageName in installedPackages,
                            Modifier.width(128.dp).height(128.dp + 48.dp * (androidx.compose.ui.platform.LocalDensity.current.fontScale - 1f).coerceAtLeast(0f))) { onLaunch(shortcut) }
                    }
                }
                Text("Ready on this device", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                val ready = shortcuts.filter { it.packageName in installedPackages }
                if (ready.isEmpty()) {
                    BlueprintPanel(Modifier.fillMaxWidth()) {
                        Text("Install a media app to enable one-click launch.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                } else LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(ready, key = { it.packageName }) { shortcut ->
                        ShortcutCard(shortcut, true, Modifier.width(220.dp)) { onLaunch(shortcut) }
                    }
                }
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text("Pick up where you left off", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Open a media app to access its watch history, playlists and downloads. GameBox does not read your viewing history.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Text("Quick actions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) } }) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(16.dp))
                    Text("Audio", Modifier.padding(start = 6.dp), fontSize = 11.sp)
                }
                OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) } }) {
                    Icon(Icons.Rounded.SportsEsports, null, Modifier.size(16.dp))
                    Text("Controllers", Modifier.padding(start = 6.dp), fontSize = 11.sp)
                }
                shortcuts.firstOrNull { it.title == "Files" }?.let { files ->
                    OutlinedButton(onClick = { onLaunch(files) }) { Text("Open files", fontSize = 11.sp) }
                }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }
        Column(Modifier.width(198.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isPc) {
                HomeStatusItem(Icons.Rounded.DesktopWindows, "PC streaming", if (moonlightStatus.moonlightInstalled) "Moonlight installed" else "Install Moonlight")
                HomeStatusItem(Icons.Rounded.Wifi, "Connection", networkLabel)
                HomeStatusItem(Icons.Rounded.SportsEsports, "Controller", connectedControllerLabel())
                HomeStatusItem(Icons.Rounded.Smartphone, "Android ${Build.VERSION.RELEASE}", Build.MODEL)
            } else {
                HomeStatusItem(Icons.AutoMirrored.Rounded.VolumeUp, "Media volume", "${(volume * 100 / volumeMax.coerceAtLeast(1))}%",
                    volume.toFloat() / volumeMax.coerceAtLeast(1))
                HomeStatusItem(Icons.Rounded.Wifi, "Network", networkLabel)
                HomeStatusItem(Icons.Rounded.SportsEsports, "Controller", connectedControllerLabel())
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text("Remote & controller", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    ControllerHint("A", "Open app", Color(0xFF78D64B))
                    ControllerHint("B", "Back", Color(0xFFFF4D5E))
                    Text("LB / RB  Change tab", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
internal fun BlueprintShortcutTile(
    shortcut: AppShortcut,
    installed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val emphasized = focused || hovered
    val scale by animateFloatAsState(if (pressed) 0.95f else if (emphasized) 1.04f else 1f, label = "hub-tile-scale")
    val accent = shortcutAccent(shortcut.title)
    Surface(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(interactionSource).onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        tonalElevation = if (emphasized) 10.dp else 2.dp,
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.72f), accent.copy(alpha = 0.26f), MaterialTheme.colorScheme.surface)))) {
            Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(0.35f))
                AppBrandMark(shortcut.title, Modifier.size(39.dp))
                Spacer(Modifier.weight(0.35f))
                Text(shortcut.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(if (installed) "Ready" else "Setup required", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun BlueprintPcHero(shortcut: AppShortcut, installed: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.blueprintClick(onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF18365B), Color(0xFF07101F), Color(0xFF040711))))) {
            AppBrandMark("Moonlight", Modifier.align(Alignment.TopEnd).padding(15.dp).size(38.dp))
            Column(Modifier.fillMaxSize().padding(15.dp)) {
                Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(5.dp)) {
                    Text(if (installed) "READY" else "SETUP REQUIRED", modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Text("YOUR PC, ON THE BIG SCREEN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                Text("Moonlight", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(if (installed) "Moonlight is ready" else "Install Moonlight to stream", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onClick) { Text("Launch", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun MoonlightStatusPanel(status: com.gamebox.os.launch.MoonlightStatus, compact: Boolean) {
    val connectivityLabel = when (status.connectivity) {
        MoonlightConnectivity.OFFLINE -> "Offline"
        MoonlightConnectivity.LOCAL_NETWORK -> "LAN ready"
        MoonlightConnectivity.INTERNET -> "Network connected"
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).semantics {
            contentDescription = "PC streaming: " + connectivityLabel
        }
    ) {
        Column(Modifier.padding(if (compact) 14.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PC Streaming", fontWeight = FontWeight.Bold)
                Text(
                    connectivityLabel,
                    color = if (status.connectivity == MoonlightConnectivity.OFFLINE)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Text(
                if (status.moonlightInstalled) {
                    "Moonlight is installed. Launch it to stream from a paired PC."
                } else {
                    "Install Moonlight to enable PC game streaming."
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp
            )
            if (status.recentSessions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Recent sessions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                status.recentSessions.take(3).forEach { session ->
                    Text(session, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
                }
            }
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
    val accent = shortcutAccent(shortcut.title)
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val emphasized = focused || hovered
    val border by animateColorAsState(
        if (emphasized) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "shortcut-focus"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else if (emphasized) 1.02f else 1f,
        label = "shortcut-scale"
    )
    Surface(
        modifier.height(146.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(interactionSource)
            .semantics { contentDescription = shortcut.title + ", " + if (installed) "installed" else "not installed" }
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(if (focused) 3.dp else 1.dp, border),
        tonalElevation = if (emphasized) 10.dp else 3.dp,
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.30f), Color.Transparent, Color.Transparent)
                )
            )
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.92f),
                    ) {
                        Box(Modifier.padding(10.dp).size(25.dp)) {
                            AppBrandMark(shortcut.title, Modifier.fillMaxSize())
                        }
                    }
                    Column {
                        Text(shortcut.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(
                            shortcut.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (installed) "READY" else "SETUP REQUIRED",
                        color = if (installed) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun shortcutAccent(title: String): Color = when (title) {
    "YouTube", "Netflix" -> Color(0xFFE53935)
    "Kodi" -> Color(0xFF168AD7)
    "Jellyfin" -> Color(0xFF6D48D7)
    "Plex" -> Color(0xFFE5A11A)
    "Spotify" -> Color(0xFF1DB954)
    "VLC" -> Color(0xFFF28C28)
    "Twitch" -> Color(0xFF9146FF)
    "Moonlight" -> Color(0xFF6C63FF)
    "Winlator" -> Color(0xFF2475D5)
    "Termux" -> Color(0xFF2E8B57)
    "Files" -> Color(0xFFE6A928)
    "Chrome" -> Color(0xFF4285F4)
    else -> Color(0xFF5C7CFA)
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
    val scrollState = rememberScrollState()
    val sectionOffsets = remember { mutableStateMapOf<SettingsSection, Int>() }
    var selectedSection by remember { mutableStateOf(SettingsSection.STORAGE) }
    val diagnosticEvents = remember { DiagnosticEventCollector() }
    val externalStorageController = remember(context, settingsRepository) {
        ExternalStorageController(context, settingsRepository)
    }
    var catalogUrl by remember(currentSettings.catalogUrl) { mutableStateOf(currentSettings.catalogUrl) }
    var catalogMessage by remember { mutableStateOf<String?>(null) }
    var theGamesDbApiKey by remember { mutableStateOf("") }
    var theGamesDbConfigured by remember { mutableStateOf(false) }
    var cloudProvider by remember(currentSettings.cloudSaveProvider) {
        mutableStateOf(currentSettings.cloudSaveProvider.uppercase())
    }
    var cloudEndpoint by remember(currentSettings.cloudSaveEndpoint) {
        mutableStateOf(currentSettings.cloudSaveEndpoint)
    }
    var cloudRegion by remember(currentSettings.cloudSaveRegion) {
        mutableStateOf(currentSettings.cloudSaveRegion)
    }
    var cloudIdentity by remember { mutableStateOf("") }
    var cloudSecret by remember { mutableStateOf("") }
    var cloudCredentialsConfigured by remember { mutableStateOf(false) }
    var cloudMessage by remember { mutableStateOf<String?>(null) }
    var companionSecret by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(settingsRepository) {
        theGamesDbConfigured = settingsRepository.hasTheGamesDbApiKey()
    }
    LaunchedEffect(settingsRepository, cloudProvider) {
        cloudCredentialsConfigured = settingsRepository.hasCloudSaveCredentials(cloudProvider)
    }
    LaunchedEffect(settingsRepository) { companionSecret = settingsRepository.companionPairingSecret() }
    val externalStorageStatus = externalStorageController.inspect(currentSettings.externalLibraryUri)
    val installedMigration = remember(context) { com.gamebox.os.storage.InstalledContentMigration(context.filesDir.resolve("installed")) }
    val migrationPlan = remember(diagnosticGames) { installedMigration.plan() }
    var showMigrationDialog by remember { mutableStateOf(false) }
    var migrationResult by remember { mutableStateOf<com.gamebox.os.storage.ContentMigrationResult?>(null) }
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
    val diagnosticsBundle = remember(diagnosticsReport, diagnosticEvents.snapshot()) {
        buildDiagnosticsRecoveryBundle(diagnosticsReport, diagnosticEvents.snapshot())
    }
    val diagnosticsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use { it.write(diagnosticsBundle) }
                    ?: error("Unable to open diagnostics destination")
            }
            if (result.isSuccess) {
                diagnosticEvents.record(com.gamebox.os.diagnostics.DiagnosticLevel.INFO, "diagnostics_export", "Recovery bundle exported")
            } else {
                diagnosticEvents.record(com.gamebox.os.diagnostics.DiagnosticLevel.ERROR, "diagnostics_export", result.exceptionOrNull()?.message ?: "Export failed")
            }
            catalogMessage = if (result.isSuccess) "Sanitized recovery bundle exported"
            else "Diagnostics export failed"
        }
    }
    val totalStorage = storageRoot.totalSpace
    val usableStorage = storageRoot.usableSpace
    val connectedControllers = InputDevice.getDeviceIds().count { id ->
        val sources = InputDevice.getDevice(id)?.sources ?: 0
        sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
    }
    val activeDownloads = diagnosticDownloads.count {
        it.status !in setOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)
    }
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val networkCapabilities = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
    val networkReady = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    fun launchSystemSettings(action: String) {
        try {
            val intent = if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                Intent(action, Uri.parse("package:${context.packageName}"))
            } else Intent(action)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun sectionAnchor(section: SettingsSection): Modifier = Modifier.onGloballyPositioned { coordinates ->
        sectionOffsets[section] = (coordinates.positionInParent().y + scrollState.value).roundToInt()
    }

    LaunchedEffect(scrollState.value, sectionOffsets.size) {
        selectedSection = SettingsNavigationPolicy.selectedSection(scrollState.value, sectionOffsets)
    }
    if (showMigrationDialog) {
        MigrationConfirmationDialog(
            plan = migrationPlan,
            storageStatus = externalStorageStatus,
            previousResult = migrationResult,
            onConfirm = {
                showMigrationDialog = false
                scope.launch {
                    val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        installedMigration.execute(context, android.net.Uri.parse(currentSettings.externalLibraryUri), migrationPlan)
                    }
                    migrationResult = result
                    catalogMessage = "Migration copied " + result.copiedCount + " item(s), " + result.retryableCount + " retryable, " + result.failedCount + " failed"
                }
            },
            onRetry = { externalTreeLauncher.launch(null) },
            onDismiss = { showMigrationDialog = false }
        )
    }
    val settingsContent: @Composable ColumnScope.() -> Unit = {
        Text("Settings", fontSize = if (compact) 28.sp else 17.sp, fontWeight = FontWeight.Bold)
        Text(
            "GameBox configuration and safe Android system shortcuts",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            fontSize = if (compact) 14.sp else 11.sp,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("App storage", fontWeight = FontWeight.Bold)
                    Text(
                        formatBytes(usableStorage) + " free of " + formatBytes(totalStorage),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    LinearProgressIndicator(
                        progress = { if (totalStorage > 0L) 1f - usableStorage.toFloat() / totalStorage.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Interface", fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Show unavailable app shortcuts")
                Text(
                    "When disabled, Media and PC Hub show only installed apps.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = currentSettings.showUnavailableShortcuts,
                onCheckedChange = { show ->
                    scope.launch { settingsRepository.setShowUnavailableShortcuts(show) }
                },
                modifier = Modifier.semantics {
                    contentDescription = "Show unavailable app shortcuts"
                }
            )
        }
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("Storage", sectionAnchor(SettingsSection.STORAGE))
        Text("External game library", fontWeight = FontWeight.Bold)
        Text(
            externalStorageStatus.displayName ?: externalStorageStatus.message,
            modifier = Modifier.semantics { contentDescription = "External game library: " + (externalStorageStatus.displayName ?: externalStorageStatus.message) },
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
        Button(
            onClick = { showMigrationDialog = true },
            enabled = !migrationPlan.isEmpty && externalStorageStatus.state == ExternalStorageState.AVAILABLE_READ_WRITE,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { contentDescription = "Migrate installed content" }
        ) {
            Text(if (migrationPlan.isEmpty) "No installed content to migrate" else "Migrate " + formatBytes(migrationPlan.totalBytes))
        }
        Text(
            "Migration copies verified installed content to the selected library. Phone files are retained.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("Controllers", sectionAnchor(SettingsSection.CONTROLLERS))
        SettingsStatusCard(
            Icons.Rounded.SportsEsports,
            if (connectedControllers == 1) "1 game controller connected" else "$connectedControllers game controllers connected",
            "Pair controllers in Android, then return to GameBox. D-pad, A/B and shoulder navigation work throughout the shell.",
        )
        SettingsActionRow("Open Bluetooth controller settings", Icons.Rounded.Bluetooth) {
            launchSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
        }
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("Downloads", sectionAnchor(SettingsSection.DOWNLOADS))
        SettingsStatusCard(
            Icons.Rounded.Downloading,
            if (activeDownloads == 1) "1 active download" else "$activeDownloads active downloads",
            "Queued transfers are durable and resume through WorkManager after an app or device restart.",
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
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("Emulators", sectionAnchor(SettingsSection.EMULATORS))
        SettingsStatusCard(
            Icons.Rounded.Memory,
            "Approved emulator adapters",
            "RetroArch, PPSSPP, Dolphin, DuckStation, M64Plus FZ and Flycast are detected at launch time. Game Details explains missing cores or adapters.",
        )
        SettingsActionRow("Manage installed emulator apps", Icons.Rounded.Apps) {
            launchSystemSettings(Settings.ACTION_APPLICATION_SETTINGS)
        }
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("Display", sectionAnchor(SettingsSection.DISPLAY))
        SettingsStatusCard(Icons.Rounded.Monitor, "Responsive display mode", if (compact) "Phone layout active" else "Wide / DeX layout active")
        SettingsActionRow("Open Android display settings", Icons.Rounded.Monitor) {
            launchSystemSettings(Settings.ACTION_DISPLAY_SETTINGS)
        }
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("Audio", sectionAnchor(SettingsSection.AUDIO))
        SettingsStatusCard(Icons.AutoMirrored.Rounded.VolumeUp, "System audio", "GameBox respects Android media volume and the active output route.")
        SettingsActionRow("Open Android sound settings", Icons.AutoMirrored.Rounded.VolumeUp) {
            launchSystemSettings(Settings.ACTION_SOUND_SETTINGS)
        }
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("Network", sectionAnchor(SettingsSection.NETWORK))
        SettingsStatusCard(
            Icons.Rounded.Wifi,
            if (networkReady) "Internet connection available" else "Offline mode active",
            if (networkReady) "Catalog refresh, metadata, downloads and cloud saves can use the current network." else "Bundled catalog and installed games remain available.",
        )
        SettingsActionRow("Open Android network settings", Icons.Rounded.Wifi) {
            launchSystemSettings(Settings.ACTION_WIRELESS_SETTINGS)
        }
        Text("Windows Companion", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Text("Enable a paired local-network status connection. The pairing secret is encrypted on this device.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (currentSettings.companionEnabled) "Companion active on port ${currentSettings.companionPort}" else "Companion disabled")
            Switch(checked = currentSettings.companionEnabled, onCheckedChange = { enabled ->
                scope.launch {
                    if (enabled && companionSecret == null) companionSecret = settingsRepository.rotateCompanionPairingSecret()
                    settingsRepository.setCompanionConfiguration(enabled, currentSettings.companionPort)
                    if (enabled) com.gamebox.os.companion.CompanionEndpointService.start(context) else com.gamebox.os.companion.CompanionEndpointService.stop(context)
                }
            })
        }
        OutlinedButton(onClick = { scope.launch { companionSecret = settingsRepository.rotateCompanionPairingSecret() } }, modifier = Modifier.padding(top = 6.dp)) { Text("Rotate pairing secret") }
        companionSecret?.let { Text("Pairing secret: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("Saves & Cloud Sync", sectionAnchor(SettingsSection.SAVES_CLOUD))
        Text("Authenticated cloud backup", fontWeight = FontWeight.Bold)
        Text(
            "Optional. GameBox encrypts credentials with Android Keystore and transfers only checksum-protected save envelopes over HTTPS.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            listOf("WEBDAV" to "WebDAV", "S3" to "S3-compatible").forEach { (value, label) ->
                if (cloudProvider == value) {
                    Button(onClick = { cloudProvider = value }) { Text(label) }
                } else {
                    OutlinedButton(onClick = { cloudProvider = value }) { Text(label) }
                }
            }
        }
        OutlinedTextField(
            value = cloudEndpoint,
            onValueChange = { cloudEndpoint = it },
            label = { Text(if (cloudProvider == "S3") "HTTPS bucket/prefix endpoint" else "HTTPS WebDAV collection") },
            supportingText = { Text("GameBox appends a game-scoped .gamebox-save object name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (cloudProvider == "S3") {
            OutlinedTextField(
                value = cloudRegion,
                onValueChange = { cloudRegion = it },
                label = { Text("S3 signing region") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
        OutlinedTextField(
            value = cloudIdentity,
            onValueChange = { cloudIdentity = it },
            label = { Text(if (cloudProvider == "S3") "Access key" else "Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        OutlinedTextField(
            value = cloudSecret,
            onValueChange = { cloudSecret = it },
            label = { Text(if (cloudProvider == "S3") "Secret key" else "Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).semantics {
                contentDescription = "Cloud save secret, hidden"
            },
        )
        Text(
            if (cloudCredentialsConfigured) "Credentials configured securely; leave both fields blank to keep them."
            else "Credentials are not configured for this provider.",
            color = if (cloudCredentialsConfigured) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 5.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = {
                val provider = runCatching { CloudSaveProvider.valueOf(cloudProvider) }.getOrNull()
                val validation = runCatching {
                    requireNotNull(provider) { "Choose a supported cloud provider" }
                    CloudSaveEndpointPolicy.objectUri(cloudEndpoint, "galaxy-patrol")
                    CloudSaveEndpointPolicy.requireRegion(provider, cloudRegion)
                    val hasNewIdentity = cloudIdentity.isNotBlank()
                    val hasNewSecret = cloudSecret.isNotBlank()
                    require(hasNewIdentity == hasNewSecret) { "Enter both credential fields or leave both blank" }
                }
                if (validation.isFailure) {
                    cloudMessage = validation.exceptionOrNull()?.message ?: "Cloud configuration is invalid"
                } else {
                    scope.launch {
                        val hasNewCredentials = cloudIdentity.isNotBlank()
                        val hasStoredCredentials = settingsRepository.hasCloudSaveCredentials(cloudProvider)
                        if (!hasNewCredentials && !hasStoredCredentials) {
                            cloudMessage = "Cloud credentials are required"
                            return@launch
                        }
                        settingsRepository.setCloudSaveConfiguration(cloudProvider, cloudEndpoint, cloudRegion)
                        if (hasNewCredentials) {
                            settingsRepository.setCloudSaveCredentials(cloudProvider, cloudIdentity, cloudSecret)
                            cloudIdentity = ""
                            cloudSecret = ""
                            cloudCredentialsConfigured = true
                        }
                        cloudMessage = "Cloud save configuration stored securely"
                    }
                }
            }) { Text("Save cloud settings") }
            OutlinedButton(
                enabled = cloudCredentialsConfigured,
                onClick = {
                    scope.launch {
                        settingsRepository.clearCloudSaveCredentials()
                        cloudIdentity = ""
                        cloudSecret = ""
                        cloudCredentialsConfigured = false
                        cloudMessage = "Cloud save credentials removed"
                    }
                },
            ) { Text("Clear credentials") }
        }
        cloudMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("System", sectionAnchor(SettingsSection.SYSTEM))
        SettingsActionRow("App storage", Icons.Rounded.Storage) { launchSystemSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) }
        SettingsActionRow("GameBox app details", Icons.Rounded.Info) { launchSystemSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) }
        SettingsActionRow("Android system settings", Icons.Rounded.Settings) { launchSystemSettings(Settings.ACTION_SETTINGS) }
        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.height(18.dp))
        SettingsSectionHeader("Developer and diagnostics")
        Text(
            "Export a sanitized recovery bundle when troubleshooting. Credentials, remote URLs, checksums, paths, and save contents are excluded.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp
        )
        OutlinedButton(
            onClick = { diagnosticsLauncher.launch("gamebox-diagnostics.zip") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export sanitized recovery bundle", modifier = Modifier.fillMaxWidth())
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
        Spacer(Modifier.height(18.dp))
        Text("TheGamesDB metadata", fontWeight = FontWeight.Bold)
        Text(
            if (theGamesDbConfigured)
                "API key configured securely. Enter a replacement key or clear it."
            else "Optional. Adds descriptions and HTTPS box art to authorized catalog entries.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
        OutlinedTextField(
            value = theGamesDbApiKey,
            onValueChange = { theGamesDbApiKey = it },
            label = { Text("TheGamesDB API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "TheGamesDB API key, hidden"
            }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Button(
                onClick = {
                    val key = theGamesDbApiKey.trim()
                    if (key.isEmpty()) {
                        catalogMessage = "Enter an API key or choose Clear"
                    } else {
                        scope.launch {
                            settingsRepository.setTheGamesDbApiKey(key)
                            theGamesDbApiKey = ""
                            theGamesDbConfigured = true
                            catalogMessage = "TheGamesDB API key stored securely. Refresh the Store to enrich metadata."
                        }
                    }
                }
            ) { Text(if (theGamesDbConfigured) "Replace key" else "Save key") }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        settingsRepository.setTheGamesDbApiKey(null)
                        theGamesDbApiKey = ""
                        theGamesDbConfigured = false
                        catalogMessage = "TheGamesDB metadata key removed"
                    }
                },
                enabled = theGamesDbConfigured
            ) { Text("Clear key") }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Runtime providers and emulator profiles remain intentionally scoped to their dedicated screens.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
    }
    val navigateToSection: (SettingsSection) -> Unit = { section ->
        selectedSection = section
        scope.launch { scrollState.animateScrollTo(sectionOffsets[section] ?: 0) }
    }
    if (compact) {
        Column(Modifier.fillMaxSize()) {
            SettingsSectionStrip(selectedSection, navigateToSection)
            Column(Modifier.weight(1f).verticalScroll(scrollState), content = settingsContent)
        }
    } else {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BlueprintRail(Modifier.width(246.dp)) {
                Text("SETTINGS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                SettingsSection.entries.forEach { section ->
                    val selected = section == selectedSection
                    val icon = settingsSectionIcon(section)
                    val interactionSource = remember(section) { MutableInteractionSource() }
                    val hovered by interactionSource.collectIsHoveredAsState()
                    Surface(
                        modifier = Modifier.fillMaxWidth().hoverable(interactionSource)
                            .blueprintClick { navigateToSection(section) }
                            .semantics {
                                this.selected = selected
                                contentDescription = "${section.label} settings section"
                            },
                        color = if (selected || hovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    ) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                            Text(section.label, modifier = Modifier.weight(1f).padding(start = 9.dp), fontSize = 11.sp)
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .75f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(scrollState).padding(18.dp),
                    content = settingsContent,
                )
            }
        }
    }
}

private fun settingsSectionIcon(section: SettingsSection): ImageVector = when (section) {
    SettingsSection.STORAGE -> Icons.Rounded.Storage
    SettingsSection.CONTROLLERS -> Icons.Rounded.SportsEsports
    SettingsSection.DOWNLOADS -> Icons.Rounded.Downloading
    SettingsSection.EMULATORS -> Icons.Rounded.Memory
    SettingsSection.DISPLAY -> Icons.Rounded.Monitor
    SettingsSection.AUDIO -> Icons.AutoMirrored.Rounded.VolumeUp
    SettingsSection.NETWORK -> Icons.Rounded.Wifi
    SettingsSection.SAVES_CLOUD -> Icons.Rounded.CloudSync
    SettingsSection.SYSTEM -> Icons.Rounded.Settings
}

@Composable
private fun SettingsSectionStrip(selected: SettingsSection, onSelect: (SettingsSection) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 10.dp),
    ) {
        items(SettingsSection.entries, key = { it.name }) { section ->
            FilterChip(
                selected = section == selected,
                onClick = { onSelect(section) },
                label = { Text(section.label) },
                leadingIcon = { Icon(settingsSectionIcon(section), null, Modifier.size(16.dp)) },
                modifier = Modifier.semantics { contentDescription = "${section.label} settings section" },
            )
        }
    }
}

@Composable
private fun SettingsStatusCard(icon: ImageVector, title: String, detail: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsActionRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val emphasized = hovered || focused
    val border by animateColorAsState(
        if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "settings-row-border",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .hoverable(interactionSource)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, border),
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp)
            .semantics { contentDescription = "Settings section: " + title }
    )
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


private fun connectedControllerLabel(): String {
    val controller = InputDevice.getDeviceIds()
        .asSequence()
        .mapNotNull { id -> InputDevice.getDevice(id) }
        .firstOrNull { device ->
            val sources = device.sources
            sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        }
    return controller?.name?.takeIf { it.isNotBlank() } ?: "Not connected"
}
