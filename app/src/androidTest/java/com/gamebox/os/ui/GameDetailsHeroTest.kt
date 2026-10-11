package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.download.AuthorizedDownloadState
import com.gamebox.os.launch.LaunchUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDetailsHeroTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun installedGameShowsAndInvokesPlayAction() {
        var plays = 0
        val game = testGame(InstallState.INSTALLED)
        compose.setContent {
            MaterialTheme {
                val primaryActionFocusRequester = remember { FocusRequester() }
                GameDetailsHero(
                    game = game,
                    detail = GameDetailPresentation.from(game),
                    compact = false,
                    authorizedFixture = false,
                    workerActive = false,
                    hasRemoteSource = false,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    onPlay = { plays++ },
                    onInstallAuthorized = {},
                    onInstallRemote = {},
                )
            }
        }

        compose.onNodeWithText("Play Now").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, plays) }
    }

    @Test
    fun unsupportedInstalledGameExplainsWhyPlayIsUnavailable() {
        var plays = 0
        val game = testGame(InstallState.INSTALLED).copy(platform = "Switch")
        compose.setContent {
            MaterialTheme {
                val primaryActionFocusRequester = remember { FocusRequester() }
                GameDetailsHero(
                    game = game,
                    detail = GameDetailPresentation.from(game),
                    compact = false,
                    authorizedFixture = false,
                    workerActive = false,
                    hasRemoteSource = false,
                    hasApprovedEmulator = false,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    onPlay = { plays++ },
                    onInstallAuthorized = {},
                    onInstallRemote = {},
                )
            }
        }

        compose.onNodeWithText("No approved emulator").assertIsNotEnabled()
        compose.onNodeWithText("GameBox cannot launch Switch titles yet.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, plays) }
    }

    @Test
    fun listingWithoutVerifiedInstallerExplainsPersonalImportPath() {
        val game = testGame(InstallState.NOT_INSTALLED)
        compose.setContent {
            MaterialTheme {
                GameDetailsHero(
                    game = game,
                    detail = GameDetailPresentation.from(game),
                    compact = false,
                    authorizedFixture = false,
                    workerActive = false,
                    hasRemoteSource = false,
                    primaryActionFocusRequester = remember { FocusRequester() },
                    onPlay = {},
                    onInstallAuthorized = {},
                    onInstallRemote = {},
                )
            }
        }

        compose.onNodeWithText(
            "No verified installer is configured. Import your own game copy from Library.",
        ).assertIsDisplayed()
    }

    @Test
    fun listingWithoutVerifiedInstallerDoesNotOfferDisabledInstallActionAsReady() {
        compose.setContent {
            MaterialTheme {
                GameDetailsContentPanel(
                    state = GameDetailsContentState(
                        game = testGame(InstallState.NOT_INSTALLED),
                        requiredEmulatorCore = null,
                        authorizedState = AuthorizedDownloadState(),
                        launchState = LaunchUiState(),
                        authorizedFixture = false,
                        workerActive = false,
                        saveDescription = "No managed save files found",
                        compact = true,
                    ),
                    actions = GameDetailsContentActions(
                        onPrimaryAction = {}, onCancelDownload = {}, onPlay = {}, onUninstall = {},
                        onPauseDownload = {}, onToggleFavorite = {}, onBack = {},
                        onRefreshSaves = {}, onCancelPreparation = {},
                    ),
                )
            }
        }

        compose.onNodeWithText(
            "How to play: this listing has no verified installer. Import your own game copy from Library, then press Play after verification.",
        ).assertIsDisplayed()
        compose.onNodeWithText("No installer").assertIsNotEnabled()
        compose.onNodeWithText("Install").assertDoesNotExist()
    }

    @Test
    fun unsupportedInstalledGameDisablesContentPanelPlayAction() {
        var plays = 0
        val game = testGame(InstallState.INSTALLED).copy(platform = "Switch")
        compose.setContent {
            MaterialTheme {
                GameDetailsContentPanel(
                    state = GameDetailsContentState(
                        game = game,
                        requiredEmulatorCore = null,
                        authorizedState = AuthorizedDownloadState(),
                        launchState = LaunchUiState(),
                        authorizedFixture = false,
                        workerActive = false,
                        saveDescription = "No managed save files found",
                        compact = true,
                        hasApprovedEmulator = false,
                    ),
                    actions = GameDetailsContentActions(
                        onPrimaryAction = { plays++ },
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

        compose.onNodeWithText("No approved emulator is available for Switch. This game can stay in your library, but GameBox cannot launch it yet.")
            .assertIsDisplayed()
        compose.onNodeWithText("Unsupported system").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, plays) }
    }

    @Test
    fun dpadDownFromPrimaryActionFocusesVimmDownloadAction() {
        var storeOpens = 0
        val game = testGame(InstallState.INSTALLED)
        compose.setContent {
            MaterialTheme {
                val primaryActionFocusRequester = remember { FocusRequester() }
                val discoveryActionFocusRequester = remember { FocusRequester() }
                Column {
                    GameDetailsHero(
                        game = game,
                        detail = GameDetailPresentation.from(game),
                        compact = false,
                        authorizedFixture = false,
                        workerActive = false,
                        hasRemoteSource = false,
                        primaryActionFocusRequester = primaryActionFocusRequester,
                        discoveryActionFocusRequester = discoveryActionFocusRequester,
                        onPlay = {},
                        onInstallAuthorized = {},
                        onInstallRemote = {},
                    )
                    GameDetailsDiscoveryCard(
                        compact = false,
                        onOpenStore = { storeOpens++ },
                        focusRequester = discoveryActionFocusRequester,
                    )
                }
            }
        }

        compose.onNodeWithText("Play Now").performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithText("Download").assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, storeOpens) }
    }

    @Test
    fun gameTitleIsExposedAsScreenReaderHeading() {
        val game = testGame(InstallState.INSTALLED)
        compose.setContent {
            MaterialTheme {
                val primaryActionFocusRequester = remember { FocusRequester() }
                GameDetailsHero(
                    game = game,
                    detail = GameDetailPresentation.from(game),
                    compact = false,
                    authorizedFixture = false,
                    workerActive = false,
                    hasRemoteSource = false,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    onPlay = {},
                    onInstallAuthorized = {},
                    onInstallRemote = {},
                )
            }
        }

        compose.onNodeWithText("Hero Test").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit),
        )
    }

    @Test
    fun wideHeroPrimaryActionTextIsReadableAtTvScale() {
        val game = testGame(InstallState.INSTALLED)
        compose.setContent {
            MaterialTheme {
                val primaryActionFocusRequester = remember { FocusRequester() }
                GameDetailsHero(
                    game = game,
                    detail = GameDetailPresentation.from(game),
                    compact = false,
                    authorizedFixture = false,
                    workerActive = false,
                    hasRemoteSource = false,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    onPlay = {},
                    onInstallAuthorized = {},
                    onInstallRemote = {},
                )
            }
        }

        compose.onNodeWithText("Play Now", useUnmergedTree = true).assertHeightIsAtLeast(16.dp)
    }

    @Test
    fun missingReleaseYearDoesNotRenderUnknownPlaceholder() {
        val game = testGame(InstallState.INSTALLED).copy(year = 0)
        compose.setContent {
            MaterialTheme {
                val primaryActionFocusRequester = remember { FocusRequester() }
                GameDetailsHero(
                    game = game,
                    detail = GameDetailPresentation.from(game),
                    compact = false,
                    authorizedFixture = false,
                    workerActive = false,
                    hasRemoteSource = false,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    onPlay = {},
                    onInstallAuthorized = {},
                    onInstallRemote = {},
                )
            }
        }

        compose.onNodeWithText("Unknown").assertDoesNotExist()
        compose.onNodeWithText("1 player").assertIsDisplayed()
        compose.onAllNodesWithText("512 MB").assertCountEquals(2)
    }

    @Test
    fun gamepadAActivatesFocusedPlayActionOnce() {
        var plays = 0
        val game = testGame(InstallState.INSTALLED)
        compose.setContent {
            MaterialTheme {
                val primaryActionFocusRequester = remember { FocusRequester() }
                GameDetailsHero(
                    game = game,
                    detail = GameDetailPresentation.from(game),
                    compact = false,
                    authorizedFixture = false,
                    workerActive = false,
                    hasRemoteSource = false,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    onPlay = { plays++ },
                    onInstallAuthorized = {},
                    onInstallRemote = {},
                )
            }
        }

        compose.onNodeWithText("Play Now").performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, plays) }
    }

    @Test
    fun authorizedFixtureShowsInstallingWhileWorkerIsActive() {
        val game = testGame(InstallState.NOT_INSTALLED)
        compose.setContent {
            MaterialTheme {
                val primaryActionFocusRequester = remember { FocusRequester() }
                GameDetailsHero(
                    game = game,
                    detail = GameDetailPresentation.from(game),
                    compact = false,
                    authorizedFixture = true,
                    workerActive = true,
                    hasRemoteSource = false,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    onPlay = {},
                    onInstallAuthorized = {},
                    onInstallRemote = {},
                )
            }
        }

        compose.onNodeWithText("Installing…").assertIsDisplayed()
    }

    private fun testGame(state: InstallState) = Game(
        id = GameId("hero-test"),
        title = "Hero Test",
        platform = "PSP",
        year = 2024,
        genre = "Action",
        sizeMb = 512,
        state = state,
    )
}
