package com.gamebox.os.importer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BrowserDownloadRecoveryTest {
    @Test
    fun durableMonitorKeepsInterruptedImportsActiveUntilTheirOwnRecordCompletes() {
        val queued = record(21L, BrowserDownloadStatus.QUEUED)
        val importing = record(22L, BrowserDownloadStatus.IMPORTING)
        val failed = record(23L, BrowserDownloadStatus.FAILED)
        val imported = record(24L, BrowserDownloadStatus.IMPORTED)
        val uncommitted = record(-25L, BrowserDownloadStatus.QUEUED)

        assertEquals(
            listOf(21L, 22L),
            activeBrowserDownloadIds(listOf(queued, importing, failed, imported, uncommitted)),
        )
    }

    @Test
    fun eachAndroidTransferHasAStableUniqueDurableWorkName() {
        assertEquals("gamebox-browser-download:41", browserDownloadWorkName(41L))
        assertEquals("gamebox-browser-download:42", browserDownloadWorkName(42L))
        assertEquals(browserDownloadWorkName(41L), browserDownloadWorkName(41L))
    }

    @Test
    fun manualImportsUseTheirOwnDurableWorkChainAndNeverStartBrowserMonitoring() {
        val manual = record(-41L, BrowserDownloadStatus.QUEUED).copy(manualImport = true)

        assertEquals("gamebox-manual-import:-41", ManualRomImportWorker.workName(manual.downloadId))
        assertEquals(emptyList<Long>(), activeBrowserDownloadIds(listOf(manual)))
    }

    @Test
    fun startupReconciliationRecoversQueuedAndInterruptedManualImports() {
        val queued = record(-41L, BrowserDownloadStatus.QUEUED).copy(manualImport = true)
        val importing = record(-42L, BrowserDownloadStatus.IMPORTING).copy(manualImport = true)
        val completed = record(-43L, BrowserDownloadStatus.IMPORTED).copy(manualImport = true)
        val browser = record(44L, BrowserDownloadStatus.QUEUED)

        assertEquals(listOf(-41L, -42L), recoverableManualImportIds(listOf(queued, importing, completed, browser)))
    }

    @Test
    fun systemCompletionResumesOnlyQueuedOrTransferringDownloads() {
        assertTrue(shouldResumeBrowserDownloadAfterCompletion(BrowserDownloadStatus.QUEUED))
        assertTrue(shouldResumeBrowserDownloadAfterCompletion(BrowserDownloadStatus.DOWNLOADING))
        assertTrue(shouldResumeBrowserDownloadAfterCompletion(BrowserDownloadStatus.PAUSED))
        assertFalse(shouldResumeBrowserDownloadAfterCompletion(BrowserDownloadStatus.IMPORTING))
        assertFalse(shouldResumeBrowserDownloadAfterCompletion(BrowserDownloadStatus.IMPORTED))
        assertFalse(shouldResumeBrowserDownloadAfterCompletion(BrowserDownloadStatus.FAILED))
    }

    @Test
    fun failedUserInitiatedSchedulingIsVisibleAndRetryable() {
        val queued = record(31L, BrowserDownloadStatus.QUEUED).copy(downloadUrl = "https://example.test/game.zip")

        val failed = browserDownloadStartFailure(queued, "scheduler unavailable")

        assertEquals(BrowserDownloadStatus.FAILED, failed.status)
        assertEquals("scheduler unavailable", failed.error)
        assertEquals(queued.downloadUrl, failed.downloadUrl)
        assertEquals(queued.downloadId, failed.downloadId)
    }

    @Test
    fun persistedSchedulingErrorIsBoundedForTheDownloadQueue() {
        val failed = browserDownloadStartFailure(record(32L, BrowserDownloadStatus.QUEUED), "x".repeat(200))

        assertEquals(180, failed.error?.length)
    }

    @Test
    fun historyLimitRetainsOldActiveDownloadsAndOnlyNewestFinishedItems() {
        val oldActive = record(1L, BrowserDownloadStatus.DOWNLOADING)
        val terminal = (2L..105L).map { record(it, BrowserDownloadStatus.IMPORTED) }

        val retained = retainRecentBrowserDownloads(listOf(oldActive) + terminal)

        assertEquals(100, retained.size)
        assertTrue(retained.any { it.downloadId == oldActive.downloadId })
        assertFalse(retained.any { it.downloadId == 2L })
        assertTrue(retained.any { it.downloadId == 105L })
    }

    @Test
    fun historyLimitNeverDropsActiveItemsEvenWhenTheyExceedTheHistoryCap() {
        val active = (1L..105L).map { record(it, BrowserDownloadStatus.QUEUED) }

        assertEquals(active.size, retainRecentBrowserDownloads(active).size)
    }

    @Test
    fun newestManualImportIsRetainedDespiteItsNegativeWorkId() {
        val recentManual = record(-900L, BrowserDownloadStatus.IMPORTED).copy(
            manualImport = true,
            createdAtMillis = 10_000L,
        )
        val olderTransfers = (1L..101L).map { record(it, BrowserDownloadStatus.IMPORTED) }

        val retained = retainRecentBrowserDownloads(olderTransfers + recentManual)

        assertEquals(100, retained.size)
        assertTrue(retained.first() == recentManual)
        assertFalse(retained.any { it.downloadId == 1L })
    }

    @Test
    fun missingAndroidQueueRowRecoversNonEmptyStagedDownload() {
        val file = File.createTempFile("gamebox-download", ".zip")
        try {
            file.writeBytes(byteArrayOf(1, 2, 3))

            assertTrue(hasRecoverableStagedDownload(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun missingQueueRowDoesNotTreatAnEmptyOrMissingFileAsComplete() {
        val empty = File.createTempFile("gamebox-download", ".zip")
        try {
            assertFalse(hasRecoverableStagedDownload(empty))
            assertFalse(hasRecoverableStagedDownload(File(empty.parentFile, "missing-gamebox-download.zip")))
        } finally {
            empty.delete()
        }
    }

    private fun record(id: Long, status: BrowserDownloadStatus) = BrowserDownloadRecord(
        downloadId = id,
        stagedName = "$id.zip",
        displayName = "game-$id.zip",
        gameId = "game-$id",
        title = "Game $id",
        platformName = "PSP",
        platformId = "psp",
        status = status,
    )
}
