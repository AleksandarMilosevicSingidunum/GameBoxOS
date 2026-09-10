package com.gamebox.os.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaTileLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun squareTileKeepsLabelsAndClickAction() = verifyTile(1f)
    @Test fun enlargedTileKeepsLabelsAndClickAction() = verifyTile(2f)

    private fun verifyTile(fontScale: Float) {
        var launches = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MaterialTheme {
                    BlueprintShortcutTile(AppShortcut("YouTube", "Video", "test.youtube"), false,
                        Modifier.size(128.dp, (128 + 48 * (fontScale - 1)).dp).testTag("media-tile")) { launches++ }
                }
            }
        }
        val tile = compose.onNodeWithTag("media-tile")
        val bounds = tile.fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("YouTube", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val status = compose.onNodeWithText("Setup required", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(title.top >= bounds.top && title.bottom <= status.top)
        assertTrue(status.bottom <= bounds.bottom && status.height > 0)
        tile.assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, launches) }
    }
}

