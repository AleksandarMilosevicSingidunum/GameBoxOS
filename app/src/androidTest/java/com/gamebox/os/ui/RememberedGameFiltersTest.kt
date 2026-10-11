package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RememberedGameFiltersTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eachScreenKeepsItsOwnFilterNamespaceAndPersistsChanges() {
        val uiState = GameBoxUiState.create().apply {
            rememberScreenValue("store.query", "store saved")
            rememberScreenValue("library.query", "library saved")
        }
        composeRule.setContent {
            val storeFilters = rememberGameFilters(uiState, "store")
            val libraryFilters = rememberGameFilters(uiState, "library")
            Column {
                Text("store=${storeFilters.query}")
                Text("library=${libraryFilters.query}")
                Button(onClick = {
                    storeFilters.query = "updated store"
                    storeFilters.region = "EU"
                }) { Text("Update store filters") }
            }
        }

        composeRule.onNodeWithText("store=store saved").assertExists()
        composeRule.onNodeWithText("library=library saved").assertExists()
        composeRule.onNodeWithText("Update store filters").performClick()
        composeRule.waitForIdle()

        assertEquals("updated store", uiState.screenValue("store.query"))
        assertEquals("EU", uiState.screenValue("store.region"))
        assertEquals("library saved", uiState.screenValue("library.query"))
    }
}
