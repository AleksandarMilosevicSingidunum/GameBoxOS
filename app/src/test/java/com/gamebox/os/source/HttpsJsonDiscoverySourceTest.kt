package com.gamebox.os.source

import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HttpsJsonDiscoverySourceTest {
    @Test
    fun parsesAndFiltersConfiguredJsonCatalog() {
        val connection = RecordingConnection(
            """
            {
              "games": [
                {
                  "id": "gow2",
                  "title": "God of War II",
                  "platform": "PS2",
                  "region": "USA",
                  "year": 2007,
                  "detailsUrl": "https://example.test/games/gow2",
                  "coverUrl": "https://example.test/covers/gow2.jpg"
                },
                {
                  "id": "wipeout",
                  "title": "Wipeout Pure",
                  "platform": "PSP"
                }
              ]
            }
            """.trimIndent()
        )
        val source = HttpsJsonDiscoverySource(
            config = GameSourceConfig(
                id = "json-source",
                name = "JSON source",
                type = GameSourceProviderType.GAMEBOX_JSON,
                baseUrl = "https://example.test/catalog.json",
            ),
            connectionFactory = { connection },
        )

        val page = runBlocking { source.search(platform = "PS2", query = "War") }

        assertEquals(1, page.games.size)
        assertEquals("God of War II", page.games.single().title)
        assertEquals("json-source", page.games.single().sourceId)
        assertEquals("application/json", connection.headers["Accept"])
        assertEquals("GameBoxOS/0.1", connection.headers["User-Agent"])
    }

    @Test
    fun rejectsRedirectAndInsecureResultUrls() {
        val redirect = RecordingConnection("{}", status = HttpURLConnection.HTTP_MOVED_TEMP)
        val source = HttpsJsonDiscoverySource(
            GameSourceConfig(
                id = "json-source",
                name = "JSON source",
                type = GameSourceProviderType.GAMEBOX_JSON,
                baseUrl = "https://example.test/catalog.json",
            ),
            connectionFactory = { redirect },
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { source.search() }
        }

        val insecure = RecordingConnection(
            """{"games":[{"id":"1","title":"Example","platform":"PS2","coverUrl":"http://bad.test/a.jpg"}]}"""
        )
        val insecureSource = HttpsJsonDiscoverySource(
            GameSourceConfig(
                id = "json-source",
                name = "JSON source",
                type = GameSourceProviderType.GAMEBOX_JSON,
                baseUrl = "https://example.test/catalog.json",
            ),
            connectionFactory = { insecure },
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { insecureSource.search() }
        }
    }

    @Test
    fun searchTemplateReceivesEncodedQueryAndPlatform() {
        var requestedUrl: String? = null
        val source = HttpsJsonDiscoverySource(
            GameSourceConfig(
                id = "json-source",
                name = "JSON source",
                type = GameSourceProviderType.GAMEBOX_JSON,
                baseUrl = "https://example.test/catalog.json",
                searchUrlTemplate = "https://example.test/search?q={query}&platform={platform}",
            ),
            connectionFactory = { uri ->
                requestedUrl = uri.toString()
                RecordingConnection("""{"games":[]}""")
            },
        )

        runBlocking { source.search(platform = "PS2", query = "God of War") }

        assertEquals(
            "https://example.test/search?q=God%20of%20War%20PS2&platform=PS2",
            requestedUrl,
        )
    }

    private class RecordingConnection(
        private val body: String,
        private val status: Int = HTTP_OK,
    ) : HttpURLConnection(URL("https://example.test")) {
        val headers = mutableMapOf<String, String>()

        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun setRequestProperty(key: String, value: String) {
            headers[key] = value
        }
        override fun getResponseCode() = status
        override fun getInputStream() = ByteArrayInputStream(body.toByteArray())
    }
}
