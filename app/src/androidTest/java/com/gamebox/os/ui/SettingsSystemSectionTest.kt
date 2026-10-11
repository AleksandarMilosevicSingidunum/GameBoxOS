package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSystemSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun systemShortcutsForwardActionsAndDescribeConsoleStartup() {
        val openedSettings = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                Column {
                    SettingsSystemSection(
                        onOpenHomeSettings = { openedSettings += "home" },
                        onOpenStorageSettings = { openedSettings += "storage" },
                        onOpenAppDetails = { openedSettings += "details" },
                        onOpenSystemSettings = { openedSettings += "system" },
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Choose default Home app").performClick()
        compose.onNodeWithContentDescription("App storage").performClick()
        compose.onNodeWithContentDescription("GameBox app details").performClick()
        compose.onNodeWithContentDescription("Android system settings").performClick()
        compose.onNodeWithText("Console startup").assertExists()

        assertEquals(listOf("home", "storage", "details", "system"), openedSettings)
    }
}
