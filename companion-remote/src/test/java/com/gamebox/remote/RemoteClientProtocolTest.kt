package com.gamebox.remote

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteClientProtocolTest {
    @Test
    fun pairsWithSixDigitCodeWithoutSendingAnExistingToken() {
        val token = "cd".repeat(32)
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                val request = executor.submit<RemoteRequest> {
                    captureAndRespond(server, "{\"token\":\"$token\"}")
                }
                val client = RemoteClient(RemotePairing("127.0.0.1", server.localPort, ""))
                assertEquals(RemotePairing("127.0.0.1", server.localPort, token), client.pair("123456"))
                val captured = request.get(5, TimeUnit.SECONDS)
                assertEquals("PUT", captured.method)
                assertEquals("/v1/pair/remote", captured.path)
                assertEquals("{\"code\":\"123456\"}", captured.body.toString(Charsets.UTF_8))
                assertTrue("Pairing request must not carry a prior token", "x-gamebox-authorization" !in captured.headers)
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun rejectsMalformedPairingCodeBeforeNetworkRequest() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteClient(RemotePairing("127.0.0.1", 49_501, "")).pair("12345")
        }
    }

    @Test
    fun explainsWhenGameBoxHasNoActiveSearchField() {
        val error = remoteHttpError(409, """{"error":"search_not_active"}""")

        assertTrue(error is RemoteSearchUnavailableException)
        assertEquals(
            "Open an app, channel, or country search on GameBox before editing text.",
            error.message,
        )
    }

    @Test
    fun identifiesRevokedRememberedCredentialsAsUnauthorized() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                val request = executor.submit<RemoteRequest> {
                    captureAndRespond(server, responseStatus = 401, responseBody = "{\"error\":\"unauthorized\"}")
                }

                assertThrows(RemoteUnauthorizedException::class.java) {
                    RemoteClient(RemotePairing("127.0.0.1", server.localPort, "ab".repeat(32))).send("status")
                }
                assertEquals("GET", request.get(5, TimeUnit.SECONDS).method)
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun revokedRememberedPairingDoesNotTriggerHostDiscovery() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                val request = executor.submit<RemoteRequest> {
                    captureAndRespond(server, responseStatus = 401, responseBody = "{\"error\":\"unauthorized\"}")
                }
                var discoveryCalled = false

                assertThrows(RemoteUnauthorizedException::class.java) {
                    reconnectRemembered(RemotePairing("127.0.0.1", server.localPort, "ab".repeat(32))) {
                        discoveryCalled = true
                        error("A revoked token must not trigger discovery")
                    }
                }

                assertEquals("GET", request.get(5, TimeUnit.SECONDS).method)
                assertEquals(false, discoveryCalled)
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun rememberedConnectionDiscoversAndSavesCurrentHostWhenSavedAddressIsStale() {
        val token = "ab".repeat(32)
        val oldServer = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val oldPort = oldServer.localPort
        oldServer.close()
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { currentServer ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                val request = executor.submit<RemoteRequest> { captureAndRespond(currentServer) }
                val saved = RemotePairing("127.0.0.1", oldPort, token)
                val updated = reconnectRemembered(saved) {
                    DiscoveredGameBox("127.0.0.1", currentServer.localPort, "GameBox")
                }

                assertEquals(RemotePairing("127.0.0.1", currentServer.localPort, token), updated)
                assertEquals("GET", request.get(5, TimeUnit.SECONDS).method)
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun signsStatusAndControlRequestsWithExpectedHmacPayload() {
        val token = "ab".repeat(32)
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                val client = RemoteClient(RemotePairing("127.0.0.1", server.localPort, token))

                fun roundTrip(action: String, method: String, body: String) {
                    val request = executor.submit<RemoteRequest> { captureAndRespond(server) }
                    assertEquals("{\"status\":\"ready\"}", client.send(action))
                    assertSigned(request.get(5, TimeUnit.SECONDS), token, method, body)
                }
                roundTrip("status", "GET", "")
                roundTrip("right", "PUT", "{\"action\":\"right\"}")
            } finally {
                executor.shutdownNow()
            }
        }
    }

    private fun assertSigned(request: RemoteRequest, token: String, method: String, expectedBody: String) {
        assertEquals(method, request.method)
        assertEquals("/v1/remote", request.path)
        assertEquals(expectedBody, request.body.toString(Charsets.UTF_8))
        val authorization = request.headers.getValue("x-gamebox-authorization").split(':')
        assertEquals("v2", authorization[0])
        assertTrue(authorization[1].toLong() > 0)
        assertTrue(authorization[2].matches(Regex("[0-9a-f]{32}")))
        val bodyHash = request.body.sha256()
        if (method == "PUT") assertEquals(bodyHash, request.headers["x-gamebox-content-sha256"])
        val payload = "v2\n$method\n/v1/remote\n${authorization[1]}\n${authorization[2]}" +
            if (method == "PUT") "\n$bodyHash" else ""
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(token.hexToBytes(), "HmacSHA256")) }
            .doFinal(payload.toByteArray(Charsets.UTF_8)).toHex()
        assertEquals(mac, authorization[3])
    }

    private data class RemoteRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: ByteArray,
    )

    private fun captureAndRespond(
        server: ServerSocket,
        responseBody: String = "{\"status\":\"ready\"}",
        responseStatus: Int = 200,
    ): RemoteRequest = server.accept().use { socket ->
        socket.soTimeout = 3_000
        val input = BufferedInputStream(socket.getInputStream())
        val requestLine = input.readAsciiLine().split(' ')
        val headers = buildMap {
            while (true) {
                val line = input.readAsciiLine()
                if (line.isEmpty()) break
                val colon = line.indexOf(':')
                put(line.substring(0, colon).lowercase(), line.substring(colon + 1).trim())
            }
        }
        val body = ByteArray(headers["content-length"]?.toInt() ?: 0)
        input.readFully(body)
        val response = responseBody.toByteArray()
        socket.getOutputStream().apply {
            val statusText = if (responseStatus == 200) "OK" else "Unauthorized"
            write("HTTP/1.1 $responseStatus $statusText\r\nContent-Length: ${response.size}\r\nConnection: close\r\n\r\n".toByteArray())
            write(response)
            flush()
        }
        RemoteRequest(requestLine[0], requestLine[1], headers, body)
    }

    private fun BufferedInputStream.readAsciiLine(): String {
        val line = ByteArrayOutputStream()
        while (true) {
            val byte = read()
            require(byte >= 0) { "Unexpected end of HTTP request" }
            if (byte == '\r'.code) {
                require(read() == '\n'.code) { "Invalid HTTP line ending" }
                return line.toByteArray().toString(Charsets.US_ASCII)
            }
            line.write(byte)
        }
    }

    private fun BufferedInputStream.readFully(bytes: ByteArray) {
        var offset = 0
        while (offset < bytes.size) {
            val count = read(bytes, offset, bytes.size - offset)
            require(count > 0) { "Incomplete HTTP request body" }
            offset += count
        }
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun String.hexToBytes(): ByteArray = ByteArray(length / 2) {
        substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
