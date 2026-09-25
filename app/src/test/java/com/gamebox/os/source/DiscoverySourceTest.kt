package com.gamebox.os.source

import kotlinx.coroutines.test.runTest
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
    fun registryResolvesSourcesCaseInsensitively() = runTest {
        val source = FakeSource("example", "Example")
        val registry = DiscoverySourceRegistry(listOf(source))

        assertEquals(source, registry.source("EXAMPLE"))
        assertNull(registry.source("missing"))
        assertEquals(listOf(source), registry.all())
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
