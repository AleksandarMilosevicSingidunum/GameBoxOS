package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogConsoleFilterRailTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun consoleFavoritesAndClearActionsRouteToStoreState() {
        val selectedConsoles = mutableListOf<String?>()
        val favorites = mutableListOf<Boolean>()
        var clearCount = 0
        var sortCount = 0

        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(230.dp).height(640.dp)) {
                    CatalogConsoleFilterRail(
                        state = CatalogConsoleFilterState(
                            discoveryGameCount = 3,
                            authorizedGameCount = 2,
                            consoleCounts = storeConsoles.associate { it.key to 1 },
                            selectedConsole = null,
                            favoritesOnly = false,
                            authorizedGames = emptyList(),
                            genre = null,
                            region = null,
                            language = null,
                            discoveryRegions = emptyList(),
                            sortByRating = false,
                        ),
                        actions = CatalogConsoleFilterActions(
                            onSelectConsole = { selectedConsoles += it?.key },
                            onFavoritesOnly = { favorites += it },
                            onGenre = {},
                            onRegion = {},
                            onLanguage = {},
                            onToggleSort = { sortCount++ },
                            onClearFilters = { clearCount++ },
                        ),
                        initialFocusRequester = null,
                        rightFocusRequester = null,
                    )
                }
            }
        }

        compose.onNodeWithText("All").performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        compose.onNodeWithText("All", useUnmergedTree = true).assertHeightIsAtLeast(15.dp)
        compose.onNodeWithText("CONSOLES").assertHeightIsAtLeast(14.dp)
        compose.onNodeWithText("GBA").assertIsDisplayed()
        compose.onNodeWithContentDescription("Store console filter: Game Boy Advance, 1 title").assertExists()
        compose.onNodeWithContentDescription("Store console filter: Game Boy Color, 1 title").assertExists()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithText("PS2").assertIsFocused()

        compose.onNodeWithText("PS2").performClick()
        compose.onNodeWithText("GameCube").performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        compose.onNodeWithText("Favorites").performScrollTo().performClick()
        compose.onNodeWithText("Clear filters").performScrollTo().performClick()
        compose.onNodeWithText("Sort: Title A–Z").performScrollTo()
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.onNodeWithText("Browse your saved catalog offline").performScrollTo().assertIsDisplayed()

        compose.runOnIdle {
            assertEquals(listOf("ps2"), selectedConsoles)
            assertEquals(listOf(true), favorites)
            assertTrue(clearCount == 1)
            assertEquals(1, sortCount)
        }
    }
}
