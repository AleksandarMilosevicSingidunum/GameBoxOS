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
                Button(onClick = { state.openDestination("HOME") }) { Text("Home") }
                Button(onClick = { state.openDestination("LIBRARY") }) { Text("Library") }
                Button(onClick = { state.openDestination("STORE") }) { Text("Store") }
                Button(onClick = { state.openDestination("DOWNLOADS") }) { Text("Downloads") }
                Button(onClick = { state.openDestination("MEDIA") }) { Text("Media") }
                Button(onClick = { state.openDestination("PC") }) { Text("PC") }
                Button(onClick = { state.openDestination("SETTINGS") }) { Text("Settings") }
            }
        }
        listOf("Home", "Library", "Store", "Downloads", "Media", "PC", "Settings").forEach { label ->
            composeRule.onNodeWithText(label).performClick()
            composeRule.onNodeWithText("current=$label").assertExists()
        }
    }
}
