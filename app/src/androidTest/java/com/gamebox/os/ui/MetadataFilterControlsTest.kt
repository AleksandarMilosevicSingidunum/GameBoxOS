package com.gamebox.os.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetadataFilterControlsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun removedSelectionCanBeClearedThenNewMetadataSelected() {
        val selected = mutableStateOf<String?>("US")
        val values = mutableStateOf(listOf("US"))
        composeRule.setContent {
            MetadataFilterControl("Region", values.value, selected.value) { selected.value = it }
        }
        composeRule.runOnIdle { values.value = emptyList() }
        composeRule.onNodeWithText("Region: US").performClick()
        composeRule.onNodeWithText("Region: All").assertExists()
        composeRule.runOnIdle { values.value = listOf("EU") }
        composeRule.onNodeWithText("Region: All").performClick()
        composeRule.onNodeWithText("Region: EU").assertExists()
        composeRule.onNodeWithText("Region: EU").performClick()
        composeRule.onNodeWithText("Region: All").assertExists()
    }
}
