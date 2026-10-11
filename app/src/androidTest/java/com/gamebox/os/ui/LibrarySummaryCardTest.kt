package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.summarizeLibrary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibrarySummaryCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun summarizesLibraryAndHighlightsMostRecentlyPlayedGame() {
        val games = listOf(
            Game(
                id = GameId("library-one"), title = "Library One", platform = "PSP",
                year = 2006, genre = "Action", sizeMb = 1200, state = InstallState.INSTALLED,
                lastPlayed = "2026-09-01T10:00:00Z", minutesPlayed = 125, favorite = true,
            ),
            Game(
                id = GameId("library-two"), title = "Library Two", platform = "PSP",
                year = 2007, genre = "Adventure", sizeMb = 900, state = InstallState.QUEUED,
                lastPlayed = "2026-09-02T10:00:00Z", minutesPlayed = 35,
            ),
        )

        compose.setContent {
            MaterialTheme { LibrarySummaryCard(summarizeLibrary(games)) }
        }

        compose.onNodeWithContentDescription("2 games, 1 installed, 1 favorite, 2 hours played")
            .assertIsDisplayed()
        compose.onNodeWithText("2h 40m total playtime").assertIsDisplayed()
        compose.onNodeWithText("Ready to resume: Library One").assertIsDisplayed()
        compose.onNodeWithText("Ready to resume: Library Two").assertDoesNotExist()
    }

    @Test
    fun compactCollectionIncludesImportableGamesWithoutCallingThemInstalled() {
        val importableGame = Game(
            id = GameId("importable-game"), title = "Importable Game", platform = "PSP",
            year = 2006, genre = "Adventure", sizeMb = 700, state = InstallState.NOT_INSTALLED,
        )

        compose.setContent {
            MaterialTheme {
                CollectionScreen(
                    title = "Library",
                    subtitle = "Your games",
                    games = listOf(importableGame),
                    restoreGameId = null,
                    onFocused = {},
                    compact = true,
                    uiState = rememberGameBoxUiState(),
                    onStore = {},
                    open = {},
                )
            }
        }

        compose.onNodeWithText("All games").assertIsDisplayed()
        compose.onNodeWithText("Installed games").assertDoesNotExist()
        compose.onNodeWithText("Importable Game").assertIsDisplayed()
    }

    @Test
    fun dpadMovesFocusFromAllToTheFirstPlatformInWideLibrary() {
        lateinit var inputModeManager: InputModeManager
        val games = listOf(
            Game(
                id = GameId("ps2-game"), title = "PS2 Game", platform = "PS2",
                year = 2003, genre = "Action", sizeMb = 1, state = InstallState.NOT_INSTALLED,
            ),
            Game(
                id = GameId("psp-game"), title = "PSP Game", platform = "PSP",
                year = 2006, genre = "Adventure", sizeMb = 1, state = InstallState.NOT_INSTALLED,
            ),
        )
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                CollectionScreen(
                    title = "Library",
                    subtitle = "Your collection",
                    games = games,
                    restoreGameId = null,
                    onFocused = {},
                    compact = false,
                    uiState = rememberGameBoxUiState(),
                    onStore = {},
                    open = {},
                )
            }
        }

        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithText("All")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithText("PS2").assertIsFocused()
    }

    @Test
    fun wideLibraryDoesNotShowEmptyResumeShelfForMissingGameFiles() {
        val missingGameWithSave = Game(
            id = GameId("missing-save"), title = "Missing Game", platform = "PSP",
            year = 2006, genre = "Adventure", sizeMb = 700, state = InstallState.MISSING_FILES,
            lastPlayed = "2026-09-02T10:00:00Z", savePresent = true,
        )

        compose.setContent {
            MaterialTheme {
                CollectionScreen(
                    title = "Library",
                    subtitle = "Your collection",
                    games = listOf(missingGameWithSave),
                    restoreGameId = null,
                    onFocused = {},
                    compact = false,
                    uiState = rememberGameBoxUiState(),
                    onStore = {},
                    open = {},
                )
            }
        }

        compose.onNodeWithText("Recently Played").assertIsDisplayed()
        compose.onNodeWithText("Ready to Resume").assertDoesNotExist()
    }
}
