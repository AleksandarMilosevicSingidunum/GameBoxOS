package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType
import com.gamebox.os.ui.theme.GameBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfiguredDiscoverySourcesSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun configuredSourceActionsAndResultsRemainAccessibleAndActivateCorrectCallbacks() {
        val sources = listOf(
            source("vimm", "Vimm's Lair", GameSourceProviderType.VIMM_LAIR),
            source("json-feed", "Homebrew Index", GameSourceProviderType.GAMEBOX_JSON),
            source("web-source", "Publisher", GameSourceProviderType.EXTERNAL_WEB),
        )
        val result = DiscoverySourceGame(
            sourceId = "vimm",
            externalId = "game-1",
            title = "Galaxy Patrol",
            platform = "PSP",
        )
        val openedSources = mutableListOf<String>()
        var openedResults = 0

        compose.setContent {
            GameBoxTheme {
                ConfiguredDiscoverySourcesSection(
                    configuredSources = sources,
                    query = "Galaxy Patrol",
                    consoleLabel = "PSP",
                    syncing = false,
                    sourceMessage = "Found 1 result from Vimm's Lair",
                    results = listOf(result),
                    allDiscoveryGames = emptyList(),
                    onOpenSource = { openedSources += it.id },
                    onOpenResult = { openedResults++ },
                )
            }
        }

        compose.onNodeWithContentDescription("Search Vimm's Lair configured discovery source")
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithContentDescription(
            "Configured source status: Found 1 result from Vimm's Lair"
        ).assertIsDisplayed()
        compose.onNodeWithContentDescription("Sync Homebrew Index configured metadata source")
            .performClick()
        compose.onNodeWithContentDescription("Open Publisher configured web source")
            .performClick()
        compose.onNodeWithContentDescription("Galaxy Patrol, PSP, external discovery result")
            .assertHasClickAction()
            .performClick()

        compose.runOnIdle {
            assertEquals(listOf("vimm", "json-feed", "web-source"), openedSources)
            assertEquals(1, openedResults)
        }
    }

    @Test
    fun vimmSourceActionCanReceiveControllerFocus() {
        var openedSource: String? = null
        compose.setContent {
            GameBoxTheme {
                ConfiguredDiscoverySourcesSection(
                    configuredSources = listOf(
                        source("vimm", "Vimm's Lair", GameSourceProviderType.VIMM_LAIR),
                    ),
                    query = "God of War",
                    consoleLabel = "PSP",
                    syncing = false,
                    results = emptyList(),
                    allDiscoveryGames = emptyList(),
                    onOpenSource = { openedSource = it.id },
                    onOpenResult = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Search Vimm's Lair configured discovery source")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithContentDescription("Search Vimm's Lair configured discovery source")
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals("vimm", openedSource) }
    }

    @Test
    fun dpadDownFromVimmSourceFocusesFirstResult() {
        val results = listOf(
            DiscoverySourceGame("vimm", "first", "First title", "PSP"),
            DiscoverySourceGame("vimm", "second", "Second title", "PSP"),
        )
        compose.setContent {
            GameBoxTheme {
                ConfiguredDiscoverySourcesSection(
                    configuredSources = listOf(
                        source("vimm", "Vimm's Lair", GameSourceProviderType.VIMM_LAIR),
                    ),
                    query = "title",
                    consoleLabel = "PSP",
                    syncing = false,
                    results = results,
                    allDiscoveryGames = emptyList(),
                    onOpenSource = {},
                    onOpenResult = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Search Vimm's Lair configured discovery source")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithContentDescription("First title, PSP, external discovery result")
            .assertIsFocused()
    }

    @Test
    fun externalDiscoveryLinkUsesGamepadActivation() {
        var opened = 0
        compose.setContent {
            GameBoxTheme {
                DiscoveryExternalLinkButton("Legal homebrew library") { opened++ }
            }
        }

        compose.onNodeWithText("Legal homebrew library")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun typoCorrectionSuggestionIsVisibleAndUpdatesSearchText() {
        var updatedQuery: String? = null
        compose.setContent {
            GameBoxTheme {
                ConfiguredDiscoverySourcesSection(
                    configuredSources = listOf(
                        source("vimm", "Vimm's Lair", GameSourceProviderType.VIMM_LAIR),
                    ),
                    query = "God of Wra",
                    consoleLabel = null,
                    syncing = false,
                    results = listOf(
                        DiscoverySourceGame(
                            sourceId = "vimm",
                            externalId = "23719",
                            title = "God of War: Chains of Olympus",
                            platform = "PSP",
                        ),
                    ),
                    allDiscoveryGames = emptyList(),
                    onOpenSource = {},
                    onOpenResult = {},
                    onUseSearchSuggestion = { updatedQuery = it },
                )
            }
        }

        compose.onNodeWithText("Did you mean “God of War”?", substring = true)
            .assertHasClickAction()
            .performClick()
        compose.runOnIdle { assertEquals("God of War", updatedQuery) }
    }

    @Test
    fun sourceActionsAreDisabledWhileConfiguredMetadataIsSyncing() {
        compose.setContent {
            GameBoxTheme {
                ConfiguredDiscoverySourcesSection(
                    configuredSources = listOf(
                        source("json-feed", "Homebrew Index", GameSourceProviderType.GAMEBOX_JSON),
                    ),
                    query = "",
                    consoleLabel = null,
                    syncing = true,
                    results = emptyList(),
                    allDiscoveryGames = emptyList(),
                    onOpenSource = {},
                    onOpenResult = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Sync Homebrew Index configured metadata source")
            .assertIsNotEnabled()
    }

    private fun source(id: String, name: String, type: GameSourceProviderType) =
        GameSourceConfig(
            id = id,
            name = name,
            type = type,
            baseUrl = if (type == GameSourceProviderType.VIMM_LAIR) "https://vimm.net" else "https://example.org",
        )
}
