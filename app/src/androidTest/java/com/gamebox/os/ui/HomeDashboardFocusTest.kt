package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.unit.dp
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeDashboardFocusTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun storageUsageProgressExposesItsCurrentValueToAccessibility() {
        compose.setContent {
            MaterialTheme {
                HomeStatusItem(
                    icon = Icons.Rounded.Storage,
                    label = "Storage",
                    value = "4% used",
                    progress = 0.04f,
                )
            }
        }

        compose.onNodeWithContentDescription("Storage usage: 4% used").assertIsDisplayed()
    }

    @Test
    fun emptyHomeExplainsOwnedCopyImportAndOpensVimmFromTvAction() {
        val setupFocusRequester = FocusRequester()
        var openedStore = false
        compose.setContent {
            MaterialTheme {
                HomeDashboardContent(
                    games = emptyList(),
                    presentation = null,
                    compact = false,
                    showDefaultHomeSetupBanner = false,
                    networkLabel = "Internet connected",
                    storagePercent = 4,
                    deviceModel = "HONOR PGT-N19",
                    controllerLabel = "Not connected",
                    onChooseHome = {},
                    emptyHomeSetupFocusRequester = setupFocusRequester,
                    openPc = {},
                    openStore = { openedStore = true },
                    onFocused = {},
                    open = {},
                )
            }
        }

        compose.onNodeWithText("Welcome to GameBox").assertExists()
        compose.onNodeWithText(
            "Open Vimm Lair in GameBox to find a title and start a tracked download. GameBox verifies and imports supported downloads automatically; use manual import for files you already have."
        )
            .assertExists()
        compose.onNodeWithText(
            "Browse console rows and Vimm Lair. A listing is discovery information, not an install file; supported source links can start a tracked download."
        ).assertExists()
        compose.onNodeWithText(
            "Pick where GameBox keeps your imported library, organized by console."
        ).assertExists()
        compose.onNodeWithText(
            "Connect a Bluetooth gamepad for comfortable couch navigation and play."
        ).assertExists()
        compose.onNodeWithText("Not connected").assertExists()
        compose.onNodeWithContentDescription("Open Vimm Lair in GameBox")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertTrue(openedStore) }
    }

    @Test
    fun emptyHomeSettingsActionOpensStorageSetup() {
        var openedStorageSettings = 0
        var openedControllerSettings = 0
        val setupFocusRequester = FocusRequester()
        compose.setContent {
            MaterialTheme {
                HomeDashboardContent(
                    games = emptyList(),
                    presentation = null,
                    compact = false,
                    showDefaultHomeSetupBanner = false,
                    networkLabel = "Internet connected",
                    storagePercent = 4,
                    deviceModel = "HONOR PGT-N19",
                    controllerLabel = "Not connected",
                    onChooseHome = {},
                    emptyHomeSetupFocusRequester = setupFocusRequester,
                    openPc = {},
                    openStore = {},
                    openStorageSettings = { openedStorageSettings++ },
                    openControllerSettings = { openedControllerSettings++ },
                    onFocused = {},
                    open = {},
                )
            }
        }

        compose.onNodeWithText("Choose folder")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.onNodeWithText("Controller settings")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle {
            assertTrue(openedStorageSettings == 1)
            assertTrue(openedControllerSettings == 1)
        }
    }

    @Test
    fun wideHomeRestoresToVisibleHeroWhenRememberedCardIsNotRendered() {
        val recent = game("recent", lastPlayed = "2026-10-01T10:00:00Z")
        val older = (1..9).map { game("older-$it") }
        val hidden = older.last()
        val games = listOf(recent) + older
        val presentation = requireNotNull(homePresentationState(games, hidden.id, compact = false))
        var openedStore = false
        var openedSettings = 0

        compose.setContent {
            val emptyHomeSetupFocusRequester = remember { FocusRequester() }
            MaterialTheme {
                HomeDashboardContent(
                    games = games,
                    presentation = presentation,
                    compact = false,
                    showDefaultHomeSetupBanner = false,
                    networkLabel = "Offline",
                    storagePercent = null,
                    deviceModel = "Test device",
                    controllerLabel = "Not connected",
                    onChooseHome = {},
                    emptyHomeSetupFocusRequester = emptyHomeSetupFocusRequester,
                    openPc = {},
                    openStore = { openedStore = true },
                    openControllerSettings = { openedSettings++ },
                    onFocused = {},
                    open = {},
                )
            }
        }

        compose.onNodeWithContentDescription(GameBoxSemantics.gameCardDescription(recent, hero = true))
            .assertIsFocused()
        compose.onAllNodesWithContentDescription(GameBoxSemantics.gameCardDescription(hidden, hero = false))
            .assertCountEquals(0)
        compose.onNodeWithContentDescription("Open Vimm Lair in GameBox")
            .assertExists()
            .performClick()
        assertTrue(openedStore)
        compose.onNodeWithContentDescription("Controller: Not connected. Open Settings to configure.")
            .assertExists()
            .performClick()
        assertTrue(openedSettings == 1)
    }

    @Test
    fun tvDpadDownFromHeroMovesIntoCollectionBeforeQuickLaunch() {
        val hero = game("recent", lastPlayed = "2026-10-01T10:00:00Z")
        val games = listOf(hero, game("second"))
        val presentation = requireNotNull(homePresentationState(games, hero.id, compact = false))
        compose.setContent {
            val setupFocusRequester = remember { FocusRequester() }
            MaterialTheme {
                HomeDashboardContent(
                    games = games,
                    presentation = presentation,
                    compact = false,
                    showDefaultHomeSetupBanner = false,
                    networkLabel = "Internet connected",
                    storagePercent = 4,
                    deviceModel = "HONOR PGT-N19",
                    controllerLabel = "Not connected",
                    onChooseHome = {},
                    emptyHomeSetupFocusRequester = setupFocusRequester,
                    openPc = {},
                    openStore = {},
                    onFocused = {},
                    open = {},
                )
            }
        }

        compose.onNodeWithContentDescription(GameBoxSemantics.gameCardDescription(hero, hero = true))
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithContentDescription(GameBoxSemantics.gameCardDescription(hero, hero = false))
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithContentDescription(GameBoxSemantics.gameCardDescription(games[1], hero = false))
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP)
        compose.onNodeWithContentDescription(GameBoxSemantics.gameCardDescription(hero, hero = false))
            .assertIsFocused()
    }

    @Test
    fun wideHomeKeepsRecentlyPlayedCardInsideTvContentViewport() {
        val recent = game("recent", lastPlayed = "2026-10-01T10:00:00Z")
        val games = listOf(recent) + (1..8).map { game("older-$it") }
        val presentation = requireNotNull(homePresentationState(games, recent.id, compact = false))
        compose.setContent {
            val setupFocusRequester = remember { FocusRequester() }
            MaterialTheme {
                Box(Modifier.width(1280.dp).height(580.dp).testTag("home-tv-content-viewport")) {
                    HomeDashboardContent(
                        games = games,
                        presentation = presentation,
                        compact = false,
                        showDefaultHomeSetupBanner = false,
                        networkLabel = "Internet connected",
                        storagePercent = 4,
                        deviceModel = "HONOR PGT-N19",
                        controllerLabel = "Not connected",
                        onChooseHome = {},
                        emptyHomeSetupFocusRequester = setupFocusRequester,
                        openPc = {},
                        openStore = {},
                        onFocused = {},
                        open = {},
                    )
                }
            }
        }

        val viewport = compose.onNodeWithTag("home-tv-content-viewport")
            .fetchSemanticsNode().boundsInRoot
        val recentCard = compose.onNodeWithContentDescription(
            GameBoxSemantics.gameCardDescription(recent, hero = false),
        ).fetchSemanticsNode().boundsInRoot
        assertTrue("Recently played must not be clipped behind the TV controller footer", recentCard.bottom <= viewport.bottom)
    }

    @Test
    fun tvHeroKeepsPrimaryActionInsideBlueprintCardBounds() {
        val hero = game("hero").copy(
            title = "A Long Console Game Title for Living Room Displays",
            description = "A deliberately long description that wraps to two lines on the TV hero card.",
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(1280.dp).height(212.dp)) {
                    GameCard(
                        game = hero,
                        modifier = Modifier.fillMaxSize(),
                        hero = true,
                        onClick = {},
                    )
                }
            }
        }

        val cardBounds = compose.onNodeWithContentDescription(
            GameBoxSemantics.gameCardDescription(hero, hero = true),
        ).fetchSemanticsNode().boundsInRoot
        val actionBounds = compose.onNodeWithText("View game", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Hero action must not be clipped by the fixed TV card height", actionBounds.top >= cardBounds.top)
        assertTrue("Hero action must fit inside the TV card", actionBounds.bottom <= cardBounds.bottom)
    }

    private fun game(id: String, lastPlayed: String? = null) = Game(
        id = GameId(id),
        title = id,
        platform = "Retro",
        year = 2026,
        genre = "Arcade",
        sizeMb = 1,
        state = InstallState.INSTALLED,
        lastPlayed = lastPlayed,
    )
}
