package com.gamebox.os

import com.gamebox.os.catalog.CatalogTransport
import com.gamebox.os.catalog.CatalogTransportClient
import com.gamebox.os.catalog.CatalogTransportProvider
import com.gamebox.os.catalog.CatalogProviderConfig
import com.gamebox.os.catalog.CatalogParser
import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.InMemoryCatalogCredentialStore
import com.gamebox.os.catalog.NoopCatalogTransportClient
import com.gamebox.os.catalog.CatalogProvider
import com.gamebox.os.catalog.CatalogSnapshot
import com.gamebox.os.catalog.SelectingCatalogProvider
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogTransportProviderTest {
    @Test
    fun noopClientFailsExplicitly() {
        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking { NoopCatalogTransportClient().fetch(CatalogTransport.WebDav("https://example.test"), null) }
        }
    }

    @Test
    fun credentialStoreKeepsSecretsOutOfTransportConfig() {
        val store = InMemoryCatalogCredentialStore(mapOf("main" to CatalogCredentials(username = "u", password = "p")))
        assertEquals("u", store.credentials("main")?.username)
        assertEquals("https://example.test", (CatalogProviderConfig(CatalogTransport.WebDav("https://example.test"), "main").transport as CatalogTransport.WebDav).baseUrl)
    }
    @Test
    fun productionContractTestsConnectionAndResolvesAuthorizedSource() = runBlocking {
        val payload = """
            {
              "schemaVersion": 1,
              "provider": {"id": "authorized", "displayName": "Authorized Catalog"},
              "games": [{
                "id": "demo",
                "title": "Demo",
                "platform": "Homebrew",
                "year": 2026,
                "genre": "Arcade",
                "sizeMb": 1,
                "contentPolicy": "authorized",
                "source": "https://example.test/demo.nes",
                "checksum": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
              }, {
                "id": "metadata-only",
                "title": "Metadata",
                "platform": "Homebrew",
                "year": 2026,
                "genre": "Arcade",
                "sizeMb": 0,
                "contentPolicy": "metadata"
              }]
            }
        """.trimIndent()
        val provider = CatalogTransportProvider(
            client = object : CatalogTransportClient {
                override suspend fun fetch(
                    transport: CatalogTransport,
                    credentials: CatalogCredentials?,
                ): String = payload
            },
            parser = CatalogParser(),
            config = { CatalogProviderConfig(CatalogTransport.Https("https://example.test/catalog.json")) },
        )

        val connection = provider.testConnection()
        assertTrue(connection.success)
        assertEquals("authorized", connection.providerId)
        assertEquals(2, connection.gameCount)

        val downloadable = provider.resolveSource(GameId("demo"))
        assertTrue(downloadable.downloadable)
        assertEquals("https://example.test/demo.nes", downloadable.sourceUrl)

        val metadataOnly = provider.resolveSource(GameId("metadata-only"))
        assertFalse(metadataOnly.downloadable)
    }

    @Test
    fun connectionTestReturnsFailureInsteadOfThrowing() = runBlocking {
        val provider = CatalogTransportProvider(
            client = NoopCatalogTransportClient(),
            parser = CatalogParser(),
            config = { CatalogProviderConfig(CatalogTransport.Https("https://example.test/catalog.json")) },
        )

        val result = provider.testConnection()

        assertFalse(result.success)
        assertTrue(result.message.contains("No catalog transport client"))
    }

    @Test
    fun selectingProviderDelegatesToCurrentProductionTransport() = runBlocking {
        var selected = "HTTPS"
        fun provider(id: String) = object : CatalogProvider {
            override suspend fun load() = CatalogSnapshot(id, id, emptyList())
        }
        val provider = SelectingCatalogProvider(
            selected = { selected },
            providers = mapOf(
                "HTTPS" to provider("https"),
                "WEBDAV" to provider("webdav"),
                "S3" to provider("s3"),
            ),
        )

        assertEquals("https", provider.refresh().providerId)
        selected = "WEBDAV"
        assertEquals("webdav", provider.testConnection().providerId)
        selected = "S3"
        assertEquals("s3", provider.load().providerId)
    }

}