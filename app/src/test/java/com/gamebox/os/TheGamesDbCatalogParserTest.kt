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
    fun parsesTheGamesDbKeyedPlatformMapAndCanonicalizesSonyName() {
        val payload = """{"data":{"platforms":{"11":{"id":11,"name":"Sony Playstation 2"},"3":{"id":3,"name":"Nintendo 64"}}}}"""

        val platforms = TheGamesDbCatalogParser.parsePlatforms(payload)

        assertEquals("playstation2", platforms.first { it.externalIds[MetadataProviderId.THE_GAMES_DB] == "11" }.id)
        assertEquals("Nintendo 64", platforms.first { it.externalIds[MetadataProviderId.THE_GAMES_DB] == "3" }.name)
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
              "include": {"boxart": {
                "base_url": {
                  "thumb": "https://cdn.example.com/thumb/",
                  "medium": "https://cdn.example.com/medium/",
                  "large": "https://cdn.example.com/large/",
                  "original": "https://cdn.example.com/original/"
                },
                "data": {"42": [
                  {"type":"boxart", "side":"front", "filename":"covers/42.jpg"},
                  {"type":"screenshot", "filename":"screens/42.jpg"},
                  {"type":"fanart", "filename":"fanart/42.jpg"},
                  {"type":"clearlogo", "filename":"logos/42.png"}
                ]}
              }}
            }
        """.trimIndent()

        val page = TheGamesDbCatalogParser.parsePlatformPage(payload, platform)

        assertEquals(2, page.nextPage)
        assertEquals("tgdb-playstation2-42", page.games.single().id.value)
        assertEquals("https://cdn.example.com/thumb/covers/42.jpg", page.games.single().media.cover)
        assertEquals("https://cdn.example.com/large/fanart/42.jpg", page.games.single().media.background)
        assertEquals("https://cdn.example.com/original/logos/42.png", page.games.single().media.logo)
        assertEquals(listOf("https://cdn.example.com/medium/screens/42.jpg"), page.games.single().media.screenshots)
    }

    @Test
    fun rejectsInsecureArtworkAndMalformedEntries() {
        val platform = TheGamesDbCatalogParser.parsePlatforms(
            """{"data":{"platforms":[{"id":11,"name":"PlayStation 2"}]}}"""
        ).single()
        val payload = """{"data":{"games":[{"id":1,"game_title":"Safe"},{"id":2}]},"include":{"boxart":{"base_url":{"thumb":"https://cdn.example.com/thumb/"},"data":{"1":[{"type":"boxart","filename":"http://bad.example/a.jpg"}]}}}}"""

        val page = TheGamesDbCatalogParser.parsePlatformPage(payload, platform)

        assertEquals(1, page.games.size)
        assertNull(page.games.single().media.cover)
        assertTrue(page.nextPage == null)
    }
}
