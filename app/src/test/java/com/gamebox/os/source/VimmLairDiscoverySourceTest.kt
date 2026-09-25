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
