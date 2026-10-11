package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDetailsToolsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun gamepadAActivatesGameSettingsAndMetadataActions() {
        lateinit var inputModeManager: androidx.compose.ui.input.InputModeManager
        var settingsToggles = 0
        var metadataEdits = 0
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                GameDetailsToolsSection(
                    showGameSettings = settingsToggles % 2 == 1,
                    metadataLabel = "Edit metadata",
                    reducedMotion = true,
                    onToggleGameSettings = { settingsToggles++ },
                    onEditMetadata = { metadataEdits++ },
                    gameSettings = {},
                )
            }
        }

        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithText("Game settings & emulator")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, settingsToggles) }
        compose.onNodeWithText("Hide game settings").assertIsFocused()

        compose.onNodeWithContentDescription("Edit game metadata and artwork")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, metadataEdits) }
    }
}
