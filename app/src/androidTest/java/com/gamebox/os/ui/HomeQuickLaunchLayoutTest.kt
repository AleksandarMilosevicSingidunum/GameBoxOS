package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
class HomeQuickLaunchLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun enlargedTextRemainsInsideCompactTilesAndActionsWork() {
        var opens = 0
        compose.setContent {
            // Fit a desktop-width panel on either screenshot emulator, with 200% text.
            CompositionLocalProvider(LocalDensity provides Density(.5f, 2f)) {
                MaterialTheme {
                    Column(Modifier.width(600.dp)) {
                        HomeQuickLaunchRow(openPc = { opens++ }, compact = false)
                    }
                }
            }
        }
        listOf("Desktop", "Steam", "Xbox", "Epic Games").forEachIndexed { index, label ->
            val tile = compose.onNodeWithContentDescription("Quick launch $label; opens PC Hub")
            val bounds = tile.fetchSemanticsNode().boundsInRoot
            val title = compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val subtitle = compose.onAllNodesWithText("PC Hub", useUnmergedTree = true)[index].fetchSemanticsNode().boundsInRoot
            assertTrue("$label title must remain inside its tile", title.top >= bounds.top && title.bottom <= bounds.bottom)
            assertTrue("$label subtitle must remain inside its tile", subtitle.top >= title.bottom && subtitle.bottom <= bounds.bottom)
            assertTrue("$label tile must grow beyond its 100 dp minimum for enlarged text", bounds.height > 50f)
            tile.assertIsDisplayed().performClick()
        }
        compose.onAllNodesWithText("PC Hub", useUnmergedTree = true).assertCountEquals(4)
        compose.runOnIdle { assertEquals(4, opens) }
    }
}

