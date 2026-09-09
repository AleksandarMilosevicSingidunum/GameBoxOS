package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.domain.*
import com.gamebox.os.storage.SaveSafetyController
import com.gamebox.os.storage.SaveSafetyState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerGameSaveCardTest {
    @get:Rule val compose = createComposeRule()
    private val game = Game(GameId("save-ui"), "Save UI", "PSP", 2000, "Test", 1, InstallState.INSTALLED)

    @Test fun runningSaveOperationDisablesActionsUntilCompletion() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val state = MutableStateFlow(SaveSafetyState(saveRecordPresent = true, backupPresent = true))
        val busy = MutableStateFlow(true)
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun observeState() = state
            override fun observeBusy() = busy
        }
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, enabled = true) } }
        compose.onNodeWithText("Back up save copy").assertIsNotEnabled()
        compose.onNodeWithText("Restore save copy").assertIsNotEnabled()
        compose.onNodeWithText("Export save backup").assertIsNotEnabled()
        compose.onNodeWithText("Import save backup").assertIsNotEnabled()
        compose.runOnIdle { busy.value = false }
        compose.onNodeWithText("Back up save copy").assertIsEnabled()
        compose.onNodeWithText("Restore save copy").assertIsEnabled()
    }

    @Test fun restoreRequiresConfirmationAndCancelDoesNotWrite() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val state = MutableStateFlow(SaveSafetyState(saveRecordPresent = true,
            relativePath = "save-ui/save.dat", backupPresent = true))
        var restores = 0
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun observeState() = state
            override fun restoreSave() { restores++ }
        }
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, enabled = true) } }
        compose.onNodeWithText("Restore save copy").performClick()
        compose.runOnIdle { assertEquals(0, restores) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, restores) }
        compose.onNodeWithText("Restore save copy").performClick()
        compose.onNodeWithText("Replace save copy").performClick()
        compose.runOnIdle { assertEquals(1, restores) }
        compose.onNodeWithText("Replace this save copy?").assertDoesNotExist()
    }

    @Test fun unavailableBackupAndLaunchGateDisableActions() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val state = MutableStateFlow(SaveSafetyState(saveRecordPresent = true, backupPresent = false))
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun observeState() = state
        }
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, enabled = false) } }
        compose.onNodeWithText("Back up save copy").assertIsNotEnabled()
        compose.onNodeWithText("Restore save copy").assertIsNotEnabled()
        compose.onNodeWithText("Export save backup").assertIsNotEnabled()
        compose.onNodeWithText("Import save backup").assertIsNotEnabled()
        compose.runOnIdle { state.value = SaveSafetyState() }
        compose.onNodeWithText("Import save copy").assertIsNotEnabled()
    }

    @Test fun importingBackupExplainsReplacementAndCanBeCancelled() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val state = MutableStateFlow(SaveSafetyState(saveRecordPresent = true))
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun observeState() = state
        }
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, enabled = true) } }
        compose.onNodeWithText("Import save backup").performClick()
        compose.onNodeWithText("Import a backup for Save UI?").assertIsDisplayed()
        compose.onNodeWithText("Choose backup file").assertIsEnabled()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Choose backup file").assertDoesNotExist()
    }
}
