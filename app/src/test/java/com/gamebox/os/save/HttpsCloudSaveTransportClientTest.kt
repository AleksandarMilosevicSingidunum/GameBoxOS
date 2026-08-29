package com.gamebox.os.save

import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.S3RequestSigner
import com.gamebox.os.catalog.SignedRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpsCloudSaveTransportClientTest {
    private val request = CloudSaveSyncRequest(
        gameId = "game-1",
        endpoint = URI("https://cloud.example/saves/game-1.bin"),
        payloadBytes = 4,
        credentialKey = "cloud",
    )

    @Test
    fun uploadsBytesWithBasicAuthenticationAndNoRedirects() = runBlocking {
        val connection = FakeConnection(status = 204)
        val client = HttpsCloudSaveTransportClient(connectionFactory = { connection })

        client.upload(request, byteArrayOf(1, 2, 3, 4), CatalogCredentials("player", "secret"))

        assertEquals("PUT", connection.requestMethod)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), connection.written.toByteArray())
        assertTrue(connection.getRequestProperty("Authorization").startsWith("Basic "))
        assertFalse(connection.instanceFollowRedirects)
        assertTrue(connection.disconnected)
    }

    @Test
    fun downloadsBoundedBytesWithS3Signature() = runBlocking {
        val connection = FakeConnection(status = 200, response = byteArrayOf(9, 8, 7))
        val signer = RecordingSigner()
        val client = HttpsCloudSaveTransportClient(
            s3Signer = signer,
            maxResponseBytes = 4,
            connectionFactory = { connection },
        )

        val result = client.download(
            request.copy(payloadBytes = 0),
            CatalogCredentials(accessKey = "key", secretKey = "secret"),
        )

        assertArrayEquals(byteArrayOf(9, 8, 7), result)
        assertEquals("GET", signer.method)
        assertEquals(64, signer.payloadHash.length)
        assertEquals("AWS4 signed", connection.getRequestProperty("Authorization"))
        assertTrue(connection.disconnected)
    }

    @Test
    fun rejectsOversizedDownloadsAndIncompleteCredentials() {
        val oversized = FakeConnection(status = 200, response = ByteArray(5))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                HttpsCloudSaveTransportClient(maxResponseBytes = 4, connectionFactory = { oversized })
                    .download(request.copy(payloadBytes = 0), CatalogCredentials("player", "secret"))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                HttpsCloudSaveTransportClient(connectionFactory = { FakeConnection(200) })
                    .download(request.copy(payloadBytes = 0), CatalogCredentials(username = "player"))
            }
        }
    }

    @Test
    fun mapsHttpFailuresToSharedRecoveryPolicy() {
        val error = assertThrows(CloudSaveTransportException::class.java) {
            runBlocking {
                HttpsCloudSaveTransportClient(connectionFactory = { FakeConnection(401) })
                    .download(request.copy(payloadBytes = 0), CatalogCredentials("player", "secret"))
            }
        }
        assertFalse(error.recovery.retryable)
    }

    private class RecordingSigner : S3RequestSigner {
        var method = ""
        var payloadHash = ""
        override fun sign(
            method: String,
            uri: String,
            payloadSha256: String,
            credentials: CatalogCredentials,
        ): SignedRequest {
            this.method = method
            payloadHash = payloadSha256
            return SignedRequest("AWS4 signed", "20260829T000000Z")
        }
    }

    private class FakeConnection(
        private val status: Int,
        response: ByteArray = ByteArray(0),
    ) : HttpURLConnection(URL("https://cloud.example")) {
        val written = ByteArrayOutputStream()
        var disconnected = false
        private val input = ByteArrayInputStream(response)

        override fun getResponseCode(): Int = status
        override fun getInputStream() = input
        override fun getOutputStream() = written
        override fun disconnect() { disconnected = true }
        override fun usingProxy(): Boolean = false
        override fun connect() = Unit
    }
}
