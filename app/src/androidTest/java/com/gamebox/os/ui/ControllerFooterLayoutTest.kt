package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControllerFooterLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun profileAndControllerStatusStayInsideTheCouchFooterBounds() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    ControllerFooter(
                        xLabel = "Search",
                        yLabel = "Switch Profile",
                        profileName = "Local player",
                    )
                }
            }
        }

        val footer = compose.onNodeWithContentDescription("Controller footer")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val status = compose.onNode(hasContentDescription("Controller status:", substring = true))
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val profile = compose.onNodeWithText("Local player").fetchSemanticsNode().boundsInRoot

        assertTrue("controller status must not begin above the footer", status.top >= footer.top)
        assertTrue("controller status must not be clipped below the footer", status.bottom <= footer.bottom)
        assertTrue("player name and status should share the compact footer row", kotlin.math.abs(profile.top - status.top) <= 2f)
    }

    @Test
    fun enlargedControllerLegendFitsA1920DpTvFooter() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredWidth(1920.dp).height(56.dp)) {
                    ControllerFooter(
                        xLabel = "Search",
                        yLabel = "Switch Profile",
                        profileName = "Local player",
                    )
                }
            }
        }

        val footer = compose.onNodeWithContentDescription("Controller footer")
            .assertExists().fetchSemanticsNode().boundsInRoot
        val lastAction = compose.onNodeWithText("View  Status", useUnmergedTree = true)
            .assertExists().fetchSemanticsNode().boundsInRoot
        val status = compose.onNode(hasContentDescription("Controller status:", substring = true))
            .assertExists().fetchSemanticsNode().boundsInRoot

        assertTrue("controller legend must begin inside the footer", lastAction.left >= footer.left)
        assertTrue("controller legend must end before the profile", lastAction.right < status.left)
        assertTrue("profile/status must fit inside the footer", status.right <= footer.right)
    }

    @Test
    fun controllerGlyphAndActionTextMeetLivingRoomReadabilityMinimum() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredWidth(1920.dp).height(56.dp)) {
                    ControllerFooter(
                        xLabel = "Search",
                        yLabel = "Switch Profile",
                        profileName = "Local player",
                    )
                }
            }
        }

        compose.onNodeWithText("A", useUnmergedTree = true).assertHeightIsAtLeast(14.dp)
        compose.onNodeWithText("Select", useUnmergedTree = true).assertHeightIsAtLeast(16.dp)
        compose.onNodeWithText("View  Status", useUnmergedTree = true).assertHeightIsAtLeast(16.dp)
    }
}
