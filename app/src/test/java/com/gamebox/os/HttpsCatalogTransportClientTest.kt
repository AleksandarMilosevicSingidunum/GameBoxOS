package com.gamebox.os

import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.CatalogTransport
import com.gamebox.os.catalog.HttpsCatalogTransportClient
import com.gamebox.os.catalog.S3RequestSigner
import com.gamebox.os.catalog.SignedRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

class HttpsCatalogTransportClientTest {
    @Test
    fun rejectsRelativeHttpsEndpointsBeforeNetworkAccess() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                HttpsCatalogTransportClient().fetch(CatalogTransport.Https("https:/catalog.json"), null)
            }
        }
    }

    @Test
    fun s3CredentialsUseARegionAwareSignerBeforeFetching() {
        var requestedRegion: String? = null
        val connection = RecordingConnection()
        val client = HttpsCatalogTransportClient(
            s3SignerFactory = { region ->
                requestedRegion = region
                object : S3RequestSigner {
                    override fun sign(method: String, uri: String, payloadSha256: String, credentials: CatalogCredentials) =
                        SignedRequest("signed", "20260901T000000Z")
                }
            },
            connectionFactory = { connection },
        )

        val payload = runBlocking {
            client.fetch(
                CatalogTransport.S3("https://example.test", "games", region = "eu-central-1"),
                CatalogCredentials(accessKey = "key", secretKey = "secret"),
            )
        }

        assertEquals("{}", payload)
        assertEquals("eu-central-1", requestedRegion)
        assertEquals("signed", connection.headers["Authorization"])
        assertEquals("20260901T000000Z", connection.headers["x-amz-date"])
    }

    private class RecordingConnection : HttpURLConnection(URL("https://example.test")) {
        val headers = mutableMapOf<String, String>()
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun setRequestProperty(key: String, value: String) {
            headers[key] = value
        }
        override fun getResponseCode() = HTTP_OK
        override fun getInputStream() = ByteArrayInputStream("{}".toByteArray())
    }
}

