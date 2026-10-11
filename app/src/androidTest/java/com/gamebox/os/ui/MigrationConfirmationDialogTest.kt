package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.storage.ContentMigrationItem
import com.gamebox.os.storage.ContentMigrationPlan
import com.gamebox.os.storage.ExternalStorageState
import com.gamebox.os.storage.ExternalStorageStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class MigrationConfirmationDialogTest {
    @get:Rule val composeRule = createComposeRule()
    private val plan = ContentMigrationPlan(listOf(ContentMigrationItem("game-1", "save.bin", 42)), 42)

    @Test fun writableStorageShowsExplicitCopyActionAndSemantics() {
        composeRule.setContent { MigrationConfirmationDialog(plan, ExternalStorageStatus(ExternalStorageState.AVAILABLE_READ_WRITE, "SSD", "ok"), onConfirm = {}, onRetry = {}, onDismiss = {}) }
        composeRule.onNodeWithText("Copy 42 bytes").assertExists()
        composeRule.onNodeWithContentDescription("Confirm migration").assertExists()
        composeRule.onNodeWithText(
            "Each phone copy is removed only after its external copy is verified. Game records and saves are kept."
        ).assertExists()
    }

    @Test fun disconnectedStorageShowsReconnectAction() {
        composeRule.setContent { MigrationConfirmationDialog(plan, ExternalStorageStatus(ExternalStorageState.DISCONNECTED, message = "gone"), onConfirm = {}, onRetry = {}, onDismiss = {}) }
        composeRule.onNodeWithText("Reconnect and retry").assertExists()
    }

    @Test fun controllerAConfirmsMigrationOnce() {
        var confirmations = 0
        composeRule.setContent {
            MigrationConfirmationDialog(
                plan,
                ExternalStorageStatus(ExternalStorageState.AVAILABLE_READ_WRITE, "SSD", "ok"),
                onConfirm = { confirmations++ },
                onRetry = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithContentDescription("Confirm migration")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        composeRule.runOnIdle { assertEquals(1, confirmations) }
    }
}

