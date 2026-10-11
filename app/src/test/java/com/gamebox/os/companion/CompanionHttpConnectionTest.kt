package com.gamebox.os.companion

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class CompanionHttpConnectionTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val now = 1_700_000_000L
    private fun request(authorization: String? = null, path: String = "/v1/status"): String =
        "GET $path HTTP/1.1\r\nHost: localhost\r\n" +
            (authorization?.let { "${CompanionProtocol.AUTHORIZATION_HEADER}: $it\r\n" } ?: "") + "\r\n"

    @Test fun authenticatedRequestNonceCanOnlyBeAcceptedOnce() {
        val guard = CompanionAuthorizationReplayGuard()
        val nonce = "0123456789abcdef0123456789abcdef"
        val authorization = CompanionProtocol.createAuthorization(
            secret, "PUT", "/v1/saves/game-one", now, bodySha256 = "a".repeat(64), nonce = nonce,
        )

        assertTrue(
            CompanionProtocol.verifyAuthorization(
                secret, "PUT", "/v1/saves/game-one", authorization, now,
                bodySha256 = "a".repeat(64), acceptNonce = { value, timestamp -> guard.accept(value, timestamp, now) },
            ),
        )
        assertFalse(
            CompanionProtocol.verifyAuthorization(
                secret, "PUT", "/v1/saves/game-one", authorization, now,
                bodySha256 = "a".repeat(64), acceptNonce = { value, timestamp -> guard.accept(value, timestamp, now) },
            ),
        )
    }

    @Test fun terminatorDoesNotRequireEofOrAnotherRead() {
        val bytes = ByteArrayInputStream(request().toByteArray())
        val input = object : InputStream() {
            override fun read(): Int {
                check(bytes.available() > 0) { "Read beyond HTTP header terminator" }
                return bytes.read()
            }
        }
        assertEquals("/v1/status", CompanionHttpRequestReader.read(input).path)
    }

    @Test fun rejectsMalformedTruncatedDuplicateAndOversizedRequests() {
        val invalid = listOf(
            "GET /v1/status HTTP/1.1\r\nHost: localhost\r\n",
            "GET /v1/status HTTP/1.1\n\n",
            request().replace("Host: localhost", "Host : localhost"),
            request().replace("Host: localhost", "Authorization: a\r\nAUTHORIZATION: b"),
            request().replace("Host: localhost", "X-Long: " + "a".repeat(5000)),
            request().replace("Host: localhost", (1..33).joinToString("\r\n") { "X-$it: a" }),
            request().replace("Host: localhost", (1..5).joinToString("\r\n") { "X-$it: " + "a".repeat(3500) }),
            request().replace("Host: localhost", "Content-Length: 1"),
            request().replace("Host: localhost", "Transfer-Encoding: chunked"),
            "GET http://example.com HTTP/1.1\r\n\r\n",
        )
        invalid.forEach { value ->
            assertTrue("Invalid request accepted", runCatching {
                CompanionHttpRequestReader.read(value.byteInputStream())
            }.isFailure)
        }
    }

    @Test fun readsOnlyDeclaredBoundedBinaryPutBody() {
        val body = "SAVE".toByteArray()
        val request = (
            "PUT /v1/saves/game-one HTTP/1.1\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                CompanionHttpRequestReader.BODY_SHA256_HEADER + ": " +
                    CompanionSaveTransferStore.sha256(body) + "\r\n" +
                "Content-Length: " + body.size + "\r\n\r\n"
            ).toByteArray() + body
        val parsed = CompanionHttpRequestReader.read(request.inputStream())
        assertEquals("PUT", parsed.method)
        assertArrayEquals(body, parsed.body)
        assertTrue(
            runCatching {
                CompanionHttpRequestReader.read(
                    (
                        "PUT /v1/saves/game-one HTTP/1.1\r\nContent-Type: application/octet-stream\r\n" +
                            CompanionHttpRequestReader.BODY_SHA256_HEADER + ": " + "0".repeat(64) +
                            "\r\nContent-Length: 4\r\n\r\nX"
                    ).byteInputStream()
                )
            }.isFailure
        )
    }

    @Test fun rejectsContentBeforeReadingWhenStagingSpaceIsInsufficient() {
        val directory = Files.createTempDirectory("companion-space-").toFile()
        try {
            val body = "NES-CONTENT".toByteArray()
            val hash = CompanionSaveTransferStore.sha256(body)
            val request = (
                "PUT /v1/content/galaxy-patrol HTTP/1.1\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    CompanionHttpRequestReader.BODY_SHA256_HEADER + ": " + hash + "\r\n" +
                    "Content-Length: " + body.size + "\r\n\r\n"
                ).toByteArray() + body

            val failure = runCatching {
                CompanionHttpRequestReader.read(
                    request.inputStream(),
                    bodyDirectory = directory,
                    availableSpace = { 0L },
                )
            }.exceptionOrNull()
            assertTrue(failure is CompanionStorageUnavailable)
            assertEquals(0, directory.listFiles()?.size ?: 0)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun rejectsContentAboveMaximumBeforeReadingBody() {
        val oversizedLength = 64L * 1024 * 1024 * 1024 + 1
        val header = (
            "PUT /v1/content/game-one HTTP/1.1\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                CompanionHttpRequestReader.BODY_SHA256_HEADER + ": " + "0".repeat(64) + "\r\n" +
                "Content-Length: " + oversizedLength + "\r\n\r\n"
        ).toByteArray()
        var bodyRead = false
        val input = object : ByteArrayInputStream(header) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                bodyRead = true
                return super.read(b, off, len)
            }
        }

        val directory = Files.createTempDirectory("companion-limit-").toFile()
        try {
            val failure = runCatching {
                CompanionHttpRequestReader.read(input, bodyDirectory = directory)
            }.exceptionOrNull()
            assertTrue(failure is CompanionPayloadTooLarge)
            assertFalse(bodyRead)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun reservesSpaceForStagingAndManagedImportCopies() {
        val directory = Files.createTempDirectory("companion-space-double-").toFile()
        try {
            val body = "NES-CONTENT".toByteArray()
            val hash = CompanionSaveTransferStore.sha256(body)
            val request = (
                "PUT /v1/content/galaxy-patrol HTTP/1.1\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    CompanionHttpRequestReader.BODY_SHA256_HEADER + ": " + hash + "\r\n" +
                    "Content-Length: " + body.size + "\r\n\r\n"
                ).toByteArray() + body

            val failure = runCatching {
                CompanionHttpRequestReader.read(
                    request.inputStream(),
                    bodyDirectory = directory,
                    availableSpace = { body.size.toLong() },
                )
            }.exceptionOrNull()

            assertTrue(failure is CompanionStorageUnavailable)
            assertEquals(0, directory.listFiles()?.size ?: 0)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun actualSocketRespondsBeforeClientClosesAndSurvivesBadClients() {
        val executor = Executors.newSingleThreadExecutor()
        val staging = Files.createTempDirectory("companion-http-space-").toFile()
        ServerSocket(0, 5, InetAddress.getLoopbackAddress()).use { listener ->
            listener.soTimeout = 5000
            val server = executor.submit {
                val connection = CompanionHttpConnection(200, staging) { 0L }
                val replayGuard = CompanionAuthorizationReplayGuard()
                repeat(6) {
                    listener.accept().use { socket ->
                        connection.handle(socket, authorizeHead = { head ->
                            val authorization = head.authorization
                            if (authorization == null) true else CompanionProtocol.verifyAuthorization(
                                secret,
                                head.method,
                                head.path,
                                authorization,
                                now,
                                bodySha256 = head.declaredBodySha256,
                                acceptNonce = { nonce, timestamp -> replayGuard.accept(nonce, timestamp, now) },
                            )
                        }) { request ->
                            CompanionStatusRoute.handle(request.method, request.path, request.authorization,
                                secret, "Test device", now)
                        }
                    }
                }
            }
            try {
                fun exchange(value: String): String = Socket(listener.inetAddress, listener.localPort).use { client ->
                    client.soTimeout = 3000
                    client.getOutputStream().apply { write(value.toByteArray()); flush() }
                    // Do not close/shutdown the write side: normal HTTP clients wait for a response.
                    client.getInputStream().bufferedReader().readText()
                }
                assertTrue(exchange(request()).startsWith("HTTP/1.1 401"))
                assertTrue(exchange("GET /v1/status HTTP/1.1\r\nHost: ").startsWith("HTTP/1.1 408"))
                assertTrue(exchange(request().replace("Host: localhost", "Content-Length: 9"))
                    .startsWith("HTTP/1.1 400"))
                val authorization = CompanionProtocol.createAuthorization(secret, "GET", "/v1/status", now)
                val success = exchange(request(authorization))
                assertTrue(success.startsWith("HTTP/1.1 200"))
                assertTrue(success.contains("\"status\":\"ready\""))
                assertTrue(exchange(request(authorization)).startsWith("HTTP/1.1 401"))
                val content = "CONTENT".toByteArray()
                val contentRequest = (
                    "PUT /v1/content/game-one HTTP/1.1\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        CompanionHttpRequestReader.BODY_SHA256_HEADER + ": " +
                            CompanionSaveTransferStore.sha256(content) + "\r\n" +
                        "Content-Length: " + content.size + "\r\n\r\n"
                    ).toByteArray() + content
                assertTrue(exchange(String(contentRequest, Charsets.ISO_8859_1)).startsWith("HTTP/1.1 507"))
                server.get(5, TimeUnit.SECONDS)
            } finally { executor.shutdownNow() }
        }
        staging.deleteRecursively()
    }

    @Test fun routeFailureReturnsServerErrorAndNextRequestStillWorks() {
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0, 2, InetAddress.getLoopbackAddress()).use { listener ->
            val connection = CompanionHttpConnection()
            val server = executor.submit {
                repeat(2) {
                    listener.accept().use { socket ->
                        connection.handle(socket) { request ->
                            if (request.path == "/explode") error("simulated route failure")
                            CompanionStatusRoute.handle(
                                request.method, request.path, request.authorization, secret, "Test device", now,
                            )
                        }
                    }
                }
            }
            try {
                fun exchange(value: String): String = Socket(listener.inetAddress, listener.localPort).use { client ->
                    client.getOutputStream().apply { write(value.toByteArray()); flush() }
                    client.getInputStream().bufferedReader().readText()
                }

                val authorization = CompanionProtocol.createAuthorization(secret, "GET", "/explode", now)
                assertTrue(exchange(request(authorization, "/explode")).startsWith("HTTP/1.1 500 Internal Server Error"))

                val statusAuthorization = CompanionProtocol.createAuthorization(secret, "GET", "/v1/status", now)
                assertTrue(exchange(request(statusAuthorization)).startsWith("HTTP/1.1 200 OK"))
                server.get(5, TimeUnit.SECONDS)
            } finally {
                executor.shutdownNow()
            }
        }
    }
}
