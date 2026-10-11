package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserDownloadRetryTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun failedTrackedTransferOffersRetryAction() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                BrowserDownloadQueueItem(
                    record = BrowserDownloadRecord(
                        downloadId = 42L,
                        stagedName = "42.nes",
                        displayName = "authorized-game.nes",
                        gameId = "authorized-game",
                        title = "Authorized Game",
                        platformName = "NES",
                        platformId = "nes",
                        status = BrowserDownloadStatus.FAILED,
                        error = "Network interrupted",
                    ),
                    onRetry = { retries++ },
                )
            }
        }

        compose.onNodeWithText("Retry download or import").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun androidPausedTransferCanBeRestartedFromDownloadManager() {
        var restarts = 0
        compose.setContent {
            MaterialTheme {
                BrowserDownloadQueueItem(
                    record = BrowserDownloadRecord(
                        downloadId = 43L,
                        stagedName = "paused.nes",
                        displayName = "authorized-game.nes",
                        gameId = "authorized-game",
                        title = "Authorized Game",
                        platformName = "NES",
                        platformId = "nes",
                        downloadUrl = "https://example.org/authorized-game.nes",
                        status = BrowserDownloadStatus.PAUSED,
                    ),
                    onRetry = { restarts++ },
                )
            }
        }

        compose.onNodeWithText("Paused by Android").assertIsDisplayed()
        compose.onNodeWithText("Restart download").performClick()
        compose.runOnIdle { assertEquals(1, restarts) }
    }

    @Test
    fun activeTransferShowsByteProgressAndSpeedEvenWhenTotalSizeIsUnknown() {
        compose.setContent {
            MaterialTheme {
                BrowserDownloadQueueItem(
                    record = BrowserDownloadRecord(
                        downloadId = 44L,
                        stagedName = "active.nes",
                        displayName = "active.nes",
                        gameId = "active-game",
                        title = "Active Game",
                        platformName = "NES",
                        platformId = "nes",
                        status = BrowserDownloadStatus.DOWNLOADING,
                        downloadedBytes = 1536L * 1024L,
                        totalBytes = 2L * 1024L * 1024L,
                        bytesPerSecond = 1536L * 1024L,
                    ),
                )
            }
        }

        compose.onNodeWithText("1.5 MB / 2.0 MB · 75%").assertIsDisplayed()
        compose.onNodeWithText("1.5 MB/s").assertIsDisplayed()
    }

    @Test
    fun unknownTransferSizeStillShowsDownloadedBytesAndSpeed() {
        compose.setContent {
            MaterialTheme {
                BrowserDownloadQueueItem(
                    record = BrowserDownloadRecord(
                        downloadId = 45L,
                        stagedName = "unknown.nes",
                        displayName = "unknown.nes",
                        gameId = "unknown-game",
                        title = "Unknown Size Game",
                        platformName = "NES",
                        platformId = "nes",
                        status = BrowserDownloadStatus.DOWNLOADING,
                        downloadedBytes = 512L * 1024L,
                        bytesPerSecond = 256L * 1024L,
                    ),
                )
            }
        }

        compose.onNodeWithText("0.5 MB downloaded").assertIsDisplayed()
        compose.onNodeWithText("256 KB/s").assertIsDisplayed()
    }

    @Test
    fun importingTrackedTransferShowsStageFileAndDeterminateProgress() {
        compose.setContent {
            MaterialTheme {
                BrowserDownloadQueueItem(
                    record = BrowserDownloadRecord(
                        downloadId = 46L,
                        stagedName = "archive.zip",
                        displayName = "archive.zip",
                        gameId = "importing-game",
                        title = "Importing Game",
                        platformName = "Game Boy Advance",
                        platformId = "gba",
                        status = BrowserDownloadStatus.IMPORTING,
                        importStage = "Extracting archive",
                        importBytesProcessed = 768L * 1024L,
                        importTotalBytes = 2L * 1024L * 1024L,
                        importFileIndex = 1,
                        importFileCount = 2,
                    ),
                )
            }
        }

        compose.onNodeWithText("Extracting archive · file 1 of 2").assertIsDisplayed()
        compose.onNodeWithText("37%").assertIsDisplayed()
        compose.onNodeWithText("768 KB / 2.0 MB").assertIsDisplayed()
    }

    @Test
    fun rejectedAttemptShowsReasonWithoutUnsafeRetry() {
        compose.setContent {
            MaterialTheme {
                BrowserDownloadQueueItem(
                    record = BrowserDownloadRecord(
                        downloadId = -1L,
                        stagedName = "rejected.download",
                        displayName = "Unrecognized format.bin",
                        gameId = "rejected-game",
                        title = "Rejected Game",
                        platformName = "NES",
                        platformId = "nes",
                        status = BrowserDownloadStatus.FAILED,
                        error = ".bin is not supported for Nintendo Entertainment System",
                    ),
                )
            }
        }

        compose.onNodeWithText("Needs attention").assertIsDisplayed()
        compose.onNodeWithText(".bin is not supported for Nintendo Entertainment System").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Retry download or import").fetchSemanticsNodes().isEmpty())
    }
}
