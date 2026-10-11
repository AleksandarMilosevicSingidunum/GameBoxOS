package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.domain.*
import com.gamebox.os.storage.SaveSafetyController
import com.gamebox.os.storage.SaveSafetyState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, compact = true, enabled = true) } }
        compose.onNodeWithText("Back up save copy").assertIsNotEnabled()
        compose.onNodeWithText("Restore save copy").assertIsNotEnabled()
        compose.onNodeWithText("Export save backup").assertIsNotEnabled()
        compose.onNodeWithText("Import save backup").assertIsNotEnabled()
        compose.onNodeWithText("Upload cloud copy").assertIsNotEnabled()
        compose.onNodeWithText("Restore cloud copy").assertIsNotEnabled()
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
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, compact = true, enabled = true) } }
        compose.onNodeWithText("Restore save copy").performClick()
        compose.runOnIdle { assertEquals(0, restores) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, restores) }
        compose.onNodeWithText("Restore save copy").performClick()
        compose.onNodeWithText("Replace save copy")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, restores) }
        compose.onNodeWithText("Replace this save copy?").assertDoesNotExist()
    }

    @Test fun unavailableBackupAndLaunchGateDisableActions() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val state = MutableStateFlow(SaveSafetyState(saveRecordPresent = true, backupPresent = false))
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun observeState() = state
        }
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, compact = true, enabled = false) } }
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
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, compact = true, enabled = true) } }
        compose.onNodeWithText("Import save backup").performClick()
        compose.onNodeWithText("Import a backup for Save UI?").assertIsDisplayed()
        compose.onNodeWithText("Choose backup file").assertIsEnabled()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Choose backup file").assertDoesNotExist()
    }

    @Test fun compactImportSaveCopyCanReceiveControllerFocus() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val state = MutableStateFlow(SaveSafetyState())
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun observeState() = state
        }
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, compact = true, enabled = true) } }

        compose.onNodeWithText("Import save copy")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus))
    }

    @Test fun everyCompactManagedSaveActionIsSpokenClickableAndFocusable() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val state = MutableStateFlow(
            SaveSafetyState(
                saveRecordPresent = true,
                relativePath = "save-ui/save.dat",
                backupPresent = true,
                sizeBytes = 12,
            )
        )
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun observeState() = state
        }
        val actions = listOf(
            "Back up save copy",
            "Import save backup",
            "Restore save copy",
            "Export save backup",
            "Upload cloud copy",
            "Restore cloud copy",
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(560.dp)) {
                    PerGameSaveCard(game, controller, compact = true, enabled = true)
                }
            }
        }

        actions.forEach { label ->
            compose.onNodeWithContentDescription(label)
                .assertHasClickAction()
                .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus))
        }
    }

    @Test fun cloudControlsUseTheSelectedGamesController() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val state = MutableStateFlow(SaveSafetyState(saveRecordPresent = true))
        var uploads = 0
        var downloads = 0
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun observeState() = state
            override fun uploadCloudSave() { uploads++ }
            override fun downloadCloudSave() { downloads++ }
        }
        compose.setContent { MaterialTheme { PerGameSaveCard(game, controller, compact = true, enabled = true) } }

        compose.onNodeWithText("Upload cloud copy")
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Restore cloud copy")
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)

        compose.runOnIdle {
            assertEquals(1, uploads)
            assertEquals(1, downloads)
        }
    }

    @Test fun wideTvSeparatesLocalAndCloudSaveToolsIntoSideBySidePanels() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val controller = app.container.saveSafetyController
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(1200.dp)) {
                    PerGameSaveCard(game, controller, compact = false, enabled = true)
                }
            }
        }

        val local = compose.onNodeWithText("Save copy").fetchSemanticsNode().boundsInRoot
        val cloud = compose.onNodeWithText("Encrypted cloud copy").fetchSemanticsNode().boundsInRoot
        assertTrue("wide TV save panels should occupy separate columns", local.right < cloud.left)
        compose.onNodeWithText("GameBox manages a separate single-file copy", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Upload cloud copy").assertIsDisplayed()
        compose.onNodeWithText("Restore cloud copy").assertIsDisplayed()
    }

}
