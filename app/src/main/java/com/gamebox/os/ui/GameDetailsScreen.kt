@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import android.content.*
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.relocation.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.gamebox.os.content.*
import com.gamebox.os.data.*
import com.gamebox.os.domain.*
import com.gamebox.os.download.*
import com.gamebox.os.importer.*
import com.gamebox.os.launch.*
import com.gamebox.os.save.*
import com.gamebox.os.settings.*
import com.gamebox.os.source.*
import com.gamebox.os.storage.*
import kotlinx.coroutines.*

internal enum class DetailControllerPrimaryAction {
    PRIMARY, OPEN_DOWNLOADS, OPEN_GAME_SETTINGS
}

internal data class DetailControllerActionPlan(
    val xLabel: String,
    val xAction: DetailControllerPrimaryAction,
    val yLabel: String,
)

internal fun detailControllerActionPlan(
    state: InstallState,
    favorite: Boolean,
    authorizedFixture: Boolean,
    workerActive: Boolean,
    hasRemoteSource: Boolean,
): DetailControllerActionPlan {
    val action = when {
        workerActive -> DetailControllerPrimaryAction.OPEN_DOWNLOADS
        state in setOf(
            InstallState.QUEUED,
            InstallState.DOWNLOADING,
            InstallState.VERIFYING,
            InstallState.INSTALLING,
        ) -> DetailControllerPrimaryAction.OPEN_DOWNLOADS
        state == InstallState.PAUSED && !hasRemoteSource ->
            DetailControllerPrimaryAction.OPEN_DOWNLOADS
        state in setOf(InstallState.NOT_INSTALLED, InstallState.FAILED, InstallState.MISSING_FILES) &&
            !authorizedFixture && !hasRemoteSource ->
            DetailControllerPrimaryAction.OPEN_GAME_SETTINGS
        else -> DetailControllerPrimaryAction.PRIMARY
    }
    val label = when (action) {
        DetailControllerPrimaryAction.OPEN_DOWNLOADS -> "View download"
        DetailControllerPrimaryAction.OPEN_GAME_SETTINGS -> "Game settings"
        DetailControllerPrimaryAction.PRIMARY -> when {
            state == InstallState.INSTALLED -> "Play"
            state == InstallState.UPDATE_AVAILABLE -> "Update"
            state == InstallState.PAUSED -> "Resume"
            state == InstallState.FAILED -> "Retry"
            authorizedFixture -> "Install game"
            else -> state.primaryAction()
        }
    }
    return DetailControllerActionPlan(
        xLabel = label,
        xAction = action,
        yLabel = if (favorite) "Unfavorite" else "Favorite",
    )
}

internal fun shouldShowAuthorizedImportCard(state: InstallState, authorizedFixture: Boolean): Boolean =
    !authorizedFixture && state in setOf(
        InstallState.NOT_INSTALLED,
        InstallState.MISSING_FILES,
        InstallState.FAILED,
    )

