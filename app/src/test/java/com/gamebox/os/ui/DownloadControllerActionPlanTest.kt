package com.gamebox.os.ui

import com.gamebox.os.domain.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadControllerActionPlanTest {
    @Test
    fun emptyQueueOffersStoreAndSettings() {
        assertEquals(
            DownloadControllerActionPlan(
                "Browse Store",
                DownloadControllerAction.OPEN_STORE,
                "Settings",
                DownloadControllerAction.OPEN_SETTINGS,
            ),
            downloadControllerActionPlan(emptyList()),
        )
    }

    @Test
    fun failedTransfersPrioritizeRetry() {
        val plan = downloadControllerActionPlan(
            listOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED),
        )

        assertEquals("Retry failed", plan.xLabel)
        assertEquals(DownloadControllerAction.RETRY_FAILED, plan.xAction)
    }

    @Test
    fun activeTransfersPrioritizePauseOverPausedTransfers() {
        val plan = downloadControllerActionPlan(
            listOf(DownloadStatus.PAUSED, DownloadStatus.DOWNLOADING),
        )

        assertEquals("Pause all", plan.yLabel)
        assertEquals(DownloadControllerAction.PAUSE_ACTIVE, plan.yAction)
    }

    @Test
    fun pausedTransfersOfferBatchResumeWhenNothingIsActive() {
        val plan = downloadControllerActionPlan(listOf(DownloadStatus.PAUSED))

        assertEquals("Resume all", plan.yLabel)
        assertEquals(DownloadControllerAction.RESUME_PAUSED, plan.yAction)
    }

    @Test
    fun cancelledTransfersCanBeRetried() {
        assertEquals(
            DownloadControllerAction.RETRY_FAILED,
            downloadControllerActionPlan(listOf(DownloadStatus.CANCELLED)).xAction,
        )
    }
}
