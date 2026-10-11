package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

        compose.onNodeWithText("Forget")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.onNodeWithText("Forget missing game files?").assertIsDisplayed()
        compose.onNodeWithText("Forget file references")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus))
        compose.onNodeWithText("Cancel")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
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

    @Test fun compactReimportPickerIsSpokenClickableAndFocusable() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        compose.setContent { MaterialTheme {
            Box(Modifier.width(560.dp)) {
                ImportedGameReimportCard(
                    retained.copy(state = InstallState.MISSING_FILES),
                    app.container.authorizedRomImporter,
                    app.container.gameRepository,
                    enabled = true,
                )
            }
        } }

        compose.onNodeWithContentDescription("Locate files")
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus))
    }
}

