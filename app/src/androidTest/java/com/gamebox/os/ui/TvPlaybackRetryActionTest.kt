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
class TvPlaybackRetryActionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun retryIsOnlyShownAfterFailureAndInvokesRetry() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                TvPlaybackRetryAction(error = "Stream failed", onRetry = { retries++ })
            }
        }

        compose.onNodeWithText("Retry stream").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun retryIsHiddenDuringNormalPlayback() {
        compose.setContent {
            MaterialTheme { TvPlaybackRetryAction(error = null, onRetry = {}) }
        }

        compose.onNodeWithText("Retry stream").assertDoesNotExist()
    }

    @Test
    fun officialFallbackIsAnExplicitActionAlongsideRetry() {
        var opened = 0
        compose.setContent {
            MaterialTheme {
                TvPlaybackRetryAction(
                    error = "Stream failed",
                    officialFallbackLabel = "Open RTS Planeta",
                    onOfficialFallback = { opened++ },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("Retry stream").assertIsDisplayed()
        compose.onNodeWithText("Open RTS Planeta").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }
}
