package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeNavigationCoverageTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun allPrimaryDestinationsAreReachable() {
        composeRule.setContent {
            val state = rememberGameBoxUiState()
            Column {
                Text("current=" + state.destination)
                Button(onClick = { state.openDestination("HOME") }) { Text("HOME") }
                Button(onClick = { state.openDestination("LIBRARY") }) { Text("LIBRARY") }
                Button(onClick = { state.openDestination("STORE") }) { Text("STORE") }
                Button(onClick = { state.openDestination("DOWNLOADS") }) { Text("DOWNLOADS") }
                Button(onClick = { state.openDestination("MEDIA") }) { Text("MEDIA") }
                Button(onClick = { state.openDestination("PC") }) { Text("PC") }
                Button(onClick = { state.openDestination("SETTINGS") }) { Text("SETTINGS") }
            }
        }
        listOf("HOME", "LIBRARY", "STORE", "DOWNLOADS", "MEDIA", "PC", "SETTINGS").forEach { label ->
            composeRule.onNodeWithText(label).performClick()
            composeRule.onNodeWithText("current=$label").assertExists()
        }
    }
}
