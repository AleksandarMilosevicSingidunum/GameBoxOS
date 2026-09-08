package com.gamebox.os.companion

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class CompanionHttpConnectionTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val now = 1_700_000_000L
    private fun request(authorization: String? = null): String =
        "GET /v1/status HTTP/1.1\r\nHost: localhost\r\n" +
            (authorization?.let { "${CompanionProtocol.AUTHORIZATION_HEADER}: $it\r\n" } ?: "") + "\r\n"

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

    @Test fun actualSocketRespondsBeforeClientClosesAndSurvivesBadClients() {
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0, 4, InetAddress.getLoopbackAddress()).use { listener ->
            listener.soTimeout = 5000
            val server = executor.submit {
                val connection = CompanionHttpConnection(200)
                repeat(4) {
                    listener.accept().use { socket ->
                        connection.handle(socket) { request ->
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
                server.get(5, TimeUnit.SECONDS)
            } finally { executor.shutdownNow() }
        }
    }
}
