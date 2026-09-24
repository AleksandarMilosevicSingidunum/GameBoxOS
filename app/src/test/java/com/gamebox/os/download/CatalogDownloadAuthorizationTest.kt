package com.gamebox.os.download

import com.gamebox.os.catalog.*
import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayInputStream

class CatalogDownloadAuthorizationTest {
    private val basic = CatalogCredentials(username = "player", password = "secret")
    private val webdav = CatalogProviderConfig(CatalogTransport.WebDav("https://games.test/library"), "dav")
    private val checksum = "a".repeat(64)

    @Test fun basicCredentialsStayInsideExactOriginAndDirectory() {
        val auth = CatalogDownloadAuthorization(webdav, basic)
        assertTrue(auth.headers("https://games.test/library/games/demo.nes").containsKey("Authorization"))
        assertTrue(auth.headers("https://GAMES.test:443/library/demo.nes").containsKey("Authorization"))
        listOf(
            "https://other.test/library/demo.nes",
            "https://games.test:444/library/demo.nes",
            "https://games.test/library-other/demo.nes",
            "https://games.test/demo.nes",
            "https://games.test/library/../private/demo.nes",
            "https://games.test/library/%2e%2e/private/demo.nes",
            "https://games.test/library/a%2f..%2fprivate/demo.nes",
            "https://games.test/library/%252e%252e/demo.nes",
            "https://games.test/library/a%5cdemo.nes",
        ).forEach { assertTrue(it, auth.headers(it).isEmpty()) }
        val https = CatalogDownloadAuthorization(
            CatalogProviderConfig(CatalogTransport.Https("https://games.test/library/catalog.json"), "https"), basic)
        assertEquals(auth.headers("https://games.test/library/demo.nes"),
            https.headers("https://games.test/library/demo.nes"))
    }

    @Test fun s3SignsEachRequestAndNeverSignsOtherBucketsOrPrefixes() {
        val calls = mutableListOf<String>()
        val auth = CatalogDownloadAuthorization(
            CatalogProviderConfig(CatalogTransport.S3("https://objects.test", "games", "authorized", "eu-west-1"), "s3"),
            CatalogCredentials(accessKey = "access", secretKey = "secret"),
            signerFactory = { region ->
                assertEquals("eu-west-1", region)
                object : S3RequestSigner {
                    override fun sign(method: String, uri: String, payloadSha256: String, credentials: CatalogCredentials): SignedRequest {
                        assertEquals("GET", method)
                        assertEquals(64, payloadSha256.length)
                        calls += uri
                        return SignedRequest("signed-" + calls.size, "date-" + calls.size)
                    }
                }
            },
        )
        val url = "https://objects.test/games/authorized/demo.nes"
        assertEquals("signed-1", auth.headers(url)["Authorization"])
        assertEquals("signed-2", auth.headers(url)["Authorization"])
        assertTrue(auth.headers("https://objects.test/other/authorized/demo.nes").isEmpty())
        assertTrue(auth.headers("https://objects.test/games/private/demo.nes").isEmpty())
        assertTrue(auth.headers(url + "?X-Amz-Signature=presigned").isEmpty())
        assertEquals(2, calls.size)
    }

    @Test fun missingCredentialFailsOnlyForItsConfiguredScope() {
        val auth = CatalogDownloadAuthorization(webdav, null)
        assertTrue(auth.headers("https://public.test/game.nes").isEmpty())
        try {
            auth.headers("https://games.test/library/demo.nes")
            fail("Missing configured credentials must not silently become anonymous")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("unavailable"))
        }
    }

    @Test fun transferAppliesAuthenticationOnInitialAndResumedRequests() {
        val auth = CatalogDownloadAuthorization(webdav, basic)
        val connections = mutableListOf<FakeConnection>()
        val source = HttpsTransferSource(
            "https://games.test/library/demo.nes", 4, checksum, auth::headers,
            connectionFactory = {
                FakeConnection(if (connections.isEmpty()) 200 else 206).also(connections::add)
            },
        )
        source.openInputAt(0).input.use { assertEquals(1, it.read()) }
        source.openInputAt(2).input.use { assertEquals(1, it.read()) }
        assertEquals("Basic cGxheWVyOnNlY3JldA==", connections[0].getRequestProperty("Authorization"))
        assertEquals(connections[0].getRequestProperty("Authorization"), connections[1].getRequestProperty("Authorization"))
        assertEquals("bytes=2-", connections[1].getRequestProperty("Range"))
        assertTrue(connections.all { it.disconnected && !it.instanceFollowRedirects })
    }

    @Test fun redirectsNeverForwardCredentialsAndDisconnect() {
        val connection = FakeConnection(302)
        val source = HttpsTransferSource(
            "https://games.test/library/demo.nes", null, checksum,
            CatalogDownloadAuthorization(webdav, basic)::headers,
            connectionFactory = { connection },
        )
        try {
            source.openInput()
            fail("Redirect must fail")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("redirect"))
        }
        assertFalse(connection.instanceFollowRedirects)
        assertTrue(connection.disconnected)
    }

    private class FakeConnection(private val status: Int) : HttpURLConnection(URL("https://games.test/library/demo.nes")) {
        var disconnected = false
        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getContentLengthLong() = if (status == 206) 2L else 4L
        override fun getHeaderField(name: String): String? = if (name == "Content-Range") "bytes 2-3/4" else null
        override fun getInputStream() = ByteArrayInputStream(byteArrayOf(1, 2))
    }
}
