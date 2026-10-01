package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WikidataMetadataClientTest {
    @Test
    fun exactTitleEnrichesOnlyMissingFields() = runBlocking {
        val client = WikidataMetadataClient(connectionFactory = { url ->
            StubConnection(url, if (url.query.orEmpty().contains("wbsearchentities")) search else entity)
        })

        val result = client.enrich(game(title = "Galaxy Patrol", description = null, artwork = null))

        assertEquals(1994, result.year)
        assertEquals("A video game.", result.description)
        assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/Galaxy_Patrol_cover.png",
            result.artworkUrl,
        )
        assertEquals("WIKIDATA", result.metadataProvider)
        assertEquals("Q123", result.metadataExternalId)
    }

    @Test
    fun existingCatalogFieldsWinOverFallback() = runBlocking {
        val client = WikidataMetadataClient(connectionFactory = { url ->
            StubConnection(url, if (url.query.orEmpty().contains("wbsearchentities")) search else entity)
        })

        val result = client.enrich(game(title = "Galaxy Patrol", description = "Curated", artwork = "https://example.test/cover.png"))

        assertEquals(2001, result.year)
        assertEquals("Curated", result.description)
        assertEquals("https://example.test/cover.png", result.artworkUrl)
    }

    @Test
    fun noExactTitleLeavesGameUntouched() = runBlocking {
        val client = WikidataMetadataClient(connectionFactory = { url ->
            StubConnection(url, "{\"search\":[]}")
        })

        val result = client.enrich(game(title = "Missing Game", description = null, artwork = null))

        assertEquals(0, result.year)
        assertNull(result.description)
        assertNull(result.metadataProvider)
    }

    private fun game(title: String, description: String?, artwork: String?) = Game(
        id = GameId("test-game"), title = title, platform = "NES", year = if (description == "Curated") 2001 else 0,
        genre = "", sizeMb = 1, state = InstallState.NOT_INSTALLED,
        description = description, artworkUrl = artwork,
    )

    private class StubConnection(url: URL, private val body: String) : HttpsURLConnection(url) {
        override fun getResponseCode() = HTTP_OK
        override fun getInputStream(): InputStream = ByteArrayInputStream(body.toByteArray())
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite(): String? = null
        override fun getLocalCertificates() = null
        override fun getServerCertificates() = null
        override fun getPeerPrincipal() = null
        override fun getLocalPrincipal() = null
    }

    private companion object {
        const val search = """
            {"search":[{"id":"Q123","label":"Galaxy Patrol"}]}
        """
        const val entity = """
            {"entities":{"Q123":{"claims":{
              "P577":[{"mainsnak":{"datavalue":{"value":{"time":"+1994-01-01T00:00:00Z"}}}}],
              "P18":[{"mainsnak":{"datavalue":{"value":"Galaxy Patrol cover.png"}}}]
            },"descriptions":{"en":{"value":"A video game."}}}}}
        """
    }
}
