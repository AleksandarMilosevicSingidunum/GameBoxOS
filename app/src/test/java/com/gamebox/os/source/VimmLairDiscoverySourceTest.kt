package com.gamebox.os.source

import java.net.URI
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class VimmLairDiscoverySourceTest {
    @Test
    fun platformMappingsCoverGameBoxTargetConsoles() {
        assertEquals("PS2", vimmLairPlatformSlug("PS2"))
        assertEquals("GameCube", vimmLairPlatformSlug("Nintendo GameCube"))
        assertEquals("Wii", vimmLairPlatformSlug("Wii"))
        assertEquals("PSP", vimmLairPlatformSlug("PlayStation Portable"))
        assertEquals("Dreamcast", vimmLairPlatformSlug("Sega Dreamcast"))
        assertEquals(null, vimmLairPlatformSlug("Switch"))
        assertEquals(null, vimmLairPlatformSlug("3DS"))
    }

    @Test
    fun globalSearchUsesConfiguredSupportedPlatformsWithBound() {
        val config = GameSourceConfig(
            id = "vimm",
            name = "Vimm",
            type = GameSourceProviderType.VIMM_LAIR,
            baseUrl = "https://vimm.net/vault",
            platforms = setOf("Wii", "PS2", "Switch", "PSP", "GameCube"),
        )

        assertEquals(
            listOf("GameCube", "PS2", "PSP", "Wii"),
            vimmLairSearchPlatforms(config, selectedPlatform = null),
        )
        assertEquals(
            listOf("PS2"),
            vimmLairSearchPlatforms(config, selectedPlatform = "PS2"),
        )
        assertTrue(vimmLairSearchPlatforms(config, selectedPlatform = "Switch").isEmpty())
        assertEquals(
            listOf("GameCube", "PS2"),
            vimmLairSearchPlatforms(config, selectedPlatform = null, maxPlatforms = 2),
        )
    }


    @Test
    fun unrestrictedGlobalSearchUsesGameBoxTargetDefaults() {
        val config = GameSourceConfig(
            id = "vimm",
            name = "Vimm",
            type = GameSourceProviderType.VIMM_LAIR,
            baseUrl = "https://vimm.net/vault",
        )

        assertEquals(
            listOf("Dreamcast", "GameCube", "PS2", "PSP", "Wii"),
            vimmLairSearchPlatforms(config, selectedPlatform = null),
        )
    }


    @Test
    fun multiConsoleSearchKeepsSuccessfulResultsWhenOnePlatformFails() = runBlocking {
        val config = GameSourceConfig(
            id = "vimm",
            name = "Vimm",
            type = GameSourceProviderType.VIMM_LAIR,
            baseUrl = "https://vimm.net/vault",
            platforms = setOf("PS2", "GameCube"),
        )
        val summary = searchVimmLairAcrossPlatforms(
            config = config,
            query = "God",
            transport = VimmLairTransport { uri ->
                when {
                    uri.path.contains("/GameCube/") -> throw java.io.IOException("temporary")
                    uri.path.contains("/PS2/") -> """<a href="/vault/100">God Hand</a>"""
                    else -> ""
                }
            },
        )

        assertEquals(listOf("GameCube", "PS2"), summary.attemptedPlatforms)
        assertEquals(listOf("GameCube"), summary.failedPlatforms)
        assertEquals(1, summary.games.size)
        assertEquals("PS2", summary.games.single().platform)
        assertEquals("God Hand", summary.games.single().title)
    }


    @Test
    fun browseUrlUsesPlatformAndAlphabetBucket() {
        assertEquals(
            "https://vimm.net/vault/PS2/G",
            vimmLairBrowseUrl("https://vimm.net/vault", "PS2", "God of War"),
        )
        assertEquals(
            "https://vimm.net/vault/Xbox/0-9",
            vimmLairBrowseUrl("https://vimm.net/vault", "Xbox", "007"),
        )
    }

    @Test
    fun parserKeepsOnlyMatchingVaultGameLinks() {
        val html = """
            <html><body>
              <table class="hovertable">
                <tr><td><a href="/vault/1234">God of War</a></td></tr>
                <tr><td><a href="/vault/5678"><span>God of War II</span></a></td></tr>
                <tr><td><a href="/vault/9999">Gran Turismo 4</a></td></tr>
                <tr><td><a href="/manual/1234">God of War Manual</a></td></tr>
              </table>
            </body></html>
        """.trimIndent()

        val games = parseVimmLairListing(
            html = html,
            sourceId = "vimm",
            platform = "PS2",
            baseUrl = "https://vimm.net/vault",
            query = "god of war",
        )

        assertEquals(2, games.size)
        assertEquals(listOf("1234", "5678"), games.map { it.externalId })
        assertEquals(
            "https://vimm.net/vault/1234",
            games.first().detailsUrl,
        )
    }

    @Test
    fun parserDecodesCommonEntitiesAndDeduplicatesIds() {
        val html = """
            <a href='/vault/42'>Tom &amp; Jerry</a>
            <a href='/vault/42'>Tom &amp; Jerry</a>
        """.trimIndent()

        val games = parseVimmLairListing(
            html,
            sourceId = "vimm",
            platform = "GameCube",
            baseUrl = "https://vimm.net/vault",
            query = "Tom & Jerry",
        )

        assertEquals(1, games.size)
        assertEquals("Tom & Jerry", games.single().title)
    }

    @Test
    fun sourceSearchFetchesOnlyOneBoundedCategoryPage() = runBlocking {
        var requested: URI? = null
        val source = VimmLairDiscoverySource(
            GameSourceConfig(
                id = "vimm",
                name = "Vimm's Lair",
                type = GameSourceProviderType.VIMM_LAIR,
                baseUrl = "https://vimm.net/vault",
                platforms = setOf("PS2", "GameCube"),
            ),
            VimmLairTransport { uri ->
                requested = uri
                """<a href="/vault/100">God Hand</a>"""
            },
        )

        val page = source.search(platform = "PS2", query = "God Hand")

        assertEquals("https://vimm.net/vault/PS2/G", requested.toString())
        assertEquals(1, page.games.size)
        assertEquals("God Hand", page.games.single().title)
    }

    @Test
    fun detailsParserExtractsSafeMetadataWithoutDownloadFields() {
        val html = """
            <html>
              <head>
                <meta property="og:title" content="God of War - Vimm's Lair">
                <meta property="og:image" content="https://vimm.net/image.php?id=1234">
                <title>Fallback title - Vimm's Lair</title>
              </head>
              <body>
                <table>
                  <tr><td>System</td><td>Sony PlayStation 2</td></tr>
                  <tr><td>Region</td><td>USA</td></tr>
                  <tr><td>Release Date</td><td>March 22, 2005</td></tr>
                  <tr><td>Media ID</td><td>should-not-be-exposed</td></tr>
                </table>
                <input name="mediaId" value="99999">
              </body>
            </html>
        """.trimIndent()

        val game = parseVimmLairDetails(
            html = html,
            sourceId = "vimm",
            externalId = "1234",
            detailsUrl = "https://vimm.net/vault/1234",
        )

        assertEquals("God of War", game.title)
        assertEquals("Sony PlayStation 2", game.platform)
        assertEquals("USA", game.region)
        assertEquals(2005, game.year)
        assertEquals("https://vimm.net/image.php?id=1234", game.coverUrl)
        assertEquals("https://vimm.net/vault/1234", game.detailsUrl)
    }

    @Test
    fun hydratePreservesSearchIdentityWhenDetailPageIsSparse() = runBlocking {
        val result = DiscoverySourceGame(
            sourceId = "vimm",
            externalId = "1234",
            title = "God of War",
            platform = "PS2",
            region = "USA",
            detailsUrl = "https://vimm.net/vault/1234",
        )
        val source = VimmLairDiscoverySource(
            GameSourceConfig(
                id = "vimm",
                name = "Vimm's Lair",
                type = GameSourceProviderType.VIMM_LAIR,
                baseUrl = "https://vimm.net/vault",
            ),
            VimmLairTransport { uri ->
                assertEquals("https://vimm.net/vault/1234", uri.toString())
                "<html><head><title>Vimm's Lair</title></head><body></body></html>"
            },
        )

        val hydrated = source.hydrate(result)

        assertEquals(result.externalId, hydrated.externalId)
        assertEquals("God of War", hydrated.title)
        assertEquals("PS2", hydrated.platform)
        assertEquals("USA", hydrated.region)
        assertEquals("https://vimm.net/vault/1234", hydrated.detailsUrl)
    }

    @Test
    fun detailsRejectsArtworkOutsideVimmHost() {
        val html = """
            <meta property="og:title" content="Example - Vimm's Lair">
            <meta property="og:image" content="https://evil.example/cover.jpg">
        """.trimIndent()

        val game = parseVimmLairDetails(
            html = html,
            sourceId = "vimm",
            externalId = "5",
            detailsUrl = "https://vimm.net/vault/5",
        )

        assertEquals(null, game.coverUrl)
    }

    @Test
    fun probeUsesOneConfiguredPlatformAndRequiresVaultStructure() = runBlocking {
        var requested: URI? = null
        val source = VimmLairDiscoverySource(
            GameSourceConfig(
                id = "vimm",
                name = "Vimm",
                type = GameSourceProviderType.VIMM_LAIR,
                baseUrl = "https://vimm.net/vault",
                platforms = setOf("PS2", "GameCube"),
            ),
            VimmLairTransport { uri ->
                requested = uri
                """
                    <html><body>
                      <a href="/vault/100">Alpha</a>
                      <a href="/vault/101">Another</a>
                    </body></html>
                """.trimIndent()
            },
        )

        val probe = source.probe()

        assertEquals("GameCube", probe.platform)
        assertEquals("https://vimm.net/vault/GameCube/A", requested.toString())
        assertEquals(2, probe.vaultLinks)
    }

    @Test
    fun probeFailsClosedWhenExpectedListingStructureIsMissing() {
        val source = VimmLairDiscoverySource(
            GameSourceConfig(
                id = "vimm",
                name = "Vimm",
                type = GameSourceProviderType.VIMM_LAIR,
                baseUrl = "https://vimm.net/vault",
            ),
            VimmLairTransport { "<html><body>maintenance</body></html>" },
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { source.probe("PS2") }
        }
    }

    @Test
    fun cachedTransportReusesFreshResponse() = runBlocking {
        var requests = 0
        var now = 1_000L
        val cache = VimmLairPageCache(maxEntries = 4, ttlMillis = 5_000L) { now }
        val uri = URI("https://vimm.net/vault/PS2/A")
        val transport = CachedVimmLairTransport(
            delegate = VimmLairTransport {
                requests += 1
                "body-" + requests
            },
            cache = cache,
        )

        assertEquals("body-1", transport.get(uri))
        assertEquals("body-1", transport.get(uri))
        assertEquals(1, requests)

        now = 7_000L
        assertEquals("body-2", transport.get(uri))
        assertEquals(2, requests)
    }

    @Test
    fun pageCacheEvictsLeastRecentlyUsedEntry() {
        var now = 1_000L
        val cache = VimmLairPageCache(maxEntries = 2, ttlMillis = 10_000L) { now }
        val first = URI("https://vimm.net/vault/PS2/A")
        val second = URI("https://vimm.net/vault/PS2/B")
        val third = URI("https://vimm.net/vault/PS2/C")

        cache.put(first, "A")
        cache.put(second, "B")
        assertEquals("A", cache.get(first))
        cache.put(third, "C")

        assertEquals("A", cache.get(first))
        assertEquals(null, cache.get(second))
        assertEquals("C", cache.get(third))
    }

    @Test
    fun sourceRejectsUnsupportedConsoleAndNonVimmHost() {
        assertThrows(IllegalArgumentException::class.java) {
            VimmLairDiscoverySource(
                GameSourceConfig(
                    id = "vimm",
                    name = "Vimm",
                    type = GameSourceProviderType.VIMM_LAIR,
                    baseUrl = "https://example.test/vault",
                )
            )
        }

        val source = VimmLairDiscoverySource(
            GameSourceConfig(
                id = "vimm",
                name = "Vimm",
                type = GameSourceProviderType.VIMM_LAIR,
                baseUrl = "https://vimm.net/vault",
            ),
            VimmLairTransport { "" },
        )
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { source.search(platform = "Switch", query = "Mario") }
        }
    }

    @Test
    fun emptyQueryAndMissingPlatformFailBeforeNetwork() {
        var calls = 0
        val source = VimmLairDiscoverySource(
            GameSourceConfig(
                id = "vimm",
                name = "Vimm",
                type = GameSourceProviderType.VIMM_LAIR,
                baseUrl = "https://vimm.net/vault",
            ),
            VimmLairTransport {
                calls += 1
                ""
            },
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { source.search(platform = "PS2", query = "") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { source.search(platform = null, query = "God") }
        }
        assertTrue(calls == 0)
    }
}