@Composable
internal fun DetailsScreen(
    game: Game,
    repository: GameRepository,
    settingsRepository: SettingsRepository,
    downloadRepository: DownloadRepository,
    authorizedDownloadController: AuthorizedDownloadController,
    remoteDownloadController: RemoteDownloadController,
    gameLaunchController: GameLaunchController,
    saveSafetyController: SaveSafetyController,
    managedSaveDiscovery: com.gamebox.os.storage.ManagedSaveDiscovery,
    importer: AuthorizedRomImporter,
    discoveryRepository: CatalogDiscoveryRepository,
    lookupScreenshots: suspend (Game) -> List<String>,
    saveControllerFactory: ((GameId, kotlinx.coroutines.CoroutineScope) -> SaveSafetyController)?,
    hostDisplayId: Int?,
    compact: Boolean,
    onDownloads: () -> Unit,
    onOpenStore: () -> Unit = {},
    onBack: () -> Unit
) {
    val launchActivity = LocalActivity.current
    val discovery by remember(game.id, discoveryRepository) {
        discoveryRepository.observeGame(game.id)
    }.collectAsState(initial = null)
    val providerScreenshots by produceState(
        initialValue = emptyList(),
        game.id,
        game.title,
        game.platform,
        game.region,
        lookupScreenshots,
    ) {
        value = try {
            lookupScreenshots(game).distinct()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
    }
    val detail = remember(game, discovery, providerScreenshots) {
        GameDetailPresentation.from(game, discovery, providerScreenshots)
    }
    val requiredEmulatorCore = remember(game) { GameDetailPresentation.requiredEmulatorCore(game) }
    val isAuthorizedFixture = game.id.value == "galaxy-patrol"
    val saveContext = rememberGameDetailsSaveContext(
        gameId = game.id,
        fallbackController = saveSafetyController,
        discovery = managedSaveDiscovery,
        controllerFactory = saveControllerFactory,
        usePerGameController = game.id.value != "galaxy-patrol",
    )
    val activeSaveController = saveContext.controller
    val contentSafetyController = saveContext.controller
    val authorizedState by authorizedDownloadController.observeState().collectAsState()
    val launchState by gameLaunchController.observeState().collectAsState()
    var showUninstallConfirmation by remember(game.id) { mutableStateOf(false) }
    var showGameSettings by remember(game.id) { mutableStateOf(false) }
    var showMetadataEditor by remember(game.id) { mutableStateOf(false) }
    val workerActive = authorizedState.status == AuthorizedDownloadState.Status.QUEUED ||
        authorizedState.status == AuthorizedDownloadState.Status.RUNNING
    val hasRemoteSource = game.sourceUrl != null && game.expectedSha256 != null
    val hasApprovedEmulator = remember(game, gameLaunchController) {
        gameLaunchController.hasApprovedAdapter(game)
    }
    val primaryActionFocusRequester = remember(game.id) { FocusRequester() }
    val discoveryActionFocusRequester = remember(game.id) { FocusRequester() }
    LaunchedEffect(game.id, compact) {
        if (!compact) {
            androidx.compose.runtime.withFrameNanos { }
            // Details enters through the shared 220 ms TV transition. Requesting
            // focus before that transition has attached the action leaves the
            // window-level root as the only focused semantics node.
            kotlinx.coroutines.delay(300)
            runCatching { primaryActionFocusRequester.requestFocus() }
        }
    }
    fun performPrimaryAction() {
        if (isAuthorizedFixture) {
            authorizedDownloadController.install()
        } else {
            when (game.state) {
                InstallState.NOT_INSTALLED, InstallState.FAILED, InstallState.MISSING_FILES ->
                    if (hasRemoteSource) remoteDownloadController.install(game)
                    else repository.setInstallState(game.id, InstallState.FAILED)
                InstallState.PAUSED ->
                    if (hasRemoteSource) remoteDownloadController.resume(game) else onDownloads()
                InstallState.QUEUED, InstallState.DOWNLOADING, InstallState.VERIFYING,
                InstallState.INSTALLING -> onDownloads()
                InstallState.INSTALLED -> {
                    if (launchActivity != null) {
                        gameLaunchController.launchOnDisplay(game, hostDisplayId, launchActivity)
                    } else {
                        gameLaunchController.launchOnDisplay(game, hostDisplayId)
                    }
                }
                InstallState.UPDATE_AVAILABLE ->
                    if (hasRemoteSource) remoteDownloadController.install(game) else onDownloads()
            }
        }
    }
    val controllerActions = LocalControllerActions.current
    val actionPlan = detailControllerActionPlan(
        state = game.state,
        favorite = game.favorite,
        authorizedFixture = isAuthorizedFixture,
        workerActive = workerActive,
        hasRemoteSource = hasRemoteSource,
    )
    DisposableEffect(controllerActions, actionPlan, game) {
        val lease = controllerActions?.configure(
            xLabel = actionPlan.xLabel,
            onX = {
                when (actionPlan.xAction) {
                    DetailControllerPrimaryAction.PRIMARY -> performPrimaryAction()
                    DetailControllerPrimaryAction.OPEN_DOWNLOADS -> onDownloads()
                    DetailControllerPrimaryAction.OPEN_GAME_SETTINGS -> showGameSettings = true
                }
            },
            yLabel = actionPlan.yLabel,
            onY = { repository.setFavorite(game.id, !game.favorite) },
        )
        onDispose {
            val owner = controllerActions
            if (owner != null && lease != null) owner.clear(lease)
        }
    }
    if (showUninstallConfirmation) {
        ContentRemovalDialog(game, contentSafetyController) { showUninstallConfirmation = false }
    }
    if (showMetadataEditor) {
        GameMetadataEditorDialog(game, repository) { showMetadataEditor = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(restoredScrollState("details." + game.id.value))) {
        GameDetailsHero(
            game = game,
            detail = detail,
            compact = compact,
            authorizedFixture = isAuthorizedFixture,
            workerActive = workerActive,
            hasRemoteSource = hasRemoteSource,
            hasApprovedEmulator = hasApprovedEmulator,
            primaryActionFocusRequester = primaryActionFocusRequester,
            discoveryActionFocusRequester = discoveryActionFocusRequester,
            onPlay = {
                if (launchActivity != null) gameLaunchController.launchOnDisplay(game, hostDisplayId, launchActivity)
                else gameLaunchController.launchOnDisplay(game, hostDisplayId)
            },
            onInstallAuthorized = authorizedDownloadController::install,
            onInstallRemote = { remoteDownloadController.install(game) },
        )
        Spacer(Modifier.height(8.dp))
        GameDetailsDiscoveryCard(compact, onOpenStore, discoveryActionFocusRequester)
        GameDetailsManualImportCard(
            game = game,
            importer = importer,
            repository = repository,
            enabled = !workerActive && launchState.status !in setOf(
                LaunchUiState.Status.PREPARING,
                LaunchUiState.Status.LAUNCHED,
                LaunchUiState.Status.SESSION_ERROR,
            ),
        )
        if (detail.hasAboutContent) {
            BlueprintPanel(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text("About this game", Modifier.semantics { heading() }, fontWeight = FontWeight.SemiBold)
                if (detail.hasDescription) {
                    Text(detail.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    detail.region?.let { DetailMetric(Icons.Rounded.Public, it) }
                    detail.ratingLabel?.let { DetailMetric(Icons.Rounded.Star, it) }
                }
            }
        }
        GameScreenshotGallery(detail, compact)
        Spacer(Modifier.height(8.dp))
        ImportedGameReimportCard(
            game,
            importer,
            repository,
            enabled = launchState.status !in setOf(
                LaunchUiState.Status.PREPARING,
                LaunchUiState.Status.LAUNCHED,
                LaunchUiState.Status.SESSION_ERROR,
            ),
        )
        PerGameSaveCard(
            game,
            activeSaveController,
            compact = compact,
            enabled = launchState.status !in setOf(
                LaunchUiState.Status.PREPARING,
                LaunchUiState.Status.LAUNCHED,
                LaunchUiState.Status.SESSION_ERROR,
            ),
        )
        Spacer(Modifier.height(8.dp))
        GameDetailsToolsSection(
            showGameSettings = showGameSettings,
            metadataLabel = if (game.metadataOverrides.isEmpty) "Edit metadata" else "Metadata corrected",
            reducedMotion = LocalReducedMotion.current,
            onToggleGameSettings = { showGameSettings = !showGameSettings },
            onEditMetadata = { showMetadataEditor = true },
        ) {
            GameSettingsPanel(game = game, repository = repository, settingsRepository = settingsRepository)
        }
        Spacer(Modifier.height(10.dp))
        GameDetailsContentPanel(
            state = GameDetailsContentState(
                game = game,
                requiredEmulatorCore = requiredEmulatorCore,
                authorizedState = authorizedState,
                launchState = launchState,
                authorizedFixture = isAuthorizedFixture,
                workerActive = workerActive,
                saveDescription = saveContext.description,
                compact = compact,
                hasApprovedEmulator = hasApprovedEmulator,
            ),
            actions = GameDetailsContentActions(
                onPrimaryAction = ::performPrimaryAction,
                onCancelDownload = authorizedDownloadController::cancel,
                onPlay = {
                    if (launchActivity != null) gameLaunchController.launchOnDisplay(game, hostDisplayId, launchActivity)
                    else gameLaunchController.launchOnDisplay(game, hostDisplayId)
                },
                onUninstall = { showUninstallConfirmation = true },
                onPauseDownload = { remoteDownloadController.pause(game) },
                onToggleFavorite = { repository.setFavorite(game.id, !game.favorite) },
                onBack = onBack,
                onRefreshSaves = managedSaveDiscovery::refresh,
                onCancelPreparation = gameLaunchController::cancelPreparation,
            ),
        )
    }
}
