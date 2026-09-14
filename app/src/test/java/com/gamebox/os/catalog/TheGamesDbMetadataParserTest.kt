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
                  "game_title": "Catalog Test",
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
            {"data":{"games":[{"game_title":"Catalog Test","boxart":{"thumb":"https://cdn.example/cover.jpg"}}]}}
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
              "data":{"games":[{"game_title":"Catalog Test","overview":"","boxart":{"thumb":"cover.jpg"}}]},
              "include":{"boxart":{"base_url":{"thumb":"http://cdn.example/"}}}
            }
        """.trimIndent()
        val credentials = """
            {"data":{"games":[{"game_title":"Catalog Test","boxart":{"thumb":"https://user:secret@cdn.example/cover.jpg"}}]}}
        """.trimIndent()

        assertEquals(game, TheGamesDbMetadataParser.enrich(game, insecure))
        assertEquals(game.artworkUrl, TheGamesDbMetadataParser.enrich(game, credentials).artworkUrl)
    }

    @Test
    fun ignoresAHighRankingNearMatchAndSelectsTheUniqueExactTitle() {
        val payload = """
            {
              "data":{"games":[
                {"game_title":"Catalog Test Championship","overview":"Wrong"},
                {"game_title":"catalog-test","overview":"Correct","boxart":{"thumb":"https://cdn.example/correct.jpg"}}
              ]}
            }
        """.trimIndent()

        val result = TheGamesDbMetadataParser.enrich(game, payload)

        assertEquals("Correct", result.description)
        assertEquals("https://cdn.example/correct.jpg", result.artworkUrl)
    }

    @Test
    fun ambiguousExactTitlesDoNotOverwriteExistingMetadata() {
        val payload = """
            {
              "data":{"games":[
                {"game_title":"Catalog Test","overview":"PS1 candidate"},
                {"game_title":"Catalog Test","overview":"Different platform candidate"}
              ]}
            }
        """.trimIndent()

        assertEquals(game, TheGamesDbMetadataParser.enrich(game, payload))
    }

    @Test
    fun nonExactResultsDoNotOverwriteExistingMetadata() {
        val payload = """
            {"data":{"games":[{"game_title":"Catalog Test Deluxe","overview":"Wrong result"}]}}
        """.trimIndent()

        assertEquals(game, TheGamesDbMetadataParser.enrich(game, payload))
    }

    @Test
    fun malformedOrEmptyResponsesNeverRemoveExistingMetadata() {
        assertEquals(game, TheGamesDbMetadataParser.enrich(game, "not-json"))
        assertEquals(game, TheGamesDbMetadataParser.enrich(game, """{"data":{"games":[]}}"""))
    }
}
