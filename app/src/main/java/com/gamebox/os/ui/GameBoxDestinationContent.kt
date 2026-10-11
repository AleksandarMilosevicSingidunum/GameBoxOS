package com.gamebox.os.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.gamebox.os.data.AppContainer
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.belongsToLibrary
import com.gamebox.os.settings.GameBoxSettings

internal data class GameBoxRouteState(
    val destination: Destination,
    val selectedGameId: GameId?,
    val games: List<Game>,
    val restorableGameId: GameId?,
    val rememberGameFocus: (GameId) -> Unit,
    val compact: Boolean,
    val uiState: GameBoxUiState,
    val settings: GameBoxSettings,
    val focusStoreSearchOnEnter: Boolean,
    val hostDisplayId: Int?,
    val externalDisplay: Boolean,
    val hostWindowHasFocus: Boolean,
)

internal data class GameBoxRouteActions(
    val openGame: (Game) -> Unit,
    val openDownloads: () -> Unit,
    val backFromDetails: () -> Unit,
    val openProfileSwitcher: () -> Unit,
    val requestStoreSearchFocus: () -> Unit,
    val onSearchFocusHandled: () -> Unit,
)

@Composable
internal fun GameBoxDestinationContent(
    container: AppContainer,
    state: GameBoxRouteState,
    actions: GameBoxRouteActions,
) {
    BlueprintScreenTransition(
        route = BlueprintScreenRoute(state.destination, state.selectedGameId?.value),
    ) { animatedRoute ->
        val animatedState = state.copy(
            destination = animatedRoute.destination,
            selectedGameId = animatedRoute.selectedGameId?.let(::GameId),
        )
        val repository = container.gameRepository
        val settingsRepository = container.settingsRepository
        val saveControllerFactory = remember(container) { container::createSaveSafetyController }
        val selected = animatedState.selectedGameId?.let(repository::game)
        if (selected != null) {
            DetailsScreen(
                selected,
                repository,
                settingsRepository,
                container.downloadRepository,
                container.authorizedDownloadController,
                container.remoteDownloadController,
                container.gameLaunchController,
                container.saveSafetyController,
                container.managedSaveDiscovery,
                discoveryRepository = container.catalogDiscoveryRepository,
                lookupScreenshots = container::gameScreenshots,
                importer = container.authorizedRomImporter,
                saveControllerFactory = saveControllerFactory,
                hostDisplayId = animatedState.hostDisplayId,
                compact = animatedState.compact,
                onDownloads = actions.openDownloads,
                onOpenStore = {
                    animatedState.uiState.requestStoreVimmSearch()
                    animatedState.uiState.openDestination(Destination.STORE.name)
                },
                onBack = actions.backFromDetails,
            )
        } else {
            when (animatedState.destination) {
                Destination.HOME -> HomeScreen(
                    animatedState.games,
                    animatedState.restorableGameId,
                    animatedState.rememberGameFocus,
                    animatedState.compact,
                    hostWindowHasFocus = animatedState.hostWindowHasFocus,
                    openPc = { animatedState.uiState.openDestination(Destination.PC.name) },
                    openStore = {
                        animatedState.uiState.requestStoreVimmSearch()
                        animatedState.uiState.openDestination(Destination.STORE.name)
                    },
                    openSearch = {
                        actions.requestStoreSearchFocus()
                        animatedState.uiState.openDestination(Destination.STORE.name)
                    },
                    openProfileSwitcher = actions.openProfileSwitcher,
                    allowDefaultHomeSetupPrompt = !animatedState.externalDisplay,
                    open = actions.openGame,
                    openStorageSettings = { animatedState.uiState.openSettings(SettingsSection.STORAGE) },
                    openControllerSettings = { animatedState.uiState.openSettings(SettingsSection.CONTROLLERS) },
                )

                Destination.LIBRARY -> CollectionScreen(
                    "Your Library",
                    "Installed games and retained imports",
                    animatedState.games.filter { it.belongsToLibrary() },
                    animatedState.restorableGameId,
                    animatedState.rememberGameFocus,
                    animatedState.compact,
                    animatedState.uiState,
                    controllerKeyboard = !animatedState.compact,
                    onStore = { animatedState.uiState.openDestination(Destination.STORE.name) },
                    open = actions.openGame,
                )

                Destination.STORE -> CatalogScreen(
                    container.gameRepository,
                    container.catalogDiscoveryRepository,
                    container.authorizedRomImporter,
                    animatedState.games,
                    animatedState.restorableGameId,
                    animatedState.rememberGameFocus,
                    animatedState.compact,
                    settingsRepository = settingsRepository,
                    gameSources = animatedState.settings.gameSources,
                    uiState = animatedState.uiState,
                    focusSearchOnEnter = animatedState.focusStoreSearchOnEnter,
                    hostWindowHasFocus = animatedState.hostWindowHasFocus,
                    onSearchFocusHandled = actions.onSearchFocusHandled,
                    controllerKeyboard = !animatedState.compact,
                    open = actions.openGame,
                    onOpenDownloads = actions.openDownloads,
                )

                Destination.DOWNLOADS -> DownloadsScreen(
                    container.gameRepository,
                    container.downloadRepository,
                    container.remoteDownloadController,
                    animatedState.compact,
                    onStore = { animatedState.uiState.openDestination(Destination.STORE.name) },
                    onSettings = { animatedState.uiState.openDestination(Destination.SETTINGS.name) },
                )

                Destination.MEDIA -> AppHubScreen(
                    "Media",
                    "Launch your living-room apps and return to GameBox",
                    mediaShortcuts,
                    animatedState.compact,
                    settingsRepository,
                    AppHubKind.MEDIA,
                )

                Destination.PC -> AppHubScreen(
                    "PC Hub",
                    "Streaming, Windows, Linux, files, browser, and desktop tools",
                    pcShortcuts,
                    animatedState.compact,
                    settingsRepository,
                    AppHubKind.PC,
                )

                Destination.SETTINGS -> SettingsScreen(
                    animatedState.compact,
                    settingsRepository,
                    container.gameRepository,
                    container.downloadRepository,
                    container.catalogProvider,
                    requestedSection = animatedState.uiState.requestedSettingsSection(),
                    onSectionRequestConsumed = animatedState.uiState::consumeSettingsSectionRequest,
                )
            }
        }
    }
}
