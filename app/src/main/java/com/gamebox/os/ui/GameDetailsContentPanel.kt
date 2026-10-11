@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.primaryAction
import com.gamebox.os.download.AuthorizedDownloadState
import com.gamebox.os.launch.LaunchUiState

internal fun authorizedDownloadStatusMessage(
    status: AuthorizedDownloadState.Status,
    contentInstalled: Boolean,
): String? = when (status) {
    AuthorizedDownloadState.Status.IDLE -> null
    AuthorizedDownloadState.Status.QUEUED -> "Waiting to install authorized game…"
    AuthorizedDownloadState.Status.RUNNING -> "Installing and verifying game…"
    AuthorizedDownloadState.Status.SUCCEEDED ->
        "Game content verified · ready to play".takeIf { contentInstalled }
    AuthorizedDownloadState.Status.MISSING_CONTENT -> "Installed game file is missing · reinstall to repair"
    AuthorizedDownloadState.Status.ALTERED_CONTENT -> "Game file verification failed · reinstall to repair"
    AuthorizedDownloadState.Status.FAILED -> "Game installation failed"
    AuthorizedDownloadState.Status.CANCELLED -> "Game installation cancelled"
}

internal fun isAuthorizedDownloadInProgress(status: AuthorizedDownloadState.Status): Boolean =
    status == AuthorizedDownloadState.Status.QUEUED ||
        status == AuthorizedDownloadState.Status.RUNNING

internal data class GameDetailsContentState(
    val game: Game,
    val requiredEmulatorCore: String?,
    val authorizedState: AuthorizedDownloadState,
    val launchState: LaunchUiState,
    val authorizedFixture: Boolean,
    val workerActive: Boolean,
    val saveDescription: String,
    val compact: Boolean,
    val hasApprovedEmulator: Boolean = true,
)

internal data class GameDetailsContentActions(
    val onPrimaryAction: () -> Unit,
    val onCancelDownload: () -> Unit,
    val onPlay: () -> Unit,
    val onUninstall: () -> Unit,
    val onPauseDownload: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onBack: () -> Unit,
    val onRefreshSaves: () -> Unit,
    val onCancelPreparation: () -> Unit,
)

