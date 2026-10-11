package com.gamebox.os.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackedSourcePageProgressTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun announcesWebViewProgressAndDisappearsWhenLoaded() {
        val progress = mutableIntStateOf(42)
        compose.setContent {
            Box(Modifier) {
                TrackedSourcePageProgress(progress.intValue, "Loading source page")
            }
        }

        compose.onNodeWithContentDescription("Loading source page")
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(0.42f, 0f..1f),
            ))

        compose.runOnIdle { progress.intValue = 100 }
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription("Loading source page").assertCountEquals(0)
    }
}
