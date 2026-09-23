package com.gamebox.os.catalog

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CatalogRefreshDelegationTest {
    @Test fun configuredAndMetadataDecoratorsUseFreshProviderOperation() = runBlocking {
        var loads = 0
        var refreshes = 0
        var fail = false
        val remote = object : CatalogProvider {
            override suspend fun load(): CatalogSnapshot {
                loads++
                return CatalogSnapshot("cached", "Cached", emptyList())
            }
            override suspend fun refresh(): CatalogSnapshot {
                refreshes++
                if (fail) error("Authentication required")
                return CatalogSnapshot("fresh", "Fresh", emptyList())
            }
        }
        val fallback = object : CatalogProvider {
            override suspend fun load() = CatalogSnapshot("fallback", "Fallback", emptyList())
        }
        val configured = ConfiguredCatalogProvider(fallback, remote, { "https://example.test/catalog.json" })
        val provider = MetadataEnrichingCatalogProvider(configured) { it }
        assertEquals("Fresh", provider.refresh().providerName)
        assertEquals(0, loads)
        assertEquals(1, refreshes)
        fail = true
        assertEquals("Fallback", provider.refresh().providerName)
        assertEquals(CatalogFallbackReason.REMOTE_FAILURE, provider.consumeFallbackReason())
        assertFalse(provider.testConnection().success)
        assertEquals(0, loads)
        assertEquals(3, refreshes)
    }

    @Test fun connectionCancellationDoesNotBecomeAnAuthenticationResult() = runBlocking {
        val provider = object : CatalogProvider {
            override suspend fun load(): CatalogSnapshot = throw CancellationException("cancelled")
        }
        val error = runCatching { provider.testConnection() }.exceptionOrNull()
        assertTrue(error is CancellationException)
    }
}
