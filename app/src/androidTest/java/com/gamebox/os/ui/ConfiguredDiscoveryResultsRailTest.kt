package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.source.DiscoverySourceGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfiguredDiscoveryResultsRailTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun gamepadAActivatesTheVimmCorrectedSearchSuggestion() {
        var appliedQuery = "God of Wra"
        compose.setContent {
            MaterialTheme {
                ConfiguredDiscoveryResultsRail(
                    results = listOf(
                        DiscoverySourceGame(
                            sourceId = "vimm-lair",
                            externalId = "ps2-123",
                            title = "God of War",
                            platform = "PlayStation 2",
                        ),
                    ),
                    allDiscoveryGames = emptyList(),
                    onOpenResult = {},
                    cardWidth = 180.dp,
                    cardHeight = 250.dp,
                    query = appliedQuery,
                    onUseSearchSuggestion = { appliedQuery = it },
                )
            }
        }

        compose.onNodeWithText("Did you mean “God of War”?")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals("God of War", appliedQuery) }
    }

    @Test
    fun compactVimmCardKeepsConsoleEditionAndActionHintVisible() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(156.dp).height(132.dp)) {
                    ConfiguredSourceResultCard(
                        game = DiscoverySourceGame(
                            sourceId = "vimm-lair",
                            externalId = "gba-123",
                            title = "Mario Kart: Super Circuit",
                            platform = "Game Boy Advance",
                            region = "USA",
                            year = 2001,
                        ),
                        modifier = Modifier.fillMaxSize(),
                        onClick = {},
                    )
                }
            }
        }

        compose.onNodeWithText("GBA").assertIsDisplayed()
        compose.onNodeWithText("Mario Kart: Super Circuit").assertIsDisplayed()
        compose.onNodeWithText("USA · 2001").assertIsDisplayed()
        compose.onNodeWithText("MK", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Details / import").assertIsDisplayed()
    }

    @Test
    fun gamepadAActivatesFocusedVimmResultCard() {
        var opened = 0
        compose.setContent {
            MaterialTheme {
                ConfiguredDiscoveryResultsRail(
                    results = listOf(
                        DiscoverySourceGame(
                            sourceId = "vimm-lair",
                            externalId = "ps2-123",
                            title = "God of War",
                            platform = "PlayStation 2",
                        ),
                    ),
                    allDiscoveryGames = emptyList(),
                    onOpenResult = { opened++ },
                    cardWidth = 180.dp,
                    cardHeight = 250.dp,
                )
            }
        }

        compose.onNodeWithContentDescription(
            "God of War, PlayStation 2, external discovery result",
        ).performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun returnFocusRequesterTargetsThePreviouslyOpenedResult() {
        val returnFocusRequester = FocusRequester()
        compose.setContent {
            MaterialTheme {
                ConfiguredDiscoveryResultsRail(
                    results = listOf(
                        DiscoverySourceGame(
                            sourceId = "vimm-lair",
                            externalId = "gba-123",
                            title = "Celeste Classic",
                            platform = "Game Boy Advance",
                        ),
                        DiscoverySourceGame(
                            sourceId = "vimm-lair",
                            externalId = "ps2-123",
                            title = "God of War",
                            platform = "PlayStation 2",
                        ),
                    ),
                    allDiscoveryGames = emptyList(),
                    onOpenResult = {},
                    cardWidth = 180.dp,
                    cardHeight = 250.dp,
                    returnFocusRequester = returnFocusRequester,
                    returnFocusKey = "vimm-lair:ps2-123",
                )
            }
        }

        compose.runOnIdle { assertTrue(returnFocusRequester.requestFocus()) }
        compose.onNodeWithContentDescription(
            "God of War, PlayStation 2, external discovery result",
        ).assertIsFocused()
    }
}
