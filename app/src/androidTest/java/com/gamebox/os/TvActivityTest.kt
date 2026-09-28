package com.gamebox.os

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/** Exercises the dedicated external-display activity contract on a real Android runtime. */
@RunWith(AndroidJUnit4::class)
class TvActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<TvActivity>()

    @Test
    fun tvActivityRequestsLandscapeAndRendersBlueprintHome() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, rule.activity.requestedOrientation)
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Home tab").assertIsDisplayed()
        rule.onNodeWithContentDescription("GameBox logo").assertIsDisplayed()
    }
}
