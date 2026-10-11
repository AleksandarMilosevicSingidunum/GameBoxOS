package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlueprintSwitchTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun switchShowsFocusAndTogglesWithControllerA() {
        compose.setContent {
            val checked = remember { mutableStateOf(false) }
            MaterialTheme {
                BlueprintSwitch(
                    checked = checked.value,
                    onCheckedChange = { checked.value = it },
                    modifier = Modifier.testTag("reduce-motion-switch"),
                )
            }
        }

        val toggle = compose.onNodeWithTag("reduce-motion-switch")
        toggle.performSemanticsAction(SemanticsActions.RequestFocus)
        toggle.assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        toggle.assertIsOn()
    }

    @Test
    fun disabledSwitchDoesNotAcceptControllerActivation() {
        var changeCount = 0
        compose.setContent {
            MaterialTheme {
                BlueprintSwitch(
                    checked = false,
                    onCheckedChange = { changeCount++ },
                    enabled = false,
                    modifier = Modifier.testTag("disabled-switch"),
                )
            }
        }

        compose.onNodeWithTag("disabled-switch").assertIsNotEnabled()
        compose.runOnIdle { org.junit.Assert.assertEquals(0, changeCount) }
    }

    @Test
    fun focusedBelowFoldSwitchScrollsIntoTheSettingsViewport() {
        compose.setContent {
            MaterialTheme {
                Column(
                    Modifier.width(220.dp)
                        .height(120.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("settings-scroll-viewport"),
                ) {
                    Spacer(Modifier.height(480.dp))
                    BlueprintSwitch(
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.testTag("below-fold-switch"),
                    )
                }
            }
        }

        compose.onNodeWithTag("below-fold-switch")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("settings-scroll-viewport").fetchSemanticsNode().boundsInRoot
        val toggle = compose.onNodeWithTag("below-fold-switch").fetchSemanticsNode().boundsInRoot
        assertTrue("Focused switch should not be below the settings viewport", toggle.bottom <= viewport.bottom)
        assertTrue("Focused switch should not be above the settings viewport", toggle.top >= viewport.top)
    }
}
