package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSectionNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun wideRailHighlightsSelectionAndReportsChosenSection() {
        var chosen: SettingsSection? = null
        compose.setContent {
            val selected = remember { mutableStateOf(SettingsSection.STORAGE) }
            MaterialTheme {
                SettingsWideSectionRail(
                    selected = selected.value,
                    storageFocusRequester = remember { FocusRequester() },
                    onSelect = {
                        selected.value = it
                        chosen = it
                    },
                )
            }
        }

        val storage = compose.onNodeWithContentDescription("Storage settings section")
        val display = compose.onNodeWithContentDescription("Display settings section")
        val sources = compose.onNodeWithContentDescription("Sources settings section")
        val developer = compose.onNodeWithContentDescription("Developer settings section")
        storage.assertIsSelected()
        display.assertIsNotSelected()
        display.performSemanticsAction(SemanticsActions.RequestFocus)
        display.assertIsFocused().assertIsNotSelected()
        display.performClick()
        display.assertIsSelected()
        assertEquals(SettingsSection.DISPLAY, chosen)
        sources.performClick()
        sources.assertIsSelected()
        assertEquals(SettingsSection.SOURCES, chosen)
        developer.performClick()
        developer.assertIsSelected()
        assertEquals(SettingsSection.DEVELOPER, chosen)
    }

    @Test
    fun requestedControllerSectionReceivesInitialFocus() {
        val controllersFocusRequester = FocusRequester()
        compose.setContent {
            MaterialTheme {
                SettingsWideSectionRail(
                    selected = SettingsSection.CONTROLLERS,
                    storageFocusRequester = controllersFocusRequester,
                    onSelect = {},
                    initialFocusSection = SettingsSection.CONTROLLERS,
                )
            }
        }

        compose.runOnIdle { controllersFocusRequester.requestFocus() }
        compose.onNodeWithContentDescription("Controllers settings section")
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun wideRailHeadingsMeetLivingRoomReadabilityMinimum() {
        compose.setContent {
            MaterialTheme {
                SettingsWideSectionRail(
                    selected = SettingsSection.STORAGE,
                    storageFocusRequester = remember { FocusRequester() },
                    onSelect = {},
                )
            }
        }

        compose.onNodeWithTag("wide-settings-heading").assertHeightIsAtLeast(14.dp)
        compose.onNodeWithTag("wide-settings-label-CONTROLLERS").assertHeightIsAtLeast(16.dp)
    }
}
