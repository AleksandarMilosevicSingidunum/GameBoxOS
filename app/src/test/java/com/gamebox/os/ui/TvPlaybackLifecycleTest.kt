package com.gamebox.os.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvPlaybackLifecycleTest {
    @Test
    fun playbackResumesOnlyWhenRequestedAndNotExplicitlyClosed() {
        assertTrue(shouldResumeTvPlayback(playbackRequested = true, explicitlyClosed = false))
        assertFalse(shouldResumeTvPlayback(playbackRequested = false, explicitlyClosed = false))
        assertFalse(shouldResumeTvPlayback(playbackRequested = true, explicitlyClosed = true))
    }
}
