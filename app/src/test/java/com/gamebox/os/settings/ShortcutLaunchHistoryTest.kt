package com.gamebox.os.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutLaunchHistoryTest {
    @Test
    fun newestLaunchWinsAndPackagesAreDeduplicated() {
        val history = addShortcutLaunch(
            addShortcutLaunch(emptyList(), "com.example.media", 100L),
            "com.example.media",
            200L,
        )

        assertEquals(listOf(ShortcutLaunchRecord("com.example.media", 200L)), history)
    }

    @Test
    fun historyIsBoundedAndNewestFirst() {
        val history = (1L..20L).fold(emptyList<ShortcutLaunchRecord>()) { current, index ->
            addShortcutLaunch(current, "com.example.app$index", index)
        }

        assertEquals(12, history.size)
        assertEquals(20L, history.first().launchedAtEpochMs)
        assertEquals(9L, history.last().launchedAtEpochMs)
    }

    @Test
    fun decoderRejectsMalformedRowsAndRoundTripsValidHistory() {
        val valid = listOf(
            ShortcutLaunchRecord("com.limelight", 200L),
            ShortcutLaunchRecord("org.videolan.vlc", 100L),
        )
        val decoded = decodeShortcutLaunchHistory(
            encodeShortcutLaunchHistory(valid) +
                "\ninvalid package\t300\ncom.example.bad\tnot-a-time\ncom.example.zero\t0"
        )

        assertEquals(valid, decoded)
        assertTrue(decoded.none { it.packageName.contains(' ') })
    }

    @Test(expected = IllegalArgumentException::class)
    fun recordRejectsUnsafePackageNames() {
        addShortcutLaunch(emptyList(), "https://example.com", 100L)
    }
}
