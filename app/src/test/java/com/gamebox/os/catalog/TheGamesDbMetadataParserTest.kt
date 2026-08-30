package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class TheGamesDbMetadataParserTest {
    private val game = Game(
        id = GameId("catalog-test"),
        title = "Catalog Test",
        platform = "PS1",
        year = 1999,
        genre = "Racing",
        sizeMb = 1,
        state = InstallState.INSTALLED,
        artworkUrl = "https://fallback.example/cover.jpg",
        description = "Fallback",
    )

    @Test
    fun resolvesRelativeArtworkAgainstProviderThumbBase() {
        val payload = """
            {
              "data": {
                "games": [{
                  "overview": "  Provider description  ",
                  "boxart": { "thumb": "boxart/front/123-1.jpg" }
                }]
              },
              "include": {
                "boxart": {
                  "base_url": { "thumb": "https://cdn.thegamesdb.net/images/original/" }
                }
              }
            }
        """.trimIndent()

        val result = TheGamesDbMetadataParser.enrich(game, payload)

        assertEquals("https://cdn.thegamesdb.net/images/original/boxart/front/123-1.jpg", result.artworkUrl)
        assertEquals("Provider description", result.description)
    }

    @Test
    fun acceptsAbsoluteHttpsArtworkWithoutBase() {
        val payload = """
            {"data":{"games":[{"boxart":{"thumb":"https://cdn.example/cover.jpg"}}]}}
        """.trimIndent()

        assertEquals(
            "https://cdn.example/cover.jpg",
            TheGamesDbMetadataParser.enrich(game, payload).artworkUrl,
        )
    }

    @Test
    fun rejectsInsecureOrCredentialBearingArtworkAndPreservesFallback() {
        val insecure = """
            {
              "data":{"games":[{"overview":"","boxart":{"thumb":"cover.jpg"}}]},
              "include":{"boxart":{"base_url":{"thumb":"http://cdn.example/"}}}
            }
        """.trimIndent()
        val credentials = """
            {"data":{"games":[{"boxart":{"thumb":"https://user:secret@cdn.example/cover.jpg"}}]}}
        """.trimIndent()

        assertEquals(game, TheGamesDbMetadataParser.enrich(game, insecure))
        assertEquals(game.artworkUrl, TheGamesDbMetadataParser.enrich(game, credentials).artworkUrl)
    }

    @Test
    fun malformedOrEmptyResponsesNeverRemoveExistingMetadata() {
        assertEquals(game, TheGamesDbMetadataParser.enrich(game, "not-json"))
        assertEquals(game, TheGamesDbMetadataParser.enrich(game, """{"data":{"games":[]}}"""))
    }
}
