package com.gamebox.os.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlueprintNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun focusDoesNotChangeSelectionAndClickSelectsOneTab() {
        lateinit var inputModeManager: InputModeManager
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            var selected by remember { mutableStateOf(Destination.HOME) }
            MaterialTheme {
                Row {
                    listOf(Destination.HOME, Destination.LIBRARY).forEach { item ->
                        NavButton(item, selected) { selected = it }
                    }
                }
            }
        }
        val home = compose.onNodeWithContentDescription("Home tab")
        val library = compose.onNodeWithContentDescription("Library tab")
        home.assertIsSelected()
        library.assertIsNotSelected()
        // Clickable tabs only accept non-touch focus in keyboard/controller mode.
        compose.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        library.performSemanticsAction(SemanticsActions.RequestFocus)
        library.assertIsFocused().assertIsNotSelected()
        home.assertIsSelected()
        library.performClick()
        library.assertIsSelected()
        home.assertIsNotSelected()
    }
}

