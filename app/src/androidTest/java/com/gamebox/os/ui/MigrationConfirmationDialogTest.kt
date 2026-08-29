package com.gamebox.os.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.storage.ContentMigrationItem
import com.gamebox.os.storage.ContentMigrationPlan
import com.gamebox.os.storage.ExternalStorageState
import com.gamebox.os.storage.ExternalStorageStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationConfirmationDialogTest {
    @get:Rule val composeRule = createComposeRule()
    private val plan = ContentMigrationPlan(listOf(ContentMigrationItem("game-1", "save.bin", 42)), 42)

    @Test fun writableStorageShowsExplicitCopyActionAndSemantics() {
        composeRule.setContent { MigrationConfirmationDialog(plan, ExternalStorageStatus(ExternalStorageState.AVAILABLE_READ_WRITE, "SSD", "ok"), onConfirm = {}, onRetry = {}, onDismiss = {}) }
        composeRule.onNodeWithText("Copy 42 bytes").assertExists()
        composeRule.onNodeWithContentDescription("Confirm migration").assertExists()
        composeRule.onNodeWithText("Nothing will be deleted from this phone.").assertExists()
    }

    @Test fun disconnectedStorageShowsReconnectAction() {
        composeRule.setContent { MigrationConfirmationDialog(plan, ExternalStorageStatus(ExternalStorageState.DISCONNECTED, message = "gone"), onConfirm = {}, onRetry = {}, onDismiss = {}) }
        composeRule.onNodeWithText("Reconnect and retry").assertExists()
    }
}
