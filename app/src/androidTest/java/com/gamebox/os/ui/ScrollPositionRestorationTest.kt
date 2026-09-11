package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollPositionRestorationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun primaryScreenScrollSurvivesSavedInstanceStateRestore() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            val uiState = rememberGameBoxUiState()
            CompositionLocalProvider(LocalGameBoxUiState provides uiState) {
                Column(
                    Modifier.height(120.dp).verticalScroll(restoredScrollState("test-screen"))
                ) {
                    Text("Top")
                    Spacer(Modifier.height(600.dp))
                    Text("Bottom", Modifier.testTag("bottom"))
                }
            }
        }

        rule.onNodeWithTag("bottom").performScrollTo().assertIsDisplayed()
        rule.waitForIdle()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("bottom").assertIsDisplayed()
    }
}
