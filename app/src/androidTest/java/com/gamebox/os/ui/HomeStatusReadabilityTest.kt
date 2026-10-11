package com.gamebox.os.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeStatusReadabilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun livingRoomStatusLabelAndValueKeepReadableLineHeights() {
        compose.setContent {
            MaterialTheme {
                HomeStatusItem(
                    icon = Icons.Rounded.Storage,
                    label = "Storage",
                    value = "4% used",
                    progress = 0.04f,
                )
            }
        }

        val labelHeight = compose.onNodeWithText("Storage").fetchSemanticsNode().boundsInRoot.height
        val valueHeight = compose.onNodeWithText("4% used").fetchSemanticsNode().boundsInRoot.height
        val minimumLabelHeight = with(compose.density) { 13.dp.toPx() }
        val minimumValueHeight = with(compose.density) { 15.dp.toPx() }
        assertTrue("Status label is below the living-room size floor", labelHeight >= minimumLabelHeight)
        assertTrue("Status value is below the living-room size floor", valueHeight >= minimumValueHeight)
    }
}
