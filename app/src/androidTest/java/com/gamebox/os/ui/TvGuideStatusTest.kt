package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvGuideStatusTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun failedGuideOffersRetry() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                TvGuideStatus(false, "Network unavailable", true, { retries++ }, {})
            }
        }
        compose.onNodeWithText("Programme guide unavailable: Network unavailable").assertIsDisplayed()
        compose.onNodeWithText("Retry guide").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun missingGuideSourceOffersPlaylistOptions() {
        var opened = 0
        compose.setContent {
            MaterialTheme {
                TvGuideStatus(false, "This playlist does not provide a programme guide.", false, {}, { opened++ })
            }
        }
        compose.onNodeWithText("Playlist options").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun loadingGuideDoesNotOfferPrematureRetry() {
        compose.setContent { MaterialTheme { TvGuideStatus(true, null, true, {}, {}) } }
        compose.onNodeWithText("Loading programme guide…").assertIsDisplayed()
        compose.onNodeWithText("Retry guide").assertDoesNotExist()
    }
}
