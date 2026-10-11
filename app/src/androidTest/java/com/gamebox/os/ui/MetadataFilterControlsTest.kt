package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
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
class MetadataFilterControlsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun gamepadACyclesFocusedFilterFromAllToFirstAvailableValue() {
        lateinit var inputModeManager: androidx.compose.ui.input.InputModeManager
        var selectedGenre: String? = null
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                val selected = remember { mutableStateOf<String?>(null) }
                MetadataFilterControl(
                    label = "Genre",
                    values = listOf("Adventure", "Racing"),
                    selected = selected.value,
                    onSelected = {
                        selected.value = it
                        selectedGenre = it
                    },
                )
            }
        }

        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithText("Genre: All")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)

        compose.runOnIdle { assertEquals("Adventure", selectedGenre) }
        compose.onNodeWithText("Genre: Adventure").assertIsFocused()
    }
}
