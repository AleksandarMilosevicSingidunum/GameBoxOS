package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameBoxLifecycleRestorationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun navigationSelectionAndFocusSurviveSavedInstanceStateRestore() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            val state = rememberGameBoxUiState()
            Column {
                Text("destination=" + state.destination)
                Text("selected=" + (state.selectedGameId ?: "none"))
                Text("focus=" + (state.restoreFocus("STORE", listOf("galaxy-patrol")) ?: "none"))
                Button(onClick = {
                    state.openDestination("STORE")
                    state.rememberFocus("STORE", "galaxy-patrol")
                    state.openGame("galaxy-patrol")
                }) { Text("Open fixture") }
            }
        }
        composeRule.onNodeWithText("Open fixture").performClick()
        composeRule.onNodeWithText("destination=STORE").assertExists()
        composeRule.onNodeWithText("selected=galaxy-patrol").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("destination=STORE").assertExists()
        composeRule.onNodeWithText("selected=galaxy-patrol").assertExists()
        composeRule.onNodeWithText("focus=galaxy-patrol").assertExists()
    }
}

