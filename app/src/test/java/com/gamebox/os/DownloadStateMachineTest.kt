package com.gamebox.os

import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.GameId
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStateMachineTest {
    @Test fun progress_isClampedAndHandlesUnknownSize() {
        assertEquals(0f, job(total = 0, downloaded = 20).progress)
        assertEquals(1f, job(total = 10, downloaded = 20).progress)
        assertEquals(0.5f, job(total = 10, downloaded = 5).progress)
    }

    @Test fun downloadStatuses_areExplicitAndStable() {
        assertEquals(
            listOf("QUEUED", "DOWNLOADING", "PAUSED", "VERIFYING", "INSTALLING", "COMPLETED", "FAILED", "CANCELLED"),
            DownloadStatus.entries.map { it.name }
        )
    }

    private fun job(total: Long, downloaded: Long) = DownloadJob(
        id = "job", gameId = GameId("game"), title = "Game",
        status = DownloadStatus.DOWNLOADING, totalBytes = total, downloadedBytes = downloaded
    )
}
