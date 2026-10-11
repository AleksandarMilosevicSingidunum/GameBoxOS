package com.gamebox.os.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RomImportProgressTest {
    @Test
    fun computesAndClampsKnownByteProgress() {
        val progress = RomImportProgress(
            stage = RomImportStage.COPYING_AND_VERIFYING,
            bytesProcessed = 150,
            totalBytes = 100,
            fileIndex = 1,
            fileCount = 2,
        )

        assertEquals(1f, progress.fraction)
    }

    @Test
    fun keepsUnknownLengthProgressIndeterminate() {
        val progress = RomImportProgress(
            stage = RomImportStage.EXTRACTING_ARCHIVE,
            bytesProcessed = 4096,
            totalBytes = null,
            fileIndex = 1,
            fileCount = 1,
        )

        assertNull(progress.fraction)
    }
}
