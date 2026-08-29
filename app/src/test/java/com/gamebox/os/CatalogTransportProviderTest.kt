package com.gamebox.os

import com.gamebox.os.catalog.CatalogTransport
import com.gamebox.os.catalog.CatalogTransportClient
import com.gamebox.os.catalog.CatalogTransportProvider
import com.gamebox.os.catalog.CatalogProviderConfig
import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.InMemoryCatalogCredentialStore
import com.gamebox.os.catalog.NoopCatalogTransportClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogTransportProviderTest {
    @Test
    fun noopClientFailsExplicitly() = runBlocking {
        assertThrows(UnsupportedOperationException::class.java) {
            NoopCatalogTransportClient().fetch(CatalogTransport.WebDav("https://example.test"), null)
        }
    }

    @Test
    fun credentialStoreKeepsSecretsOutOfTransportConfig() {
        val store = InMemoryCatalogCredentialStore(mapOf("main" to CatalogCredentials(username = "u", password = "p")))
        assertEquals("u", store.credentials("main")?.username)
        assertEquals("https://example.test", CatalogProviderConfig(CatalogTransport.WebDav("https://example.test"), "main").transport.baseUrl)
    }
}