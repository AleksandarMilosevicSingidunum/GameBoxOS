package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.importer.RomImportProgress
import com.gamebox.os.importer.RomImportStage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameImportProgressIndicatorTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactImportProgressIsVisibleAndAnnouncedAsIndeterminateWork() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(560.dp)) {
                    GameImportProgressIndicator(Modifier.width(520.dp))
                }
            }
        }

        compose.onNodeWithContentDescription("Importing and verifying game copy")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Import in progress",
            ))
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            ))
    }

    @Test
    fun knownCopySizeShowsDeterminateProgressAndSpokenPercentage() {
        val progress = RomImportProgress(
            stage = RomImportStage.COPYING_AND_VERIFYING,
            bytesProcessed = 128,
            totalBytes = 512,
            fileIndex = 1,
            fileCount = 1,
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(560.dp)) {
                    GameImportProgressIndicator(
                        modifier = Modifier.width(520.dp),
                        progress = progress,
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Importing and verifying game copy")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Copying and verifying · file 1 of 1 · 25%",
            ))
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(0.25f, 0f..1f),
            ))
    }

    @Test
    fun cancelActionIsVisibleAndInvokesItsHandler() {
        var cancelled = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(560.dp)) {
                    GameImportProgressActions(
                        modifier = Modifier.width(520.dp),
                        progress = null,
                        onCancel = { cancelled = true },
                    )
                }
            }
        }

        compose.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(cancelled) }
    }

    @Test
    fun gamepadACanFocusAndCancelAnActiveImport() {
        var cancelled = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(560.dp)) {
                    GameImportProgressActions(
                        modifier = Modifier.width(520.dp),
                        progress = null,
                        onCancel = { cancelled = true },
                    )
                }
            }
        }

        compose.onNodeWithText("Cancel")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertTrue(cancelled) }
    }

    @Test
    fun discoveryImportSectionExplainsSupportedFormatsAndRoutesDiscSetPicker() {
        var discSetPickerOpened = false
        compose.setContent {
            MaterialTheme {
                DiscoveryImportSection(
                    platformLabel = "PlayStation Portable",
                    acceptedFormats = "ISO, CSO, PBP, ZIP, 7Z",
                    importing = false,
                    progress = null,
                    message = null,
                    supportsDiscSet = true,
                    onCancel = {},
                    onImportDiscSet = { discSetPickerOpened = true },
                )
            }
        }

        compose.onNodeWithText("Accepted for PlayStation Portable: ISO, CSO, PBP, ZIP, 7Z")
            .assertIsDisplayed()
        compose.onNodeWithText("Import multi-file disc set").performClick()
        compose.runOnIdle { assertTrue(discSetPickerOpened) }
    }

    @Test
    fun gamepadAOpensMultiFileDiscSetPicker() {
        var discSetPickerOpened = false
        compose.setContent {
            MaterialTheme {
                DiscoveryImportSection(
                    platformLabel = "PSP",
                    acceptedFormats = "ISO, CSO, PBP, ZIP, 7Z",
                    importing = false,
                    progress = null,
                    message = null,
                    supportsDiscSet = true,
                    onCancel = {},
                    onImportDiscSet = { discSetPickerOpened = true },
                )
            }
        }

        compose.onNodeWithText("Import multi-file disc set")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertTrue(discSetPickerOpened) }
    }

    @Test
    fun discoveryImportSectionKeepsProgressAndCancellationWiredTogether() {
        val progress = RomImportProgress(
            stage = RomImportStage.EXTRACTING_ARCHIVE,
            bytesProcessed = 512,
            totalBytes = 1024,
            fileIndex = 1,
            fileCount = 1,
        )
        compose.setContent {
            MaterialTheme {
                DiscoveryImportSection(
                    platformLabel = "PSP",
                    acceptedFormats = "ISO, CSO, PBP",
                    importing = true,
                    progress = progress,
                    message = "Importing and verifying archive.zip…",
                    supportsDiscSet = false,
                    onCancel = {},
                    onImportDiscSet = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Importing and verifying game copy").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
