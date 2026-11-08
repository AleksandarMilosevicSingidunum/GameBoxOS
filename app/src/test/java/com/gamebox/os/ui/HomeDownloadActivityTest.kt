package com.gamebox.os.ui

import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.GameId
import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeDownloadActivityTest {
    private fun record(status: BrowserDownloadStatus) = BrowserDownloadRecord(
        downloadId = 1, stagedName = "game.7z", displayName = "Game", gameId = "game",
        title = "Game", platformName = "PSP", platformId = "psp", status = status,
    )

    @Test fun completedAndCancelledBrowserItemsDoNotShowActivity() {
        assertNull(homeDownloadActivity(emptyList(), listOf(record(BrowserDownloadStatus.IMPORTED), record(BrowserDownloadStatus.CANCELLED))))
    }

    @Test fun importProgressUsesImportBytesAndUnknownSizeIsIndeterminate() {
        val importing = record(BrowserDownloadStatus.IMPORTING).copy(importBytesProcessed = 25, importTotalBytes = 100)
        assertEquals(HomeDownloadActivity("Game", "Importing", .25f, 1), homeDownloadActivity(emptyList(), listOf(importing)))
        assertNull(homeDownloadActivity(emptyList(), listOf(importing.copy(importTotalBytes = null)))?.progress)
    }

    @Test fun attentionTakesPriorityAndCheckingCountsAsActivity() {
        val checking = DownloadJob("1", GameId("a"), "Checking game", DownloadStatus.VERIFYING, 100, 100)
        val failed = record(BrowserDownloadStatus.FAILED)
        assertEquals(HomeDownloadActivity("Game", "Needs attention", null, 2), homeDownloadActivity(listOf(checking), listOf(failed)))
        assertEquals(2, downloadActivityCount(listOf(DownloadStatus.VERIFYING, DownloadStatus.INSTALLING), emptyList()))
    }
}
