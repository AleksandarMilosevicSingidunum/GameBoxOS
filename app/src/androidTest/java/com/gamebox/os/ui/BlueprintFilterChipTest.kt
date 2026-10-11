package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlueprintFilterChipTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun filterChipDisplaysControllerFocusAndActivatesWithA() {
        compose.setContent {
            val selected = remember { mutableStateOf(false) }
            MaterialTheme {
                BlueprintFilterChip(
                    selected = selected.value,
                    onClick = { selected.value = !selected.value },
                    label = { Text("Favorites") },
                )
            }
        }

        val chip = compose.onNodeWithText("Favorites")
        chip.performSemanticsAction(SemanticsActions.RequestFocus)
        chip.assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.onNodeWithText("Favorites").assertIsSelected()
    }

    @Test
    fun focusedOverflowChipIsBroughtIntoTheHorizontalViewport() {
        compose.setContent {
            MaterialTheme {
                Row(
                    Modifier.width(150.dp)
                        .horizontalScroll(rememberScrollState())
                        .testTag("console-filter-viewport"),
                ) {
                    (1..8).forEach { number ->
                        BlueprintFilterChip(
                            selected = number == 1,
                            onClick = {},
                            label = { Text("Console $number") },
                            modifier = Modifier.testTag("console-filter-$number"),
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("console-filter-8")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("console-filter-viewport").fetchSemanticsNode().boundsInRoot
        val focusedChip = compose.onNodeWithTag("console-filter-8").fetchSemanticsNode().boundsInRoot
        assertTrue("Focused console filter must be visible at the viewport's right edge", focusedChip.right <= viewport.right)
        assertTrue("Focused console filter must not be clipped at the viewport's left edge", focusedChip.left >= viewport.left)
    }
}
