package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadManagerSectionTabsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun queueAndCompletedTabsExposeCountsAndSwitchSelection() {
        var showingCompleted = false
        compose.setContent {
            MaterialTheme {
                DownloadManagerSectionTabs(
                    queueCount = 2,
                    completedCount = 5,
                    showingCompleted = showingCompleted,
                    onSelectCompleted = { showingCompleted = it },
                )
            }
        }

        compose.onNodeWithText("Download Queue · 2").assertIsSelected()
        compose.onNodeWithText("Completed · 5").performClick()
        compose.runOnIdle { assertTrue(showingCompleted) }
        compose.onNodeWithText("Completed · 5").assertIsSelected()
        compose.onNodeWithText("Download Queue · 2").performClick()
        compose.runOnIdle { assertFalse(showingCompleted) }
    }
}