@Composable
internal fun GameDetailsContentPanel(
    state: GameDetailsContentState,
    actions: GameDetailsContentActions,
) {
    val game = state.game
    val launchState = state.launchState
    val missingInstall = game.state in setOf(
        InstallState.NOT_INSTALLED,
        InstallState.FAILED,
        InstallState.MISSING_FILES,
    )
    val installerUnavailable = !state.authorizedFixture && missingInstall &&
        (game.sourceUrl == null || game.expectedSha256 == null)
    val howToPlay = if (installerUnavailable) {
        "How to play: no verified installer is configured. Use Import your copy below, then press Play after verification."
    } else {
        "How to play: install this authorized game, install the approved emulator shown in Game settings, then press Play after verification."
    }
    val canUninstall = launchState.status !in setOf(
        LaunchUiState.Status.PREPARING,
        LaunchUiState.Status.LAUNCHED,
        LaunchUiState.Status.SESSION_ERROR,
    )

    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(9.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Game content · ${game.state.displayName()}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            state.requiredEmulatorCore?.let { requiredCore ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Required core: $requiredCore",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics {
                        contentDescription = "Required emulator core: $requiredCore"
                    },
                )
            }
            if (game.state !in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE)) {
                Spacer(Modifier.height(8.dp))
                Text(
                    howToPlay,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    modifier = Modifier.semantics {
                        contentDescription = howToPlay
                    },
                )
            }
            if (game.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE) &&
                !state.hasApprovedEmulator
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "No approved emulator is available for ${game.platform}. This game can stay in your library, but GameBox cannot launch it yet.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics {
                        contentDescription = "Play unavailable: no approved emulator for ${game.platform}"
                    },
                )
            }
            val downloadStatus = state.authorizedState.status
            authorizedDownloadStatusMessage(
                downloadStatus,
                contentInstalled = game.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE),
            )
                ?.takeIf { state.authorizedFixture }
                ?.let { statusMessage ->
                Spacer(Modifier.height(12.dp))
                Text(
                    statusMessage,
                    color = when (downloadStatus) {
                        AuthorizedDownloadState.Status.SUCCEEDED -> MaterialTheme.colorScheme.tertiary
                        AuthorizedDownloadState.Status.MISSING_CONTENT,
                        AuthorizedDownloadState.Status.ALTERED_CONTENT,
                        AuthorizedDownloadState.Status.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
                if (state.authorizedFixture && isAuthorizedDownloadInProgress(downloadStatus)) {
                    Text(
                        "${(state.authorizedState.progress * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                    LinearProgressIndicator(
                        progress = { state.authorizedState.progress },
                        modifier = Modifier.fillMaxWidth().testTag("authorized-install-progress"),
                    )
                }
                state.authorizedState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(18.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BlueprintButton(
                    onClick = actions.onPrimaryAction,
                    modifier = (if (state.compact) Modifier.fillMaxWidth() else Modifier)
                        .bringFocusedControlIntoView(),
                    enabled = when {
                        state.authorizedFixture -> !state.workerActive
                        game.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE) &&
                            !state.hasApprovedEmulator -> false
                        game.state in setOf(
                            InstallState.NOT_INSTALLED,
                            InstallState.FAILED,
                            InstallState.MISSING_FILES,
                        ) -> game.sourceUrl != null && game.expectedSha256 != null
                        else -> true
                    },
                ) {
                    Text(
                        if (!state.authorizedFixture &&
                            game.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE) &&
                            !state.hasApprovedEmulator
                        ) "Unsupported system"
                        else if (installerUnavailable) "No installer"
                        else if (!state.authorizedFixture) game.state.primaryAction()
                        else if (state.authorizedState.status == AuthorizedDownloadState.Status.SUCCEEDED)
                            "Reinstall game" else "Install game",
                    )
                }
                if (state.authorizedFixture && state.workerActive) {
                    BlueprintOutlinedButton(
                        onClick = actions.onCancelDownload,
                        modifier = Modifier.bringFocusedControlIntoView(),
                    ) { Text("Cancel") }
                }
                if (state.authorizedFixture && game.state in setOf(
                        InstallState.INSTALLED,
                        InstallState.UPDATE_AVAILABLE,
                    )
                ) {
                    BlueprintButton(
                        onClick = actions.onPlay,
                        modifier = Modifier.bringFocusedControlIntoView(),
                    ) { Text("Play") }
                    TvDetailsSecondaryAction(
                        label = "Uninstall content",
                        compact = state.compact,
                        enabled = canUninstall,
                        onClick = actions.onUninstall,
                    )
                }
                if (!state.authorizedFixture && game.state in setOf(
                        InstallState.INSTALLED,
                        InstallState.UPDATE_AVAILABLE,
                        InstallState.MISSING_FILES,
                    )
                ) {
                    TvDetailsSecondaryAction(
                        label = "Uninstall content",
                        compact = state.compact,
                        enabled = canUninstall,
                        onClick = actions.onUninstall,
                    )
                }
                if (!state.authorizedFixture && game.state == InstallState.DOWNLOADING &&
                    game.sourceUrl != null && game.expectedSha256 != null
                ) {
                    BlueprintOutlinedButton(
                        onClick = actions.onPauseDownload,
                        modifier = Modifier.bringFocusedControlIntoView(),
                    ) { Text("Pause") }
                }
                BlueprintOutlinedButton(
                    onClick = actions.onToggleFavorite,
                    modifier = Modifier.bringFocusedControlIntoView(),
                ) {
                    Text(if (game.favorite) "Remove favorite" else "Favorite")
                }
                BlueprintOutlinedButton(
                    onClick = actions.onBack,
                    modifier = Modifier.bringFocusedControlIntoView(),
                ) { Text("Back") }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = state.saveDescription
                },
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Managed save data", fontWeight = FontWeight.Bold)
                    Text(
                        state.saveDescription,
                        color = if (game.savePresent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                    Text(
                        "Emulator-owned saves require a configured connection; they are not included in this scan.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                    BlueprintOutlinedButton(
                        onClick = actions.onRefreshSaves,
                        modifier = Modifier.bringFocusedControlIntoView(),
                    ) { Text("Refresh saves") }
                }
            }
            if (launchState.gameId == game.id && launchState.status != LaunchUiState.Status.IDLE) {
                if (launchState.status == LaunchUiState.Status.PREPARING) {
                    BlueprintOutlinedButton(
                        onClick = actions.onCancelPreparation,
                        modifier = Modifier.bringFocusedControlIntoView(),
                    ) { Text("Cancel preparation") }
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
                        LaunchUiState.Status.HANDOFF_REJECTED,
                    )) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
