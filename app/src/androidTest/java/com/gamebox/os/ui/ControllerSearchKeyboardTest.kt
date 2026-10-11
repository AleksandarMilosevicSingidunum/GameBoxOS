package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class ControllerSearchKeyboardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dpadMovesAcrossKeysAndASelectsSearchTextWithoutIme() {
        var query by mutableStateOf("")
        var keyboardVisible by mutableStateOf(false)

        rule.setContent {
            MaterialTheme {
                Column {
                    ControllerSearchField(
                        query = query,
                        onOpenKeyboard = { keyboardVisible = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (keyboardVisible) {
                        ControllerSearchKeyboardDialog(
                            query = query,
                            onQueryChange = { query = it },
                            onDismiss = { keyboardVisible = false },
                        )
                    }
                }
            }
        }

        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithContentDescription("Search games. Press A to type with the controller.")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithContentDescription("Key Q").assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithContentDescription("Key W").assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT)
        rule.onNodeWithContentDescription("Key Q").assertIsFocused()
        rule.onNodeWithContentDescription("Key 0").assertIsDisplayed()
        rule.onNodeWithContentDescription("Key P").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithContentDescription("Key A").assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithContentDescription("Key Z").assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithContentDescription("Search query: Z").assertTextEquals("Z")
        rule.onNodeWithContentDescription("Finish search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Finish search")
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithContentDescription("Search games: Z. Press A to edit.").assertExists()
    }

    @Test
    fun tvKeyboardDialogUsesCappedCenteredWidthInsteadOfStretchingAcrossScreen() {
        var density = 1f
        rule.setContent {
            density = LocalDensity.current.density
            MaterialTheme {
                ControllerSearchKeyboardDialog(
                    query = "",
                    onQueryChange = {},
                    onDismiss = {},
                )
            }
        }

        val dialog = rule.onNodeWithContentDescription("Controller search keyboard dialog")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            "TV search dialog must honor its 900dp width cap",
            dialog.width / density <= 900.5f,
        )
    }

    @Test
    fun shellBClosesSearchKeyboardBeforeGlobalNavigation() {
        var query by mutableStateOf("")
        var keyboardVisible by mutableStateOf(true)
        var globalBackCalls = 0
        val controllerActions = ControllerActionRegistry().apply {
            configure(
                xLabel = "Search",
                onX = {},
                yLabel = "Filters",
                onY = {},
                onBack = {
                    if (keyboardVisible) {
                        keyboardVisible = false
                        true
                    } else false
                },
            )
        }
        rule.setContent {
            MaterialTheme {
                Column(
                    Modifier.fillMaxWidth().onGameBoxRootInput(
                        soundsEnabled = false,
                        playSoundEffect = {},
                        controllerActions = controllerActions,
                        movePrimaryTab = {},
                        openQuickActions = {},
                        openSystemStatus = {},
                        fallbackX = {},
                        fallbackY = {},
                        handleBack = {
                            controllerActions.dispatchBack { globalBackCalls++; true }
                        },
                    ),
                ) {
                    if (keyboardVisible) {
                        ControllerSearchKeyboardDialog(
                            query = query,
                            onQueryChange = { query = it },
                            onDismiss = { keyboardVisible = false },
                        )
                    }
                }
            }
        }

        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_B)
        rule.waitForIdle()

        assertFalse(keyboardVisible)
        assertEquals(0, globalBackCalls)

        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_B)
        rule.waitForIdle()
        assertEquals(1, globalBackCalls)
    }

    @Test
    fun aReopensControllerKeyboardAfterBClosesItWithoutLosingQuery() {
        var query by mutableStateOf("")
        var keyboardVisible by mutableStateOf(false)
        rule.setContent {
            MaterialTheme {
                Column {
                    ControllerSearchField(
                        query = query,
                        onOpenKeyboard = { keyboardVisible = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (keyboardVisible) {
                        ControllerSearchKeyboardDialog(
                            query = query,
                            onQueryChange = { query = it },
                            onDismiss = { keyboardVisible = false },
                        )
                    }
                }
            }
        }

        rule.onNodeWithContentDescription("Search games. Press A to type with the controller.")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        rule.onNodeWithContentDescription("Key Q").assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        rule.onNodeWithContentDescription("Search query: Q").assertTextEquals("Q")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_B)
        rule.waitForIdle()

        assertFalse(keyboardVisible)
        rule.onNodeWithContentDescription("Search games: Q. Press A to edit.").assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        rule.onNodeWithContentDescription("Key Q").assertIsFocused()
        rule.onNodeWithContentDescription("Search query: Q").assertExists()
    }
}
