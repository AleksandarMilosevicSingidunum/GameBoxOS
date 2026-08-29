package com.gamebox.os.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteCatalogMetadataTest {
    private val checksum = "a".repeat(64)

    @Test
    fun preservesAuthorizedSourceAndChecksum() {
        val snapshot = CatalogParser().parse(
            """
            {
              "schemaVersion": 1,
              "provider": {"id":"test","displayName":"Test"},
              "games":[{
                "id":"demo",
                "title":"Demo",
                "platform":"Homebrew",
                "year":2026,
                "genre":"Demo",
                "sizeMb":2,
                "contentPolicy":"homebrew",
                "source":"https://downloads.example.com/demo.bin",
                "checksum":"$checksum"
              }]
            }
            """.trimIndent()
        )
        assertEquals("https://downloads.example.com/demo.bin", snapshot.games.single().sourceUrl)
        assertEquals(checksum, snapshot.games.single().expectedSha256)
    }

    @Test
    fun rejectsIncompleteOrInsecureRemoteMetadata() {
        assertThrows(CatalogFormatException::class.java) {
            CatalogParser().parse(manifest("http://downloads.example.com/demo.bin", checksum))
        }
        assertThrows(CatalogFormatException::class.java) {
            CatalogParser().parse(manifest("https://downloads.example.com/demo.bin", null))
        }
        assertThrows(CatalogFormatException::class.java) {
            CatalogParser().parse(manifest("https://downloads.example.com/demo.bin", "bad"))
        }
    }

    private fun manifest(source: String, checksum: String?): String {
        val checksumField = checksum?.let { ""","checksum":"$it"""" } ?: ""
        return """
            {
              "schemaVersion":1,
              "provider":{"id":"test","displayName":"Test"},
              "games":[{
                "id":"demo","title":"Demo","platform":"Homebrew","year":2026,
                "genre":"Demo","sizeMb":2,"contentPolicy":"homebrew",
                "source":"$source"$checksumField
              }]
            }
        """.trimIndent()
    }
}
