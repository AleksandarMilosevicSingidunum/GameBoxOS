package com.gamebox.os.source

import com.gamebox.os.domain.normalizeCatalogTitle
import com.gamebox.os.importer.RomImportPolicy
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class VimmLairDiscoverySourceTest {
    @Test
    fun defaultPresetMatchesRequestedConsoleOrder() {
        assertEquals(
            listOf(
                "Dreamcast", "Game Boy Advance", "Game Boy Color", "GameCube", "N64", "NES",
                "PS1", "PS2", "PS3", "PSP", "SNES", "Wii", "WiiWare", "Xbox",
                "Xbox 360", "DS", "3DS", "Switch",
            ),
            vimmLairDefaultPresetPlatforms,
        )
        assertEquals(vimmLairDefaultPresetPlatforms.toSet(), defaultVimmLairGameSource().platforms)
        assertEquals(null, vimmLairPlatformSlug("3DS"))
        assertEquals(null, vimmLairPlatformSlug("Switch"))
    }

    @Test
    fun vimmSourceExposesDiscoveryOnlyCapabilities() {
        val source = VimmLairDiscoverySource(
            GameSourceConfig(
                id = "vimm",
                name = "Vimm's Lair",
                type = GameSourceProviderType.VIMM_LAIR,
                baseUrl = "https://vimm.net/vault",
            ),
            VimmLairTransport { error("transport must not be called") },
        )

        assertTrue(source.capabilities.search)
        assertTrue(source.capabilities.details)
        assertTrue(source.capabilities.externalOpen)
        assertTrue(!source.capabilities.paging)
    }

    @Test
    fun platformMappingsCoverEveryVimmConsole() {
        assertEquals("PS2", vimmLairPlatformSlug("PS2"))
        assertEquals("GameCube", vimmLairPlatformSlug("Nintendo GameCube"))
        assertEquals("Wii", vimmLairPlatformSlug("Wii"))
        assertEquals("PSP", vimmLairPlatformSlug("PlayStation Portable"))
        assertEquals("Dreamcast", vimmLairPlatformSlug("Sega Dreamcast"))
        assertEquals("Atari2600", vimmLairPlatformSlug("Atari 2600"))
        assertEquals("Atari5200", vimmLairPlatformSlug("Atari 5200"))
        assertEquals("Atari7800", vimmLairPlatformSlug("Atari 7800"))
        assertEquals("CDi", vimmLairPlatformSlug("CD-i"))
        assertEquals("PS3", vimmLairPlatformSlug("PlayStation 3"))
        assertEquals("Xbox360", vimmLairPlatformSlug("Xbox 360"))
        assertEquals("MasterSystem", vimmLairPlatformSlug("Master System"))
        assertEquals("GameGear", vimmLairPlatformSlug("Game Gear"))
        assertEquals("VirtualBoy", vimmLairPlatformSlug("Virtual Boy"))
        assertEquals("Lynx", vimmLairPlatformSlug("Atari Lynx"))
        assertEquals("Jaguar", vimmLairPlatformSlug("Atari Jaguar"))
        assertEquals("JaguarCD", vimmLairPlatformSlug("Atari Jaguar CD"))
        assertEquals("SegaCD", vimmLairPlatformSlug("Sega CD"))
        assertEquals("32X", vimmLairPlatformSlug("Sega 32X"))
        assertEquals("TG16", vimmLairPlatformSlug("TurboGrafx-16"))
        assertEquals("TGCD", vimmLairPlatformSlug("TurboGrafx-CD"))
        assertEquals("WiiWare", vimmLairPlatformSlug("WiiWare"))
        assertEquals(null, vimmLairPlatformSlug("Switch"))
        assertEquals(null, vimmLairPlatformSlug("3DS"))
        val mappedPlatforms = vimmLairSupportedPlatforms.mapNotNull(::vimmLairPlatformSlug)
        assertEquals(vimmLairSupportedPlatforms.size, mappedPlatforms.size)
        assertEquals(mappedPlatforms.size, mappedPlatforms.distinct().size)
    }

    @Test
    fun everyVimmConsoleHasAConsoleSpecificImportProfile() {
        val missingProfiles = vimmLairSupportedPlatforms.filterNot { platform ->
            RomImportPolicy.hasKnownPlatformProfile(platform)
        }

        assertEquals(emptyList<String>(), missingProfiles)
        assertTrue("cdi" in RomImportPolicy.supportedExtensions("Jaguar CD"))
        assertTrue("chd" in RomImportPolicy.supportedExtensions("CD-i"))
        assertTrue("vb" in RomImportPolicy.supportedExtensions("Virtual Boy"))
        assertTrue("32x" in RomImportPolicy.supportedExtensions("Sega 32X"))
        assertTrue("hes" in RomImportPolicy.supportedExtensions("TurboGrafx-CD"))
        assertTrue("wad" in RomImportPolicy.supportedExtensions("WiiWare"))
        vimmLairSupportedPlatforms.forEach { platform ->
            assertTrue("$platform must accept ZIP containers", "zip" in RomImportPolicy.supportedExtensions(platform))
            assertTrue("$platform must accept 7z containers", "7z" in RomImportPolicy.supportedExtensions(platform))
        }
        assertEquals("copy.7z", RomImportPolicy.safeFileName("copy.7z", "PS2"))
        assertTrue("iso" in RomImportPolicy.supportedGameExtensions("PS2"))
        assertTrue("7z" !in RomImportPolicy.supportedGameExtensions("PS2"))
        assertThrows(IllegalArgumentException::class.java) {
            RomImportPolicy.safeArchiveContentFileName("nested.7z", "PS2")
        }
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
            vimmLairSupportedPlatforms.sortedBy { normalizeCatalogTitle(it) },
            vimmLairSearchPlatforms(config, selectedPlatform = null),
        )
    }


    @Test
    fun multiConsoleSearchKeepsResultsAndReportsSafeFailureReasons() = runBlocking {
        val config = GameSourceConfig(
            id = "vimm",
            name = "Vimm",
            type = GameSourceProviderType.VIMM_LAIR,
            baseUrl = "https://vimm.net/vault",
            platforms = setOf("PS2", "GameCube", "NES"),
        )
        val summary = searchVimmLairAcrossPlatforms(
            config = config,
            query = "God",
            transport = VimmLairTransport { uri ->
                when {
                    uri.path.contains("/GameCube/") ->
                        throw IllegalArgumentException("Vimm request failed with HTTP 429")
                    uri.path.contains("/NES/") -> throw java.io.IOException("temporary")
                    uri.path.contains("/PS2/") -> """<a href="/vault/100">God Hand</a>"""
                    else -> ""
                }
            },
        )

        assertEquals(listOf("GameCube", "NES", "PS2"), summary.attemptedPlatforms)
        assertEquals(listOf("GameCube", "NES"), summary.failedPlatforms)
        assertEquals(mapOf("GameCube" to "HTTP 429", "NES" to "network error"), summary.failureReasons)
        assertEquals(1, summary.games.size)
        assertEquals("PS2", summary.games.single().platform)
        assertEquals("God Hand", summary.games.single().title)
    }

    @Test
    fun pspChainsOfOlympusFlowSearchesHydratesAndKeepsVimmMetadataOnly() = runBlocking {
        val config = GameSourceConfig(
            id = "vimm",
            name = "Vimm's Lair",
            type = GameSourceProviderType.VIMM_LAIR,
            baseUrl = "https://vimm.net/vault",
            platforms = setOf("PSP"),
        )
        val transport = VimmLairTransport { uri ->
            when (uri.path) {
                "/vault/PSP/G" -> """
                    <a href="/vault/24680">God of War: Chains of Olympus</a>
                """.trimIndent()
                "/vault/24680" -> """
                    <meta property="og:title" content="God of War: Chains of Olympus - Vimm's Lair">
                    <meta property="og:image" content="https://vimm.net/image.php?id=24680">
                    <table>
                      <tr><td>System</td><td>PSP</td></tr>
                      <tr><td>Region</td><td>USA</td></tr>
                      <tr><td>Release Date</td><td>March 4, 2008</td></tr>
                    </table>
                """.trimIndent()
                else -> error("unexpected Vimm request: $uri")
            }
        }
        val source = VimmLairDiscoverySource(config, transport)

        val result = source.search(platform = "PSP", query = "God of War Chains of Olympus").games.single()
        val hydrated = source.hydrate(result)

        assertEquals("24680", result.externalId)
        assertEquals("God of War: Chains of Olympus", hydrated.title)
        assertEquals("PSP", hydrated.platform)
        assertEquals("USA", hydrated.region)
        assertEquals(2008, hydrated.year)
        assertEquals("https://vimm.net/vault/24680", hydrated.detailsUrl)
        assertEquals("https://vimm.net/image.php?id=24680", hydrated.coverUrl)
        assertTrue(hydrated.detailsUrl!!.startsWith("https://vimm.net/vault/"))
    }

    @Test
    fun multiConsoleSearchPropagatesCancellationInsteadOfMarkingPlatformFailed() = runBlocking {
        val config = GameSourceConfig(
            id = "vimm",
            name = "Vimm's Lair",
            type = GameSourceProviderType.VIMM_LAIR,
            baseUrl = "https://vimm.net/vault",
            platforms = setOf("PSP", "PS2"),
        )
        val cancellation = CancellationException("cancelled")
        val sourceJob = launch {
            try {
                searchVimmLairAcrossPlatforms(
                    config = config,
                    query = "God of War",
                    transport = VimmLairTransport { throw cancellation },
                )
                error("expected cancellation")
            } catch (error: CancellationException) {
                assertEquals("cancelled", error.message)
            }
        }
        sourceJob.join()
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
    fun parserFindsCloseSpellingMatchesForGoogleLikeSearch() {
        val html = """
            <a href="/vault/23719">God of War: Chains of Olympus</a>
            <a href="/vault/23720">God of War: Ghost of Sparta</a>
            <a href="/vault/23721">Gran Turismo</a>
        """.trimIndent()

        val games = parseVimmLairListing(
            html = html,
            sourceId = "vimm",
            platform = "PSP",
            baseUrl = "https://vimm.net/vault",
            query = "God of Wra Chains of Olympus",
        )

        assertEquals(listOf("23719"), games.map { it.externalId })
        assertTrue(vimmLairApproximateTitleMatch("God of War: Chains of Olympus", "God of Wra"))
        assertTrue(!vimmLairApproximateTitleMatch("Gran Turismo", "God of Wra"))
        assertEquals("God of War", vimmLairCorrectedQuery("God of Wra", games))
        assertEquals(null, vimmLairCorrectedQuery("God of War", games))
    }

    @Test
    fun exactGodOfWarChainsVaultLinkHydratesMetadataWithoutCreatingMediaFields() = runBlocking {
        var requested: URI? = null
        val source = VimmLairDiscoverySource(
            GameSourceConfig(
                id = "vimm",
                name = "Vimm's Lair",
                type = GameSourceProviderType.VIMM_LAIR,
                baseUrl = "https://vimm.net/vault",
                platforms = setOf("PSP"),
            ),
            VimmLairTransport { uri ->
                requested = uri
                """
                    <meta property="og:title" content="God of War: Chains of Olympus - Vimm's Lair">
                    <table>
                      <tr><td>System</td><td>PSP</td></tr>
                      <tr><td>Region</td><td>USA</td></tr>
                    </table>
                """.trimIndent()
            },
        )

        val details = source.details("23719")

        assertEquals("https://vimm.net/vault/23719", requested.toString())
        assertEquals("God of War: Chains of Olympus", details.title)
        assertEquals("PSP", details.platform)
        assertEquals("USA", details.region)
        assertEquals("23719", details.externalId)
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
    fun cachedTransportDoesNotRetainFailedResponses() {
        var requests = 0
        val cache = VimmLairPageCache(maxEntries = 4, ttlMillis = 5_000L)
        val uri = URI("https://vimm.net/vault/PS2/A")
        val transport = CachedVimmLairTransport(
            delegate = VimmLairTransport {
                requests += 1
                if (requests == 1) error("temporary Vimm failure")
                "recovered"
            },
            cache = cache,
        )

        assertThrows(IllegalStateException::class.java) { runBlocking { transport.get(uri) } }
        assertEquals("recovered", runBlocking { transport.get(uri) })
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
    fun closerTitleMatchesSortBeforeLooseSubstringMatches() {
        val html = """
            <a href="/vault/1">God Hand</a>
            <a href="/vault/2">Hand of God</a>
            <a href="/vault/3">God</a>
            <a href="/vault/4">The Godfather</a>
        """.trimIndent()

        val games = parseVimmLairListing(
            html = html,
            sourceId = "vimm",
            platform = "PS2",
            baseUrl = "https://vimm.net/vault",
            query = "God",
        )

        assertEquals(
            listOf("God", "God Hand", "Hand of God", "The Godfather"),
            games.map { it.title },
        )
    }

    @Test
    fun exactPspTitleResolvesToMetadataOnlyVaultLink() {
        val games = parseVimmLairListing(
            html = "<a href=\"/vault/23719\">God of War: Chains of Olympus</a>",
            sourceId = "vimm",
            platform = "PSP",
            baseUrl = "https://vimm.net/vault",
            query = "God of War Chains of Olympus",
        )

        assertEquals(1, games.size)
        assertEquals("23719", games.single().externalId)
        assertEquals("God of War: Chains of Olympus", games.single().title)
        assertEquals("https://vimm.net/vault/23719", games.single().detailsUrl)
    }

    @Test
    fun matchRankPrefersExactThenPrefixThenWordPrefix() {
        assertEquals(0, vimmLairMatchRank("God", "God"))
        assertEquals(1, vimmLairMatchRank("God Hand", "God"))
        assertEquals(2, vimmLairMatchRank("Hand of God", "God"))
        assertEquals(3, vimmLairMatchRank("The Godfather", "God"))
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
