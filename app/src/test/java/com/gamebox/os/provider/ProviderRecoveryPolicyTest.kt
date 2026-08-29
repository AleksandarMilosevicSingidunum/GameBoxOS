package com.gamebox.os.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderRecoveryPolicyTest {
    @Test fun retriesTransientAndRateLimitedFailuresWithBoundedDelay() {
        val transient = ProviderRecoveryPolicy.classify(503, attempt = 2)
        assertTrue(transient.retryable); assertEquals(4_000L, transient.delayMillis)
        val limited = ProviderRecoveryPolicy.classify(429, attempt = 99)
        assertTrue(limited.retryable); assertEquals(64_000L, limited.delayMillis)
    }
    @Test fun doesNotRetryAuthenticationOrNotFound() {
        assertTrue(!ProviderRecoveryPolicy.classify(401).retryable)
        assertTrue(!ProviderRecoveryPolicy.classify(404).retryable)
    }
}
