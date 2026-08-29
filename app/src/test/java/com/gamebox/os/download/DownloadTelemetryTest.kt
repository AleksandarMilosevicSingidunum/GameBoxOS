package com.gamebox.os.download

import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class DownloadTelemetryTest {
    private val mib = 1024L * 1024L

    @Test
    fun trackerMeasuresSpeedAndEtaFromProgressDeltas() {
        val tracker = DownloadTelemetryTracker()
        val initial = job(downloaded = 0L, total = 4L * mib)

        assertNull(tracker.sample(initial, 1_000L))
        val telemetry = tracker.sample(initial.copy(downloadedBytes = mib), 2_000L)

        assertNotNull(telemetry)
        assertEquals(mib, telemetry!!.bytesPerSecond)
        assertEquals(3L, telemetry.etaSeconds)
        assertEquals("1.0 MB/s - 3 sec remaining", formatDownloadTelemetry(telemetry))
    }

    @Test
    fun trackerResetsAcrossPauseAndByteRegression() {
        val tracker = DownloadTelemetryTracker()
        val active = job(downloaded = mib, total = 4L * mib)
        assertNull(tracker.sample(active, 1_000L))
        assertNotNull(tracker.sample(active.copy(downloadedBytes = 2L * mib), 2_000L))

        assertNull(tracker.sample(active.copy(status = DownloadStatus.PAUSED), 3_000L))
        assertNull(tracker.sample(active.copy(downloadedBytes = mib / 2L), 4_000L))
    }

    @Test
    fun capacityAssessmentIncludesRemainingBytesAndSafetyReserve() {
        val reserve = 128L * mib
        val active = job(downloaded = mib, total = 10L * mib)
        val required = 9L * mib + reserve

        assertNull(assessDownloadCapacity(active, required, reserve))
        val warning = assessDownloadCapacity(active, required - 2L * mib, reserve)

        assertNotNull(warning)
        assertEquals(2L * mib, warning!!.shortfallBytes)
        assertEquals("Low storage: 2.0 MB more required", formatCapacityWarning(warning))
    }

    @Test
    fun terminalJobsDoNotShowCapacityWarnings() {
        val completed = job(downloaded = mib, total = mib)
            .copy(status = DownloadStatus.COMPLETED)

        assertNull(assessDownloadCapacity(completed, 0L, reserveBytes = 128L * mib))
    }

    private fun job(downloaded: Long, total: Long) = DownloadJob(
        id = "job",
        gameId = GameId("game"),
        title = "Game",
        status = DownloadStatus.DOWNLOADING,
        totalBytes = total,
        downloadedBytes = downloaded
    )
}
