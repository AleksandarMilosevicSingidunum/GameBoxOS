package com.gamebox.os.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalFullscreenRetryPolicyTest {
    @Test
    fun fullscreenRetriesAreBoundedAndBackOff() {
        assertEquals(500L, externalFullscreenRetryDelayMillis(0))
        assertEquals(1_200L, externalFullscreenRetryDelayMillis(1))
        assertNull(externalFullscreenRetryDelayMillis(2))
    }

    @Test
    fun onlyTopFocusRaceIsRetried() {
        assertTrue(isRetryableExternalFullscreenError(
            IllegalStateException("The window is not the top focused window."),
        ))
        assertFalse(isRetryableExternalFullscreenError(
            IllegalStateException("The window is already fully expanded."),
        ))
        assertFalse(isRetryableExternalFullscreenError(IllegalArgumentException("bad request")))
    }
}
