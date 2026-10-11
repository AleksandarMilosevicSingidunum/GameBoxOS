package com.gamebox.os.importer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class RomImportProgressReporterTest {
    @Test
    fun deliversStructuredProgressToObserver() {
        var observed: RomImportProgress? = null
        val reporter = RomImportProgressReporter(Job()) { observed = it }

        reporter.report(RomImportStage.COPYING_ARCHIVE, 128L, 256L, 2, 3)

        assertEquals(
            RomImportProgress(RomImportStage.COPYING_ARCHIVE, 128L, 256L, 2, 3),
            observed,
        )
    }

    @Test
    fun ignoresOrdinaryObserverFailure() {
        val reporter = RomImportProgressReporter(Job()) { error("observer failed") }

        reporter.report(RomImportStage.EXTRACTING_ARCHIVE, 0L, null, 1, 1)
    }

    @Test
    fun propagatesObserverCancellation() {
        val reporter = RomImportProgressReporter(Job()) { throw CancellationException("cancel import") }

        try {
            reporter.report(RomImportStage.COPYING_AND_VERIFYING, 0L, null, 1, 1)
            fail("Cancellation must propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancel import", expected.message)
        }
    }

    @Test
    fun refusesToNotifyWhenImportContextIsCancelled() {
        var notified = false
        val job = Job().apply { cancel() }
        val reporter = RomImportProgressReporter(job) { notified = true }

        try {
            reporter.report(RomImportStage.COPYING_AND_VERIFYING, 0L, null, 1, 1)
            fail("Cancelled import context must propagate")
        } catch (_: CancellationException) {
            assertEquals(false, notified)
        }
    }
}
