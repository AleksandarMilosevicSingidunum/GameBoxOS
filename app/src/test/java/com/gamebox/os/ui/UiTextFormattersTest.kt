package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextFormattersTest {
    @Test
    fun formatsSmallDownloadsWithoutRoundingThemToZeroMegabytes() {
        assertEquals("0 B", formatDownloadBytes(0L))
        assertEquals("512 B", formatDownloadBytes(512L))
        assertEquals("1.0 KB", formatDownloadBytes(1024L))
        assertEquals("48.0 KB", formatDownloadBytes(49_168L))
        assertEquals("1.0 MB", formatDownloadBytes(1024L * 1024L))
    }

    @Test
    fun formatsTransferRatesForDownloadQueue() {
        assertEquals("0 B/s", formatTransferRate(0L))
        assertEquals("512 B/s", formatTransferRate(512L))
        assertEquals("2 KB/s", formatTransferRate(2L * 1024L))
        assertEquals("1.5 MB/s", formatTransferRate(1536L * 1024L))
        assertEquals("0 B/s", formatTransferRate(-10L))
    }
}
