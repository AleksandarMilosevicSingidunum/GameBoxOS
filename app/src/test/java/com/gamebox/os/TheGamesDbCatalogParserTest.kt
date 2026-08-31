package com.gamebox.os

import com.gamebox.os.catalog.TheGamesDbCatalogParser
import com.gamebox.os.domain.MetadataProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TheGamesDbCatalogParserTest {
    @Test
    fun resolvesPlatformsWithoutHardcodedNumericIds() {
        val payload = """{"data":{"platforms":[{"id":11,"name":"PlayStation 2"},{"id":7,"name":"Nintendo Entertainment System"}]}}"""

        val platforms = TheGamesDbCatalogParser.parsePlatforms(payload)

        assertEquals("11", platforms.first().externalIds[MetadataProviderId.THE_GAMES_DB])
        assertEquals("playstation2", platforms.first().id)
    }

    @Test
    fun parsesPagedMetadataOnlyCatalogWithHttpsArtwork() {
        val platform = TheGamesDbCatalogParser.parsePlatforms(
            """{"data":{"platforms":[{"id":11,"name":"PlayStation 2"}]}}"""
        ).single()
        val payload = """
            {
              "data": {
                "pages": {"current": 1, "next": 2},
                "games": [{
                  "id": 42,
                  "game_title": "Example Game",
                  "release_date": "2007-03-13",
                  "overview": "Description",
                  "players": "2",
                  "rating": "4.8",
                  "boxart": {"thumb": "covers/42.jpg"}
                }]
              },
              "include": {"boxart": {"base_url": {"thumb": "https://cdn.example.com/thumb/"}}}
            }
        """.trimIndent()

        val page = TheGamesDbCatalogParser.parsePlatformPage(payload, platform)

        assertEquals(2, page.nextPage)
        assertEquals("tgdb-playstation2-42", page.games.single().id.value)
        assertEquals("https://cdn.example.com/thumb/covers/42.jpg", page.games.single().media.cover)
    }

    @Test
    fun rejectsInsecureArtworkAndMalformedEntries() {
        val platform = TheGamesDbCatalogParser.parsePlatforms(
            """{"data":{"platforms":[{"id":11,"name":"PlayStation 2"}]}}"""
        ).single()
        val payload = """{"data":{"games":[{"id":1,"game_title":"Safe","boxart":{"thumb":"http://bad.example/a.jpg"}},{"id":2}]} }"""

        val page = TheGamesDbCatalogParser.parsePlatformPage(payload, platform)

        assertEquals(1, page.games.size)
        assertNull(page.games.single().media.cover)
        assertTrue(page.nextPage == null)
    }
}
