package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportedGameReimportCardTest {
    @get:Rule val compose = createComposeRule()
    private val retained = Game(GameId("restore-ui"), "Restore UI", "PSP", 2000,
        "Test", 1, InstallState.NOT_INSTALLED, localContentRelativePath = "restore-ui/game.iso")

    @Test fun restoreTracksInstallationStateWithoutLosingTheGameEntry() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val game = mutableStateOf(retained)
        compose.setContent { MaterialTheme {
            ImportedGameReimportCard(game.value, app.container.authorizedRomImporter,
                app.container.gameRepository, enabled = true)
        } }
        compose.onNodeWithText("Locate files").assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle { game.value = retained.copy(state = InstallState.INSTALLED) }
        compose.onNodeWithText("Locate files").assertDoesNotExist()
        compose.runOnIdle { game.value = retained.copy(state = InstallState.MISSING_FILES) }
        compose.onNodeWithText("Locate files").assertIsEnabled()
        compose.runOnIdle { game.value = retained.copy(localContentRelativePath = null) }
        compose.onNodeWithText("Repair missing game files").assertDoesNotExist()
    }

    @Test fun forgetRequiresExplicitSaveSafeConfirmation() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        compose.setContent { MaterialTheme {
            ImportedGameReimportCard(retained.copy(state = InstallState.MISSING_FILES),
                app.container.authorizedRomImporter, app.container.gameRepository, enabled = true)
        } }

        compose.onNodeWithText("Forget").assertIsDisplayed().performClick()
        compose.onNodeWithText("Forget missing game files?").assertIsDisplayed()
        compose.onNodeWithText("Forget file references").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Forget missing game files?").assertDoesNotExist()
    }

    @Test fun launchOrRecoveryGateDisablesPickerUntilSafe() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val enabled = mutableStateOf(false)
        compose.setContent { MaterialTheme {
            ImportedGameReimportCard(retained, app.container.authorizedRomImporter,
                app.container.gameRepository, enabled = enabled.value)
        } }
        compose.onNodeWithText("Locate files").assertIsNotEnabled()
        compose.runOnIdle { enabled.value = true }
        compose.onNodeWithText("Locate files").assertIsEnabled()
    }
}

