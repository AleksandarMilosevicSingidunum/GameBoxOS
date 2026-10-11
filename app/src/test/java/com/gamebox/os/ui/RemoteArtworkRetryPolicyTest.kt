package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteArtworkRetryPolicyTest {
    @Test
    fun retriesTransientImageFailuresWithBoundedBackoff() {
        assertEquals(listOf(0L, 1_500L, 4_000L), artworkRetryDelaysMillis())
        assertEquals(listOf(0L), artworkRetryDelaysMillis(maxAttempts = 1))
        assertEquals(emptyList<Long>(), artworkRetryDelaysMillis(maxAttempts = 0))
    }
}
