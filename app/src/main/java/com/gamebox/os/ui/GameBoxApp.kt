@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.AppContainer
import com.gamebox.os.domain.GameId
import com.gamebox.os.navigation.GameBoxNavigationRequest
import kotlinx.coroutines.launch

@Composable
fun GameBoxApp(
    container: AppContainer,
    navigationRequest: GameBoxNavigationRequest? = null,
    externalDisplayOverride: Boolean = false,
    externalDisplayBottomInsetPx: Int = 0,
    hostWindowHasFocus: Boolean = true,
    textInputExitRequest: Int = 0,
) {
    val focusManager = LocalFocusManager.current
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    val repository = container.gameRepository
    val gameLaunchController = container.gameLaunchController
    val settingsRepository = container.settingsRepository
    val games by repository.observeGames().collectAsState()
    val appSettings by settingsRepository.settings.collectAsState(
        initial = com.gamebox.os.settings.GameBoxSettings()
    )
    val context = LocalContext.current
    val browserDownloadStore = remember(context) {
        (context.applicationContext as com.gamebox.os.GameBoxApplication).browserDownloadStore
    }
    val browserDownloadRecords by browserDownloadStore.records.collectAsState()
    val downloadJobs by container.downloadRepository.observeJobs().collectAsState()
    val downloadActivityCount = remember(downloadJobs, browserDownloadRecords) {
        downloadActivityCount(
            remoteStatuses = downloadJobs.map { it.status },
            browserStatuses = browserDownloadRecords.map { it.status },
        )
    }
    val allowDeveloperOptions = LocalContext.current.applicationInfo.flags and
        android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    val hostDisplayId = LocalActivity.current?.window?.decorView?.display?.displayId
        ?: LocalView.current.display?.displayId
    // External HDMI/Desktop-mode surfaces are the living-room target even when
    // the OEM reports them with the phone's high-density dp configuration.
    val externalDisplay = externalDisplayOverride || (hostDisplayId != null &&
        hostDisplayId != android.view.Display.DEFAULT_DISPLAY
        )
    val externalDisplayBottomInset = externalDisplayBottomInsetDp(
        externalDisplay = externalDisplay,
        bottomInsetPx = externalDisplayBottomInsetPx,
        density = androidx.compose.ui.platform.LocalDensity.current.density,
    ).dp
    val runtimeDeviceStatus = rememberRuntimeDeviceStatus()
    val controllerActions = remember { ControllerActionRegistry() }
    val appScope = rememberCoroutineScope()
    var showProfileSwitcher by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(false) }
    var showSystemStatus by remember { mutableStateOf(false) }
    var focusStoreSearchOnEnter by remember { mutableStateOf(false) }
    val selectedTabFocusRequester = remember { FocusRequester() }
    val uiState = rememberGameBoxUiState()
    val destination = runCatching { Destination.valueOf(uiState.destination) }
        .getOrDefault(Destination.HOME)
    val selectedGameId = uiState.selectedGameId?.let(::GameId)
    fun handleBack(): Boolean = controllerActions.dispatchBack {
        when (
            gameBoxBackTarget(
                showQuickActions = showQuickActions,
                showSystemStatus = showSystemStatus,
                showProfileSwitcher = showProfileSwitcher,
                hasSelectedGame = selectedGameId != null,
            )
        ) {
            GameBoxBackTarget.QUICK_ACTIONS -> { showQuickActions = false; true }
            GameBoxBackTarget.SYSTEM_STATUS -> { showSystemStatus = false; true }
            GameBoxBackTarget.PROFILE_SWITCHER -> { showProfileSwitcher = false; true }
            GameBoxBackTarget.GAME_DETAILS -> { uiState.clearSelection(); true }
            GameBoxBackTarget.NONE -> {
                if (destination != Destination.HOME) uiState.openDestination(Destination.HOME.name)
                true
            }
        }
    }
    BackHandler(onBack = { handleBack() })
    val restorableGameId = uiState.restoreFocus(destination.name, games.map { it.id.value })?.let(::GameId)
    val rememberGameFocus: (GameId) -> Unit = { focused ->
        uiState.rememberFocus(destination.name, focused.value, games.map { it.id.value })
    }

    LaunchedEffect(navigationRequest?.requestId) {
        navigationRequest?.let { request ->
            val target = runCatching { Destination.valueOf(request.destination) }
                .getOrDefault(Destination.HOME)
            uiState.openDestination(target.name)
            request.gameId?.let(uiState::openGame)
        }
    }

    LaunchedEffect(textInputExitRequest) {
        if (textInputExitRequest > 0) {
            focusManager.clearFocus(force = true)
            softwareKeyboard?.hide()
        }
    }

    LaunchedEffect(destination, selectedGameId, hostWindowHasFocus) {
        if (!hostWindowHasFocus) return@LaunchedEffect
        if (destination != Destination.SETTINGS &&
            destination != Destination.STORE &&
            destination != Destination.DOWNLOADS &&
            selectedGameId == null
        ) {
            selectedTabFocusRequester.requestFocus()
        }
    }

    fun moveTab(offset: Int) {
        uiState.openDestination(destinationAfterPrimaryTabMove(destination, offset).name)
    }

    BlueprintViewport(
        safeAreaPercent = appSettings.safeAreaPercent,
        reducedMotion = appSettings.reducedMotion,
        uiSoundEffectsEnabled = appSettings.uiSoundEffectsEnabled,
        focusDebugOverlayEnabled = appSettings.focusDebugOverlayEnabled && allowDeveloperOptions,
        controllerActions = controllerActions,
        uiState = uiState,
        runtimeDeviceStatus = runtimeDeviceStatus,
    ) {
    val soundView = LocalView.current
    val uiSoundsEnabled = LocalUiSoundEffects.current
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .onGameBoxRootInput(
                soundsEnabled = uiSoundsEnabled,
                playSoundEffect = soundView::playSoundEffect,
                controllerActions = controllerActions,
                movePrimaryTab = ::moveTab,
                openQuickActions = { showQuickActions = true },
                openSystemStatus = { showSystemStatus = true },
                fallbackX = { uiState.openDestination(Destination.STORE.name) },
                fallbackY = { uiState.openDestination(Destination.SETTINGS.name) },
                handleBack = ::handleBack,
            )
    ) {
        val compact = if (externalDisplay) {
            false
        } else {
            shouldUseCompactLayout(
                mode = com.gamebox.os.settings.effectiveDeveloperLayoutMode(
                    appSettings.developerLayoutMode,
                    allowDeveloperOptions,
                ),
                widthDp = maxWidth.value,
                heightDp = maxHeight.value,
            )
        }
        BlueprintBackdrop(compact)
        Column(
            Modifier.fillMaxSize()
                // Keep backgrounds full-bleed, but inset controls from persistent OEM
                // taskbar/navigation overlays reported by the external display.
                .externalDisplaySystemOverlayInsets(externalDisplay, externalDisplayBottomInset)
                .padding(
                    start = if (compact) 16.dp else 24.dp,
                    end = if (compact) 16.dp else 24.dp,
                    top = if (compact) 10.dp else 14.dp,
                    // Leave a small breathing gap after any reported system overlay inset.
                    bottom = if (compact) 10.dp else 14.dp,
                )
        ) {
            TopNav(
                selected = destination,
                compact = compact,
                downloadActivityCount = downloadActivityCount,
                onSelect = { uiState.openDestination(it.name) },
                onSearch = {
                    focusStoreSearchOnEnter = true
                    uiState.openDestination(Destination.STORE.name)
                },
                selectedFocusRequester = selectedTabFocusRequester,
            )
            SessionRecoveryBanner(gameLaunchController)
            Spacer(Modifier.height(if (compact) 14.dp else 12.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                GameBoxDestinationContent(
                    container = container,
                    state = GameBoxRouteState(
                        destination = destination,
                        selectedGameId = selectedGameId,
                        games = games,
                        restorableGameId = restorableGameId,
                        rememberGameFocus = rememberGameFocus,
                        compact = compact,
                        uiState = uiState,
                        settings = appSettings,
                        focusStoreSearchOnEnter = focusStoreSearchOnEnter,
                        hostDisplayId = hostDisplayId,
                        externalDisplay = externalDisplay,
                        hostWindowHasFocus = hostWindowHasFocus,
                    ),
                    actions = GameBoxRouteActions(
                        openGame = { uiState.openGame(it.id.value) },
                        openDownloads = { uiState.openDestination(Destination.DOWNLOADS.name) },
                        backFromDetails = uiState::clearSelection,
                        openProfileSwitcher = { showProfileSwitcher = true },
                        requestStoreSearchFocus = { focusStoreSearchOnEnter = true },
                        onSearchFocusHandled = { focusStoreSearchOnEnter = false },
                    ),
                )
            }

            if (!compact) {
                Spacer(Modifier.height(8.dp))
                ControllerFooter(controllerActions.xLabel, controllerActions.yLabel, appSettings.activeProfileName)
            }
        }
        if (showQuickActions) {
            QuickActionsDialog(
                destination = destination,
                xLabel = controllerActions.xLabel,
                yLabel = controllerActions.yLabel,
                onX = {
                    showQuickActions = false
                    if (!controllerActions.invokeX()) uiState.openDestination(Destination.STORE.name)
                },
                onY = {
                    showQuickActions = false
                    if (!controllerActions.invokeY()) uiState.openDestination(Destination.SETTINGS.name)
                },
                onHome = {
                    showQuickActions = false
                    uiState.openDestination(Destination.HOME.name)
                },
                onDownloads = {
                    showQuickActions = false
                    uiState.openDestination(Destination.DOWNLOADS.name)
                },
                onSettings = {
                    showQuickActions = false
                    uiState.openDestination(Destination.SETTINGS.name)
                },
                onDismiss = { showQuickActions = false },
                autoFocusFirstAction = !compact,
            )
        }
        if (showSystemStatus) {
            SystemStatusDialog(onDismiss = { showSystemStatus = false })
        }
        if (showProfileSwitcher) {
            ProfileSwitcherDialog(
                activeProfile = appSettings.activeProfileName,
                onDismiss = { showProfileSwitcher = false },
                onSelect = { profile ->
                    appScope.launch { settingsRepository.setActiveProfileName(profile) }
                    showProfileSwitcher = false
                },
            )
        }
    }
}
}
