package com.gamebox.os.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.MaterialTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.storage.ContentMigrationPlan
import com.gamebox.os.storage.ExternalStorageState
import com.gamebox.os.storage.ExternalStorageStatus
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStorageInspectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun storageRootsAnnounceInspectionAndKeepMigrationDisabled() {
        compose.setContent {
            MaterialTheme {
                SettingsStorageSection(
                    compact = false,
                    currentSettings = GameBoxSettings(),
                    externalStorageStatus = null,
                    emulatorSaveStorageStatus = null,
                    migrationPlan = ContentMigrationPlan(emptyList(), 0L),
                    onChooseExternalLibrary = {},
                    onForgetExternalLibrary = {},
                    onMigrateInstalledContent = {},
                    onChooseEmulatorSaveRoot = {},
                    onForgetEmulatorSaveRoot = {},
                )
            }
        }

        compose.onNodeWithText("Keep your games if you uninstall GameBox").assertIsDisplayed()
        compose.onNodeWithText("Checking external library…").assertIsDisplayed()
        compose.onNodeWithText("Checking emulator save root…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Migrate installed content").assertIsNotEnabled()
    }

    @Test
    fun migrationPlanScanIsClearlyAnnouncedAndCannotBeConfirmed() {
        compose.setContent {
            MaterialTheme {
                SettingsStorageSection(
                    compact = false,
                    currentSettings = GameBoxSettings(),
                    externalStorageStatus = ExternalStorageStatus(
                        ExternalStorageState.AVAILABLE_READ_WRITE,
                        displayName = "SSD",
                        message = "Ready",
                    ),
                    emulatorSaveStorageStatus = null,
                    migrationPlan = null,
                    onChooseExternalLibrary = {},
                    onForgetExternalLibrary = {},
                    onMigrateInstalledContent = {},
                    onChooseEmulatorSaveRoot = {},
                    onForgetEmulatorSaveRoot = {},
                )
            }
        }

        compose.onNodeWithText("Scanning installed content…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Migrate installed content").assertIsNotEnabled()
    }

    @Test
    fun failedMigrationScanOffersRetryAndKeepsMigrationActionDisabled() {
        var retried = false
        compose.setContent {
            MaterialTheme {
                SettingsStorageSection(
                    compact = false,
                    currentSettings = GameBoxSettings(),
                    externalStorageStatus = ExternalStorageStatus(
                        ExternalStorageState.AVAILABLE_READ_WRITE,
                        displayName = "SSD",
                        message = "Ready",
                    ),
                    emulatorSaveStorageStatus = null,
                    migrationPlan = null,
                    migrationScanFailed = true,
                    onChooseExternalLibrary = {},
                    onForgetExternalLibrary = {},
                    onMigrateInstalledContent = {},
                    onRetryMigrationScan = { retried = true },
                    onChooseEmulatorSaveRoot = {},
                    onForgetEmulatorSaveRoot = {},
                )
            }
        }

        compose.onNodeWithText("Installed content scan failed").assertIsDisplayed()
        compose.onNodeWithContentDescription("Migrate installed content").assertIsNotEnabled()
        compose.onNodeWithText("Retry scan").assertIsEnabled().performClick()
        assertTrue(retried)
    }

    @Test
    fun documentProviderInspectionRunsOffTheMainThread() {
        val inspectionThread = AtomicReference<String?>()
        val inspector = Any()

        compose.setContent {
            MaterialTheme {
                val status = rememberStorageStatus("content://test/tree", inspector) {
                    inspectionThread.set(Thread.currentThread().name)
                    ExternalStorageStatus(
                        ExternalStorageState.NOT_CONFIGURED,
                        message = "Inspection complete",
                    )
                }
                androidx.compose.material3.Text(status?.message ?: "Checking…")
            }
        }

        compose.waitUntil(5_000) { inspectionThread.get() != null }
        assertNotEquals("main", inspectionThread.get())
        compose.onNodeWithText("Inspection complete").assertIsDisplayed()
    }

    @Test
    fun providerFailureExitsCheckingStateWithAnActionableMessage() {
        val inspector = Any()
        compose.setContent {
            MaterialTheme {
                val status = rememberStorageStatus("content://unavailable/tree", inspector) {
                    error("Document provider is unavailable")
                }
                androidx.compose.material3.Text(status?.message ?: "Checking…")
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Document provider is unavailable")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Document provider is unavailable").assertIsDisplayed()
    }
}
