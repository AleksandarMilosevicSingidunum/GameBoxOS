package com.gamebox.os.save

import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.S3RequestSigner
import com.gamebox.os.catalog.SignedRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
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
    fun verifiesUploadBeforeSendingPayload() {
        val connection = FakeConnection(status = 204)
        val payload = byteArrayOf(1, 2, 3, 4)
        val error = assertThrows(CloudSaveIntegrityException::class.java) {
            runBlocking {
                HttpsCloudSaveTransportClient(connectionFactory = { connection }).upload(
                    request.copy(expectedSha256 = sha256(byteArrayOf(4, 3, 2, 1))),
                    payload,
                    CatalogCredentials("player", "secret"),
                )
            }
        }

        assertEquals(sha256(payload), error.actualSha256)
        assertEquals(0, connection.written.size())
        assertFalse(connection.disconnected)
    }

    @Test
    fun downloadsBoundedBytesWithS3Signature() = runBlocking {
        val payload = byteArrayOf(9, 8, 7)
        val connection = FakeConnection(status = 200, response = payload)
        val signer = RecordingSigner()
        val client = HttpsCloudSaveTransportClient(
            s3Signer = signer,
            maxResponseBytes = 4,
            connectionFactory = { connection },
        )

        val result = client.download(
            request.copy(payloadBytes = 0, expectedSha256 = sha256(payload).uppercase()),
            CatalogCredentials(accessKey = "key", secretKey = "secret"),
        )

        assertArrayEquals(payload, result)
        assertEquals("GET", signer.method)
        assertEquals(64, signer.payloadHash.length)
        assertEquals("AWS4 signed", connection.getRequestProperty("Authorization"))
        assertTrue(connection.disconnected)
    }

    @Test
    fun rejectsCorruptedDownloadAndDisconnects() {
        val payload = byteArrayOf(9, 8, 7)
        val connection = FakeConnection(status = 200, response = payload)
        val error = assertThrows(CloudSaveIntegrityException::class.java) {
            runBlocking {
                HttpsCloudSaveTransportClient(connectionFactory = { connection }).download(
                    request.copy(payloadBytes = 0, expectedSha256 = sha256(byteArrayOf(7, 8, 9))),
                    CatalogCredentials("player", "secret"),
                )
            }
        }

        assertEquals(sha256(payload), error.actualSha256)
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
    fun rejectsMalformedExpectedChecksum() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                HttpsCloudSaveTransportClient(connectionFactory = { FakeConnection(200) })
                    .download(
                        request.copy(payloadBytes = 0, expectedSha256 = "not-a-sha256"),
                        CatalogCredentials("player", "secret"),
                    )
            }
        }
    }

    @Test
    fun retriesTransientUploadsWithBoundedBackoff() = runBlocking {
        val first = FakeConnection(status = 503)
        val second = FakeConnection(status = 204)
        val connections = ArrayDeque(listOf(first, second))
        val delays = mutableListOf<Long>()
        val client = HttpsCloudSaveTransportClient(
            maxRetries = 2,
            retryDelay = { delays += it },
            connectionFactory = { connections.removeFirst() },
        )

        client.upload(request, byteArrayOf(1, 2, 3, 4), CatalogCredentials("player", "secret"))

        assertEquals(listOf(1_000L), delays)
        assertTrue(first.disconnected)
        assertTrue(second.disconnected)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), second.written.toByteArray())
    }

    @Test
    fun stopsAfterConfiguredRetryBudget() {
        var attempts = 0
        val delays = mutableListOf<Long>()
        val error = assertThrows(CloudSaveTransportException::class.java) {
            runBlocking {
                HttpsCloudSaveTransportClient(
                    maxRetries = 2,
                    retryDelay = { delays += it },
                    connectionFactory = {
                        attempts++
                        FakeConnection(status = 503)
                    },
                ).download(request.copy(payloadBytes = 0), CatalogCredentials("player", "secret"))
            }
        }

        assertTrue(error.recovery.retryable)
        assertEquals(3, attempts)
        assertEquals(listOf(1_000L, 2_000L), delays)
    }

    @Test
    fun rejectsDeclaredOversizedResponseBeforeOpeningBody() {
        val connection = FakeConnection(status = 200, declaredLength = 5)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                HttpsCloudSaveTransportClient(maxResponseBytes = 4, connectionFactory = { connection })
                    .download(request.copy(payloadBytes = 0), CatalogCredentials("player", "secret"))
            }
        }
        assertFalse(connection.inputOpened)
        assertTrue(connection.disconnected)
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

    private fun sha256(payload: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }

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
        private val declaredLength: Long = -1,
    ) : HttpURLConnection(URL("https://cloud.example")) {
        val written = ByteArrayOutputStream()
        var disconnected = false
        var inputOpened = false
        private val input = ByteArrayInputStream(response)

        override fun getResponseCode(): Int = status
        override fun getContentLengthLong(): Long = declaredLength
        override fun getInputStream(): ByteArrayInputStream {
            inputOpened = true
            return input
        }
        override fun getOutputStream() = written
        override fun disconnect() { disconnected = true }
        override fun usingProxy(): Boolean = false
        override fun connect() = Unit
    }
}
