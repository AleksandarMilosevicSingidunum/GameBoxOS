package com.gamebox.os.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.LibrarySummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryOverviewSidebarTest {
    @get:Rule
    val compose = createComposeRule()

    private val summary = LibrarySummary(
        totalGames = 1,
        installedGames = 1,
        favorites = 0,
        totalMinutesPlayed = 0,
        resumeGame = null,
    )

    @Test
    fun hidesSavePanelUntilSaveDataExists() {
        compose.setContent {
            MaterialTheme {
                LibraryOverviewSidebar(
                    modifier = Modifier.width(220.dp),
                    summary = summary,
                    totalStorageBytes = 100L,
                    freeStorageBytes = 50L,
                    gamesWithSaves = emptyList(),
                )
            }
        }

        compose.onNodeWithText("Library Overview").assertIsDisplayed()
        compose.onNodeWithText("1 game playable offline").assertIsDisplayed()
        compose.onNodeWithText("Local Saves").assertDoesNotExist()
    }

    @Test
    fun showsSavePanelWhenGamesHaveDetectedSaves() {
        compose.setContent {
            MaterialTheme {
                LibraryOverviewSidebar(
                    modifier = Modifier.width(220.dp),
                    summary = summary,
                    totalStorageBytes = 100L,
                    freeStorageBytes = 50L,
                    gamesWithSaves = listOf(
                        Game(
                            id = GameId("save-panel-test"),
                            title = "Save Panel Test",
                            platform = "Retro",
                            year = 2026,
                            genre = "Test",
                            sizeMb = 1,
                            state = InstallState.INSTALLED,
                            savePresent = true,
                            saveSizeBytes = 12L,
                        ),
                    ),
                )
            }
        }

        compose.onNodeWithText("Local Saves").assertIsDisplayed()
        compose.onNodeWithText("1 game with saves").assertIsDisplayed()
    }
}
