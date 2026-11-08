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

    @Test
    fun failedLookupIsRetriedOnNextRequest() = runBlocking {
        var providerAvailable = false
        var requests = 0
        val client = WikidataMetadataClient(connectionFactory = { url ->
            requests++
            val body = when {
                url.query.orEmpty().contains("wbsearchentities") && providerAvailable -> search
                url.query.orEmpty().contains("wbsearchentities") -> "{\"search\":[]}"
                else -> entity
            }
            StubConnection(url, body)
        })
        val game = game(title = "Galaxy Patrol", description = null, artwork = null)

        assertEquals(game, client.enrich(game))
        providerAvailable = true
        assertEquals(1994, client.enrich(game).year)
        assertEquals("A failed lookup must not be cached", 3, requests)
    }

    @Test
    fun successfulLookupCacheEvictsLeastRecentlyUsedTitles() = runBlocking {
        var requests = 0
        val client = WikidataMetadataClient(connectionFactory = { url ->
            requests++
            val body = if (url.query.orEmpty().contains("wbsearchentities")) {
                val encoded = url.query.orEmpty().substringAfter("search=").substringBefore('&')
                val title = java.net.URLDecoder.decode(encoded, "UTF-8")
                """{"search":[{"id":"Q123","label":"$title"}]}"""
            } else entity
            StubConnection(url, body)
        })
        val titles = (0..128).map { "Game $it" }

        titles.forEach { title -> client.enrich(game(title, description = null, artwork = null)) }
        val requestsAfterFill = requests
        client.enrich(game(titles.first(), description = null, artwork = null))

        assertEquals(258, requestsAfterFill)
        assertEquals("The least recently used title should be looked up again", 260, requests)
    }

    @Test
    fun exposesExactWikidataMatchForExplicitMetadataSelection() = runBlocking {
        val client = WikidataMetadataClient(connectionFactory = { url ->
            StubConnection(url, if (url.query.orEmpty().contains("wbsearchentities")) search else entity)
        })

        val candidate = client.findCandidates(game(title = "Galaxy Patrol", description = null, artwork = null)).single()

        assertEquals("Q123", candidate.externalId)
        assertEquals("Galaxy Patrol", candidate.title)
        assertEquals("WIKIDATA", candidate.provider)
        assertEquals(1994, candidate.year)
        assertEquals("A video game.", candidate.description)
    }

    @Test
    fun rejectsNonGameAndAmbiguousEntitiesForAutomaticEnrichment() = runBlocking {
        val search = """{"search":[{"id":"Q123","label":"Galaxy Patrol"},{"id":"Q124","label":"Galaxy Patrol"},{"id":"Q125","label":"Galaxy Patrol"}]}"""
        val entities = """{"entities":{"Q123":${gameEntity("Q123")},"Q124":${gameEntity("Q124")},"Q125":${gameEntity("Q125", "Q5")}}}"""
        val client = WikidataMetadataClient(connectionFactory = { url ->
            StubConnection(url, when {
                url.query.orEmpty().contains("wbsearchentities") -> search
                else -> entities
            })
        })
        val original = game("Galaxy Patrol", description = null, artwork = null)

        assertEquals(original, client.enrich(original))
        val candidates = client.findCandidates(original)
        assertEquals(listOf("Q123", "Q124"), candidates.map { it.externalId })
    }

    @Test
    fun existingYearDisambiguatesExactTitleGameMatches() = runBlocking {
        val search = """{"search":[{"id":"Q123","label":"Galaxy Patrol"},{"id":"Q124","label":"Galaxy Patrol"}]}"""
        val first = gameEntity("Q123", year = 1994)
        val second = gameEntity("Q124", year = 2001)
        val client = WikidataMetadataClient(connectionFactory = { url ->
            StubConnection(url, if (url.query.orEmpty().contains("wbsearchentities")) search else
                """{"entities":{"Q123":$first,"Q124":$second}}""")
        })

        val result = client.enrich(game("Galaxy Patrol", description = null, artwork = null).copy(year = 2001))

        assertEquals(2001, result.year)
        assertEquals("Q124", result.metadataExternalId)
    }

    @Test
    fun exactPlatformDisambiguatesSameTitleAndIsShownInManualCandidates() = runBlocking {
        val search = """{"search":[{"id":"Q123","label":"Galaxy Patrol"},{"id":"Q124","label":"Galaxy Patrol"}]}"""
        val entities = """{"entities":{"Q123":${gameEntity("Q123", year = 1994, platform = "QNES")},"Q124":${gameEntity("Q124", year = 2001, platform = "QPSP")}}}"""
        val platformEntities = """{"entities":{"QNES":{"labels":{"en":{"value":"Nintendo Entertainment System"}}},"QPSP":{"labels":{"en":{"value":"PlayStation Portable"}}}}}"""
        val client = WikidataMetadataClient(connectionFactory = { url ->
            val query = url.query.orEmpty()
            StubConnection(url, when {
                query.contains("wbsearchentities") -> search
                query.contains("QNES") || query.contains("QPSP") -> platformEntities
                else -> entities
            })
        })
        val game = game("Galaxy Patrol", description = null, artwork = null).copy(platform = "NES")

        val result = client.enrich(game)
        val candidates = client.findCandidates(game)

        assertEquals("Q123", result.metadataExternalId)
        assertEquals(listOf("Nintendo Entertainment System", "PlayStation Portable"), candidates.map { it.platform })
        val otherPlatform = game.copy(platform = "Nintendo DS")
        assertEquals(otherPlatform, client.enrich(otherPlatform))
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
        fun gameEntity(id: String, type: String = "Q7889", year: Int = 1994, platform: String? = null) = """
            {"id":"$id","claims":{"P31":[{"mainsnak":{"datavalue":{"value":{"id":"$type"}}}}],
              "P577":[{"mainsnak":{"datavalue":{"value":{"time":"+$year-01-01T00:00:00Z"}}}}],
              "P18":[{"mainsnak":{"datavalue":{"value":"Galaxy Patrol cover.png"}}}],
              "P400":[${platform?.let { "{\"mainsnak\":{\"datavalue\":{\"value\":{\"id\":\"$it\"}}}}" } ?: ""}]},
              "descriptions":{"en":{"value":"A video game."}}}
        """.trimIndent()
        const val search = """
            {"search":[{"id":"Q123","label":"Galaxy Patrol"}]}
        """
        const val entity = """
            {"entities":{"Q123":{"claims":{"P31":[{"mainsnak":{"datavalue":{"value":{"id":"Q7889"}}}}],
              "P577":[{"mainsnak":{"datavalue":{"value":{"time":"+1994-01-01T00:00:00Z"}}}}],
              "P18":[{"mainsnak":{"datavalue":{"value":"Galaxy Patrol cover.png"}}}]
            },"descriptions":{"en":{"value":"A video game."}}}}}
        """
    }
}
