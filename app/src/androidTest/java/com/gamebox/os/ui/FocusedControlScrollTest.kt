package com.gamebox.os.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusedControlScrollTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun focusedControlNearScrollEndIsBroughtIntoViewport() {
        compose.setContent {
            val scrollState = rememberScrollState()
            val endFocus = remember { FocusRequester() }
            LaunchedEffect(endFocus) {
                withFrameNanos { }
                endFocus.requestFocus()
            }
            Column(
                Modifier
                    .height(180.dp)
                    .verticalScroll(scrollState),
            ) {
                repeat(4) { index ->
                    val focus = if (index == 3) Modifier.focusRequester(endFocus) else Modifier
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .then(focus)
                            .bringFocusedControlIntoView()
                            .focusable()
                            .testTag("focus-item-$index"),
                    )
                }
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("focus-item-3").assertIsFocused().assertIsDisplayed()
    }
}
