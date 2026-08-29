package com.gamebox.os.catalog

import com.gamebox.os.provider.ProviderFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CatalogTransportRecoveryTest {
    @Test fun authenticationFailureIsTypedAndNotRetryable() {
        val error = catalogTransportFailure(status = 401)
        assertEquals(ProviderFailureKind.AUTHENTICATION, error.recovery.kind)
        assertTrue(!error.recovery.retryable)
    }
    @Test fun serverAndIoFailuresAreRetryable() {
        assertTrue(catalogTransportFailure(status = 503).recovery.retryable)
        assertTrue(catalogTransportFailure(error = IOException("offline")).recovery.retryable)
    }
}
