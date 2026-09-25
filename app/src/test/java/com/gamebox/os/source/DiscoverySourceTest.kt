package com.gamebox.os.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DiscoverySourceTest {
    @Test
    fun sourceConfigRequiresSafeHttpsUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            GameSourceConfig(
                id = "local-source",
                name = "Local source",
                type = GameSourceProviderType.EXTERNAL_WEB,
                baseUrl = "http://example.test",
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            GameSourceConfig(
                id = "local-source",
                name = "Local source",
                type = GameSourceProviderType.EXTERNAL_WEB,
                baseUrl = "https://user:secret@example.test",
            )
        }
    }

    @Test
    fun registryResolvesSourcesCaseInsensitively() {
        val source = FakeSource("example", "Example")
        val registry = DiscoverySourceRegistry(listOf(source))

        assertEquals(source, registry.source("EXAMPLE"))
        assertNull(registry.source("missing"))
        assertEquals(listOf(source), registry.all())
    }


    @Test
    fun sourceConfigsRoundTripAndBrowseTemplateIsEncoded() {
        val source = GameSourceConfig(
            id = "external-library",
            name = "External library",
            type = GameSourceProviderType.EXTERNAL_WEB,
            baseUrl = "https://example.test/library",
            platforms = setOf("PS2", "PSP"),
            searchUrlTemplate = "https://example.test/search?q={query}&platform={platform}",
        )

        val decoded = decodeGameSourceConfigs(encodeGameSourceConfigs(listOf(source)))

        assertEquals(listOf(source), decoded)
        assertEquals(
            "https://example.test/search?q=God%20of%20War%20PS2&platform=PS2",
            source.resolveBrowseUrl("God of War", "PS2"),
        )
    }

    @Test
    fun platformRestrictionsAreNormalizedAndDisabledSourcesAreExcluded() {
        val source = GameSourceConfig(
            id = "ps-source",
            name = "PS source",
            type = GameSourceProviderType.EXTERNAL_WEB,
            baseUrl = "https://example.test",
            platforms = setOf("PS2", "PlayStation Portable"),
        )
        assertEquals(true, source.supportsPlatform("ps2"))
        assertEquals(true, source.supportsPlatform("PlayStation Portable"))
        assertEquals(false, source.supportsPlatform("GameCube"))
        assertEquals(false, source.copy(enabled = false).supportsPlatform("PS2"))
    }

    @Test
    fun sourceIdsAreGeneratedDeterministically() {
        val existing = setOf("my-library", "my-library-2")
        assertEquals("my-library-3", nextGameSourceId("My Library", existing))
    }

    @Test
    fun duplicateSourceIdsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DiscoverySourceRegistry(
                listOf(
                    FakeSource("same", "One"),
                    FakeSource("SAME", "Two"),
                )
            )
        }
    }

    private class FakeSource(
        override val id: String,
        override val displayName: String,
    ) : DiscoverySource {
        override suspend fun search(
            platform: String?,
            query: String,
            page: Int,
        ): DiscoverySourcePage = DiscoverySourcePage(emptyList())

        override suspend fun details(externalId: String): DiscoverySourceGame =
            DiscoverySourceGame(
                sourceId = id,
                externalId = externalId,
                title = "Example",
                platform = "PS2",
            )
    }
}
