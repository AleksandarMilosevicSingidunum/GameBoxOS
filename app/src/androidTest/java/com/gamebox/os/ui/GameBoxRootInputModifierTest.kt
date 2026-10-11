package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameBoxRootInputModifierTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun modifierDispatchesControllerCommandsThroughShellCallbacks() {
        val actions = ControllerActionRegistry()
        var xInvocations = 0
        var yInvocations = 0
        actions.configure("Test X", { xInvocations++ }, "Test Y", { yInvocations++ })
        val movedTabs = mutableListOf<Int>()
        var quickActionsOpened = 0
        var systemStatusOpened = 0
        var backHandled = 0

        rule.setContent {
            MaterialTheme {
                Column(
                    Modifier.fillMaxSize().onGameBoxRootInput(
                        soundsEnabled = false,
                        playSoundEffect = {},
                        controllerActions = actions,
                        movePrimaryTab = { offset -> movedTabs.add(offset) },
                        openQuickActions = { quickActionsOpened++ },
                        openSystemStatus = { systemStatusOpened++ },
                        fallbackX = { error("Registered X action should run") },
                        fallbackY = { error("Registered Y action should run") },
                        handleBack = { backHandled++; true },
                    ),
                ) {
                    Text("Input target", Modifier.focusable())
                }
            }
        }
        rule.onNodeWithText("Input target")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        listOf(
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_B,
        ).forEach { keyCode -> instrumentation.sendKeyDownUpSync(keyCode) }

        rule.runOnIdle {
            assertEquals(listOf(1, -1), movedTabs)
            assertEquals(1, quickActionsOpened)
            assertEquals(1, systemStatusOpened)
            assertEquals(1, xInvocations)
            assertEquals(1, yInvocations)
            assertEquals(1, backHandled)
        }
    }
}
