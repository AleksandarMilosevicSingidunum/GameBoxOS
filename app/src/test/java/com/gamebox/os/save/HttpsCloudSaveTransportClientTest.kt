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
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
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
    fun roundTripsPayloadThroughLoopbackHttpServerWithBasicAuthentication() = runBlocking {
        val payload = byteArrayOf(7, 4, 2, 9, 1)
        val received = AtomicReference<ByteArray?>(null)
        val methods = Collections.synchronizedList(mutableListOf<String>())
        val authorization = AtomicReference<String?>(null)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val executor = Executors.newSingleThreadExecutor()
        server.executor = executor
        server.createContext("/saves/game-1.bin") { exchange ->
            methods += exchange.requestMethod
            authorization.set(exchange.requestHeaders.getFirst("Authorization"))
            exchange.requestBody.use { body ->
                if (exchange.requestMethod == "PUT") received.set(body.readBytes())
            }
            if (exchange.requestMethod == "PUT") {
                exchange.sendResponseHeaders(204, -1)
            } else {
                val stored = received.get() ?: ByteArray(0)
                exchange.responseHeaders.set("Content-Type", "application/octet-stream")
                exchange.sendResponseHeaders(200, stored.size.toLong())
                exchange.responseBody.use { it.write(stored) }
            }
            exchange.close()
        }
        server.start()

        try {
            val endpoint = URI("https://cloud.example.test/saves/game-1.bin")
            val request = this@HttpsCloudSaveTransportClientTest.request.copy(
                endpoint = endpoint,
                payloadBytes = payload.size.toLong(),
                expectedSha256 = sha256(payload),
            )
            val client = HttpsCloudSaveTransportClient(
                connectionFactory = {
                    URL("http://127.0.0.1:${server.address.port}/saves/game-1.bin")
                        .openConnection() as HttpURLConnection
                },
            )
            val credentials = CatalogCredentials("player", "secret")

            client.upload(request, payload, credentials)
            val downloaded = client.download(request.copy(payloadBytes = 0), credentials)

            assertArrayEquals(payload, received.get())
            assertArrayEquals(payload, downloaded)
            assertEquals(listOf("PUT", "GET"), methods)
            assertTrue(authorization.get().orEmpty().startsWith("Basic "))
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
    }

    @Test
    fun roundTripsPayloadThroughLoopbackHttpServerWithSigV4Authentication() = runBlocking {
        val payload = byteArrayOf(3, 1, 4, 1, 5, 9)
        val received = AtomicReference<ByteArray?>(null)
        val authorization = AtomicReference<String?>(null)
        val amzDate = AtomicReference<String?>(null)
        val payloadHash = AtomicReference<String?>(null)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val executor = Executors.newSingleThreadExecutor()
        server.executor = executor
        server.createContext("/games/authorized/game-1.gamebox-save") { exchange ->
            authorization.set(exchange.requestHeaders.getFirst("Authorization"))
            amzDate.set(exchange.requestHeaders.getFirst("x-amz-date"))
            payloadHash.set(exchange.requestHeaders.getFirst("x-amz-content-sha256"))
            if (!authorization.get().orEmpty().startsWith("AWS4-HMAC-SHA256 ")) {
                exchange.sendResponseHeaders(403, -1)
                exchange.close()
                return@createContext
            }
            exchange.requestBody.use { body ->
                if (exchange.requestMethod == "PUT") received.set(body.readBytes())
            }
            if (exchange.requestMethod == "PUT") {
                exchange.sendResponseHeaders(204, -1)
            } else {
                val stored = received.get() ?: ByteArray(0)
                exchange.sendResponseHeaders(200, stored.size.toLong())
                exchange.responseBody.use { it.write(stored) }
            }
            exchange.close()
        }
        server.start()

        try {
            val endpoint = URI("https://objects.example.test/games/authorized/game-1.gamebox-save")
            val request = this@HttpsCloudSaveTransportClientTest.request.copy(
                endpoint = endpoint,
                payloadBytes = payload.size.toLong(),
                expectedSha256 = sha256(payload),
            )
            val client = HttpsCloudSaveTransportClient(
                s3Signer = com.gamebox.os.catalog.AwsSignatureV4Signer("eu-central-1"),
                connectionFactory = {
                    URL("http://127.0.0.1:${server.address.port}/games/authorized/game-1.gamebox-save")
                        .openConnection() as HttpURLConnection
                },
            )
            val credentials = CatalogCredentials(accessKey = "access-key", secretKey = "secret-key")

            client.upload(request, payload, credentials)
            val downloaded = client.download(request.copy(payloadBytes = 0), credentials)

            assertArrayEquals(payload, received.get())
            assertArrayEquals(payload, downloaded)
            assertTrue(authorization.get().orEmpty().startsWith("AWS4-HMAC-SHA256 "))
            assertTrue(amzDate.get().orEmpty().matches(Regex("\\d{8}T\\d{6}Z")))
            assertEquals(64, payloadHash.get().orEmpty().length)
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
    }

    @Test
    fun classifiesTruncatedDeclaredResponseAsRetryableTransportFailure() {
        val connection = FakeConnection(
            status = 200,
            response = byteArrayOf(1, 2, 3),
            declaredLength = 5,
        )
        val error = assertThrows(CloudSaveTransportException::class.java) {
            runBlocking {
                HttpsCloudSaveTransportClient(
                    maxRetries = 0,
                    connectionFactory = { connection },
                ).download(request.copy(payloadBytes = 0), CatalogCredentials("player", "secret"))
            }
        }

        assertTrue(error.recovery.retryable)
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

    @Test
    fun cancellationDisconnectsAnActiveBlockingConnection() = runBlocking {
        val connection = BlockingConnection()
        val created = CountDownLatch(1)
        val client = HttpsCloudSaveTransportClient(connectionFactory = {
            created.countDown()
            connection
        })
        val operation = async(Dispatchers.IO) {
            client.download(request.copy(payloadBytes = 0), CatalogCredentials("player", "secret"))
        }

        assertTrue(created.await(2, TimeUnit.SECONDS))
        operation.cancel()
        assertTrue(connection.disconnectedLatch.await(2, TimeUnit.SECONDS))
        operation.cancelAndJoin()
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

    private class BlockingConnection : HttpURLConnection(URL("https://cloud.example")) {
        val disconnectedLatch = CountDownLatch(1)
        @Volatile var disconnected = false

        override fun getResponseCode(): Int = 200
        override fun getInputStream(): InputStream = object : InputStream() {
            override fun read(): Int {
                while (!disconnected) Thread.sleep(5)
                throw IOException("connection disconnected")
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = read()
        }
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun disconnect() {
            disconnected = true
            disconnectedLatch.countDown()
        }
        override fun usingProxy(): Boolean = false
        override fun connect() = Unit
    }
}
