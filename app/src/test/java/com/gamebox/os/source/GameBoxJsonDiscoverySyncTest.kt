package com.gamebox.os.source

import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.data.local.CatalogExternalIdEntity
import com.gamebox.os.data.local.CatalogGameEntity
import com.gamebox.os.data.local.CatalogPlatformEntity
import java.net.URI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GameBoxJsonDiscoverySyncTest {
    private val parser = GameBoxJsonDiscoveryParser()

    @Test
    fun parserAcceptsMetadataOnlySchemaAndNormalizesPlatform() {
        val games = parser.parse(
            """
            {
              "schemaVersion": 1,
              "games": [{
                "id": "provider-game-1",
                "title": "Example Game",
                "platform": "PlayStation 2",
                "year": 2005,
                "region": "USA",
                "rating": 8.5,
                "coverUrl": "https://cdn.example.test/cover.jpg",
                "screenshots": [
                  "https://cdn.example.test/one.jpg",
                  "https://cdn.example.test/two.jpg"
                ]
              }]
            }
            """.trimIndent()
        )

        assertEquals(1, games.size)
        assertEquals("provider-game-1", games.single().externalId)
        assertEquals("Example Game", games.single().title)
        assertEquals("playstation2", games.single().platformId)
        assertEquals(2005, games.single().year)
        assertEquals(2, games.single().screenshots.size)
    }

    @Test
    fun supportedConsoleAliasesUseTheGamesDbCompatiblePlatformIds() {
        assertEquals("playstation2", canonicalDiscoveryPlatformId("PS2"))
        assertEquals("nintendogamecube", canonicalDiscoveryPlatformId("GameCube"))
        assertEquals("nintendowii", canonicalDiscoveryPlatformId("Nintendo Wii"))
        assertEquals("playstationportable", canonicalDiscoveryPlatformId("PSP"))
        assertEquals("segadreamcast", canonicalDiscoveryPlatformId("Dreamcast"))
        assertEquals("nintendo3ds", canonicalDiscoveryPlatformId("3DS"))
        assertEquals("nintendoswitch", canonicalDiscoveryPlatformId("Switch"))
    }

    @Test
    fun parserRejectsUnsafeArtworkAndDuplicateIds() {
        assertThrows(IllegalArgumentException::class.java) {
            parser.parse(
                """
                {
                  "schemaVersion": 1,
                  "games": [{
                    "id": "one",
                    "title": "Unsafe",
                    "platform": "PS2",
                    "coverUrl": "http://example.test/cover.jpg"
                  }]
                }
                """.trimIndent()
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            parser.parse(
                """
                {
                  "schemaVersion": 1,
                  "games": [
                    {"id": "same", "title": "One", "platform": "PS2"},
                    {"id": "same", "title": "Two", "platform": "PS2"}
                  ]
                }
                """.trimIndent()
            )
        }
    }

    @Test
    fun syncPreservesFavoritesExistingPlatformAndScopesConfiguredPlatforms() = runBlocking {
        val dao = FakeDiscoveryDao(
            existingPlatform = CatalogPlatformEntity(
                id = "playstation2",
                name = "Sony Playstation 2",
                theGamesDbId = "11",
                updatedAtMillis = 10L,
            ),
        )
        val payload = """
            {
              "schemaVersion": 1,
              "games": [
                {
                  "id": "../owned-game",
                  "title": "Owned Game",
                  "platform": "PS2",
                  "year": 2004,
                  "coverUrl": "https://cdn.example.test/owned.jpg"
                },
                {
                  "id": "other",
                  "title": "Other Console",
                  "platform": "GameCube",
                  "year": 2003
                }
              ]
            }
        """.trimIndent()
        val transport = GameBoxJsonDiscoveryTransport { _: URI -> payload }
        val sync = GameBoxJsonDiscoverySync(
            dao = dao,
            transport = transport,
            nowMillis = { 1234L },
        )
        val source = GameSourceConfig(
            id = "personal-json",
            name = "Personal metadata",
            type = GameSourceProviderType.GAMEBOX_JSON,
            baseUrl = "https://example.test/catalog.json",
            platforms = setOf("PS2"),
        )
        dao.favoriteIds = setOf(gameBoxJsonDiscoveryGameId(source.id, "../owned-game"))

        val result = sync.sync(source)

        assertEquals(
            ConfiguredDiscoverySyncResult.Success("personal-json", games = 1, platforms = 1),
            result,
        )
        assertEquals(1, dao.games.size)
        val stored = dao.games.single()
        assertEquals("Owned Game", stored.title)
        assertTrue(stored.favorite)
        assertFalse(stored.id.contains(".."))
        assertFalse(stored.id.contains("owned-game"))
        assertEquals("playstation2", stored.platformId)

        val platform = dao.platforms.single()
        assertEquals("Sony Playstation 2", platform.name)
        assertEquals("11", platform.theGamesDbId)
        assertEquals(1234L, platform.updatedAtMillis)

        val external = dao.externalIds.single()
        assertEquals("GAMEBOX_JSON:personal-json", external.provider)
        assertEquals("../owned-game", external.externalId)
        assertEquals(stored.id, external.gameId)
    }

    @Test
    fun disabledAndExternalWebSourcesAreNotFetched() = runBlocking {
        var calls = 0
        val sync = GameBoxJsonDiscoverySync(
            dao = FakeDiscoveryDao(),
            transport = GameBoxJsonDiscoveryTransport {
                calls += 1
                """{"schemaVersion":1,"games":[]}"""
            },
        )

        val json = GameSourceConfig(
            id = "json-source",
            name = "JSON",
            type = GameSourceProviderType.GAMEBOX_JSON,
            baseUrl = "https://example.test/catalog.json",
            enabled = false,
        )
        val web = GameSourceConfig(
            id = "web-source",
            name = "Web",
            type = GameSourceProviderType.EXTERNAL_WEB,
            baseUrl = "https://example.test/",
        )

        assertEquals(ConfiguredDiscoverySyncResult.Disabled, sync.sync(json))
        assertEquals(ConfiguredDiscoverySyncResult.Unsupported, sync.sync(web))
        assertEquals(0, calls)
    }

    private class FakeDiscoveryDao(
        private val existingPlatform: CatalogPlatformEntity? = null,
    ) : CatalogDiscoveryDao {
        val platforms = mutableListOf<CatalogPlatformEntity>()
        val games = mutableListOf<CatalogGameEntity>()
        val externalIds = mutableListOf<CatalogExternalIdEntity>()
        var favoriteIds: Set<String> = emptySet()

        override suspend fun upsertPlatforms(platforms: List<CatalogPlatformEntity>) {
            this.platforms += platforms
        }

        override suspend fun upsertGames(games: List<CatalogGameEntity>) {
            this.games += games
        }

        override suspend fun upsertExternalIds(ids: List<CatalogExternalIdEntity>) {
            externalIds += ids
        }

        override fun observeGames(
            platformId: String?,
            normalizedQuery: String,
            limit: Int,
            offset: Int,
        ): Flow<List<CatalogGameEntity>> = flowOf(games)

        override fun observeGame(gameId: String): Flow<CatalogGameEntity?> =
            flowOf(games.firstOrNull { it.id == gameId })

        override fun observePlatforms(): Flow<List<CatalogPlatformEntity>> =
            flowOf(listOfNotNull(existingPlatform) + platforms)

        override suspend fun countGames(platformId: String): Int =
            games.count { it.platformId == platformId }

        override suspend fun setFavorite(gameId: String, favorite: Boolean) = Unit

        override suspend fun favoriteGameIds(gameIds: List<String>): List<String> =
            gameIds.filter { it in favoriteIds }
    }
}
