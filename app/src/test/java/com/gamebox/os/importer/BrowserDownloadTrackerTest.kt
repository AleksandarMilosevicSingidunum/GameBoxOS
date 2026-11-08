package com.gamebox.os.importer

import android.app.DownloadManager
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserDownloadTrackerTest {
    @Test
    fun pendingSystemRequestIsShownAsQueued() {
        assertEquals(BrowserDownloadStatus.QUEUED, browserDownloadTransferStatus(DownloadManager.STATUS_PENDING))
    }

    @Test
    fun runningAndPausedSystemRequestsKeepTheirOwnStates() {
        assertEquals(BrowserDownloadStatus.DOWNLOADING, browserDownloadTransferStatus(DownloadManager.STATUS_RUNNING))
        assertEquals(BrowserDownloadStatus.PAUSED, browserDownloadTransferStatus(DownloadManager.STATUS_PAUSED))
    }
}
