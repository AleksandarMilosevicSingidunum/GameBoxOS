package com.gamebox.os.ui

import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserDownloadSectionsTest {
    @Test
    fun completedBackgroundImportStaysInManagerHistory() {
        val queued = record(1L, BrowserDownloadStatus.QUEUED)
        val downloading = record(2L, BrowserDownloadStatus.DOWNLOADING)
        val imported = record(3L, BrowserDownloadStatus.IMPORTED)
        val failed = record(4L, BrowserDownloadStatus.FAILED)

        val sections = browserDownloadSections(listOf(queued, downloading, imported, failed))

        assertEquals(listOf(queued, downloading, failed), sections.active)
        assertEquals(listOf(imported), sections.completed)
    }

    @Test
    fun completedHistoryIsTheUsefulDefaultWhenTheActiveQueueIsEmpty() {
        val sections = browserDownloadSections(listOf(record(7L, BrowserDownloadStatus.IMPORTED)))

        assertEquals(true, shouldShowCompletedDownloadsByDefault(sections))
    }

    private fun record(id: Long, status: BrowserDownloadStatus) = BrowserDownloadRecord(
        downloadId = id,
        stagedName = "$id.nes",
        displayName = "game-$id.nes",
        gameId = "game-$id",
        title = "Game $id",
        platformName = "NES",
        platformId = "nes",
        status = status,
    )
}
