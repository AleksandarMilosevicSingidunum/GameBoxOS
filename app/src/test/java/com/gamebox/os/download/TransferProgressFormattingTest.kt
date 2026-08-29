package com.gamebox.os.download

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferProgressFormattingTest {
    @Test
    fun formatsKnownAndUnknownTotals() {
        assertEquals("1.0 MB of 2.0 MB", formatTransferProgress(1024L * 1024L, 2L * 1024L * 1024L))
        assertEquals("1.5 GB of 2.0 GB", formatTransferProgress(1536L * 1024L * 1024L, 2048L * 1024L * 1024L))
        assertEquals("0.5 MB", formatTransferProgress(512L * 1024L, -1L))
    }
}
