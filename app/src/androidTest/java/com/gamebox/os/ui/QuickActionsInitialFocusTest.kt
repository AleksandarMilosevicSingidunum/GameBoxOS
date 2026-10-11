package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickActionsInitialFocusTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun wideQuickActionsDialogStartsOnDownloads() {
        compose.setContent {
            MaterialTheme {
                QuickActionsDialog(
                    destination = Destination.HOME,
                    xLabel = "Search",
                    yLabel = "Switch Profile",
                    onX = {},
                    onY = {},
                    onHome = {},
                    onDownloads = {},
                    onSettings = {},
                    onDismiss = {},
                )
            }
        }

        compose.waitForIdle()
        compose.onNodeWithContentDescription("Downloads").assertIsFocused()
    }

    @Test
    fun AActivatesDownloadsWithoutNavigatingPastOtherActions() {
        var downloadsOpened = false
        compose.setContent {
            MaterialTheme {
                QuickActionsDialog(
                    destination = Destination.HOME,
                    xLabel = "Search",
                    yLabel = "Switch Profile",
                    onX = {},
                    onY = {},
                    onHome = {},
                    onDownloads = { downloadsOpened = true },
                    onSettings = {},
                    onDismiss = {},
                )
            }
        }

        compose.waitForIdle()
        compose.onNodeWithContentDescription("Downloads").assertIsFocused()
        compose.onRoot().performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)
        }
        assert(downloadsOpened)
    }

    @Test
    fun dpadMovesFromDownloadsToHomeAndBack() {
        compose.setContent {
            MaterialTheme {
                QuickActionsDialog(
                    destination = Destination.HOME,
                    xLabel = "Search",
                    yLabel = "Switch Profile",
                    onX = {},
                    onY = {},
                    onHome = {},
                    onDownloads = {},
                    onSettings = {},
                    onDismiss = {},
                )
            }
        }

        compose.waitForIdle()
        compose.onNodeWithContentDescription("Downloads").assertIsFocused()
        compose.onRoot().performKeyInput { keyDown(Key.DirectionDown); keyUp(Key.DirectionDown) }
        compose.onNodeWithContentDescription("Home").assertIsFocused()
        compose.onRoot().performKeyInput { keyDown(Key.DirectionUp); keyUp(Key.DirectionUp) }
        compose.onNodeWithContentDescription("Downloads").assertIsFocused()
    }
}
