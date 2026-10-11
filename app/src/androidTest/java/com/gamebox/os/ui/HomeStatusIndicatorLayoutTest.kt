package com.gamebox.os.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeStatusIndicatorLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longControllerNameEllipsizesBeforeTheStatusPipAndCardEdge() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(200.dp)) {
                    HomeStatusItem(
                        icon = Icons.Rounded.SportsEsports,
                        label = "Controller",
                        value = "Nintendo Switch Pro Wireless Controller",
                    )
                }
            }
        }

        val cardRight = compose.onNodeWithTag("home-status-item-Controller", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.right
        val indicatorLeft = compose.onNodeWithTag("home-status-indicator-Controller", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.left
        val valueRight = compose.onNodeWithText(
            "Nintendo Switch Pro Wireless Controller",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.right

        assertTrue("Status value must ellipsize before the indicator", valueRight < indicatorLeft)
        assertTrue("Status indicator must remain inside the card", indicatorLeft < cardRight)
    }
}
