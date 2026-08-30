package com.gamebox.os

import com.gamebox.os.catalog.CatalogFormatException
import com.gamebox.os.catalog.CatalogParser
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogParserTest {
    private val parser = CatalogParser()

    @Test fun validManifest_mapsExplicitState() {
        val snapshot = parser.parse(validManifest())
        assertEquals("fixture", snapshot.providerId)
        assertEquals(InstallState.QUEUED, snapshot.games.single().state)
    }

    @Test fun duplicateGameIds_areRejected() {
        val input = validManifest().replace(
            "] }",
            ", {\"id\":\"game\",\"title\":\"Second\",\"platform\":\"Homebrew\",\"year\":2026,\"genre\":\"Test\",\"sizeMb\":2,\"contentPolicy\":\"freeware\"}] }"
        )
        assertThrows(CatalogFormatException::class.java) { parser.parse(input) }
    }

    @Test fun unsupportedSchema_isRejected() {
        assertThrows(CatalogFormatException::class.java) {
            parser.parse(validManifest().replace("\"schemaVersion\":1", "\"schemaVersion\":99"))
        }
    }

    @Test fun richMetadata_mapsArtworkAndDescription() {
        val input = validManifest().replace(
            "}] }",
            ",\"artworkUrl\":\"https://cdn.example.test/game.jpg\",\"description\":\"A test game\",\"players\":\"1\",\"language\":\"English\",\"region\":\"Worldwide\"}] }"
        )
        val game = parser.parse(input).games.single()
        assertEquals("https://cdn.example.test/game.jpg", game.artworkUrl)
        assertEquals("A test game", game.description)
        assertEquals("1", game.players)
    }

    @Test fun insecureArtworkUrl_isRejected() {
        val input = validManifest().replace(
            "}] }",
            ",\"artworkUrl\":\"http://cdn.example.test/game.jpg\"}] }"
        )
        assertThrows(CatalogFormatException::class.java) { parser.parse(input) }
    }

    @Test fun malformedJson_isRejected() {
        assertThrows(CatalogFormatException::class.java) { parser.parse("{not-json") }
    }

    private fun validManifest() =
        """{"schemaVersion":1,"provider":{"id":"fixture","displayName":"Fixture"},"games":[{"id":"game","title":"Game","platform":"Homebrew","year":2026,"genre":"Test","sizeMb":1,"contentPolicy":"freeware","initialState":"QUEUED"}] }"""
}
