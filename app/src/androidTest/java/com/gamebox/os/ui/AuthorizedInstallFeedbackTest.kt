package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.download.AuthorizedDownloadState
import com.gamebox.os.download.AuthorizedHomebrewDownload
import com.gamebox.os.launch.LaunchUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthorizedInstallFeedbackTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun successfulInstallShowsReadyMessageWithoutStaleProgressBar() {
        showPanel(
            AuthorizedDownloadState(
                status = AuthorizedDownloadState.Status.SUCCEEDED,
                bytesTransferred = AuthorizedHomebrewDownload.SIZE_BYTES,
            ),
        )

        compose.onNodeWithText("Game content verified · ready to play").assertIsDisplayed()
        compose.onNodeWithTag("authorized-install-progress").assertDoesNotExist()
    }

    @Test
    fun uninstallDoesNotLeaveAStaleSuccessfulInstallMessage() {
        showPanel(
            AuthorizedDownloadState(
                status = AuthorizedDownloadState.Status.SUCCEEDED,
                bytesTransferred = AuthorizedHomebrewDownload.SIZE_BYTES,
            ),
            contentInstalled = false,
        )

        compose.onNodeWithText("Game content · not installed").assertIsDisplayed()
        compose.onNodeWithText("Game content verified · ready to play").assertDoesNotExist()
        compose.onNodeWithTag("authorized-install-progress").assertDoesNotExist()
    }

    @Test
    fun activeInstallShowsProgressAndPercent() {
        showPanel(
            AuthorizedDownloadState(
                status = AuthorizedDownloadState.Status.RUNNING,
                bytesTransferred = AuthorizedHomebrewDownload.SIZE_BYTES / 2,
            ),
        )

        compose.onNodeWithText("Installing and verifying game…").assertIsDisplayed()
        compose.onNodeWithText("50%").assertIsDisplayed()
        compose.onNodeWithTag("authorized-install-progress").assertIsDisplayed()
    }

    @Test
    fun gamepadAActivatesFocusedInstallActionOnce() {
        var installs = 0
        showPanel(AuthorizedDownloadState(), contentInstalled = false, onPrimaryAction = { installs++ })

        compose.onNodeWithText("Install game")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, installs) }
    }

    private fun showPanel(
        download: AuthorizedDownloadState,
        contentInstalled: Boolean = download.status == AuthorizedDownloadState.Status.SUCCEEDED,
        onPrimaryAction: () -> Unit = {},
    ) {
        val game = Game(
            id = GameId("galaxy-patrol"),
            title = "Galaxy Patrol",
            platform = "Retro",
            year = 2018,
            genre = "Arcade",
            sizeMb = 1,
            state = if (contentInstalled) InstallState.INSTALLED
            else if (download.status == AuthorizedDownloadState.Status.SUCCEEDED) InstallState.NOT_INSTALLED
            else InstallState.DOWNLOADING,
        )
        compose.setContent {
            MaterialTheme {
                GameDetailsContentPanel(
                    state = GameDetailsContentState(
                        game = game,
                        requiredEmulatorCore = null,
                        authorizedState = download,
                        launchState = LaunchUiState(),
                        authorizedFixture = true,
                        workerActive = download.status in setOf(
                            AuthorizedDownloadState.Status.QUEUED,
                            AuthorizedDownloadState.Status.RUNNING,
                        ),
                        saveDescription = "No managed save files found",
                        compact = true,
                    ),
                    actions = GameDetailsContentActions(
                        onPrimaryAction = onPrimaryAction,
                        onCancelDownload = {},
                        onPlay = {},
                        onUninstall = {},
                        onPauseDownload = {},
                        onToggleFavorite = {},
                        onBack = {},
                        onRefreshSaves = {},
                        onCancelPreparation = {},
                    ),
                )
            }
        }
    }
}
