package com.gamebox.os.companion

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Locale

internal data class CompanionHttpRequest(
    val method: String,
    val path: String,
    val authorization: String?,
)

/** One bounded, body-free HTTP request. Never reads past the terminating CRLF. */
internal object CompanionHttpRequestReader {
    private const val MAX_HEAD_BYTES = 16 * 1024
    private const val MAX_LINE_BYTES = 4096
    private const val MAX_HEADERS = 32
    private val headerName = Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")

    fun read(input: InputStream, beforeRead: () -> Unit = {}): CompanionHttpRequest {
        var bytes = 0
        fun next(): Int {
            require(++bytes <= MAX_HEAD_BYTES) { "Request head too large" }
            beforeRead()
            return input.read().also { if (it < 0) throw EOFException("Incomplete request") }
        }
        fun line(): String {
            val result = StringBuilder()
            while (true) {
                val value = next()
                if (value == 13) {
                    require(next() == 10) { "Invalid line ending" }
                    return result.toString()
                }
                require(value in 32..126 && result.length < MAX_LINE_BYTES) { "Invalid or oversized line" }
                result.append(value.toChar())
            }
        }
        val parts = line().split(' ')
        require(parts.size == 3 && parts[0].matches(Regex("[A-Z]+")) &&
            parts[1].startsWith('/') && parts[2] in setOf("HTTP/1.0", "HTTP/1.1")) { "Invalid request line" }
        val headers = mutableMapOf<String, String>()
        var count = 0
        while (true) {
            val line = line()
            if (line.isEmpty()) break
            require(++count <= MAX_HEADERS) { "Too many headers" }
            val colon = line.indexOf(':')
            require(colon > 0 && headerName.matches(line.substring(0, colon))) { "Invalid header" }
            val name = line.substring(0, colon).lowercase(Locale.ROOT)
            require(name !in headers) { "Duplicate header" }
            headers[name] = line.substring(colon + 1).trim()
        }
        require("transfer-encoding" !in headers &&
            (headers["content-length"] == null || headers["content-length"] == "0")) { "Bodies are not supported" }
        return CompanionHttpRequest(parts[0], parts[1], headers[CompanionProtocol.AUTHORIZATION_HEADER.lowercase(Locale.ROOT)])
    }
}

/** Client errors/timeouts are scoped to this connection, never the listening service. */
internal class CompanionHttpConnection(private val requestTimeoutMillis: Int = 5_000) {
    init { require(requestTimeoutMillis > 0) }

    fun handle(socket: Socket, route: (CompanionHttpRequest) -> CompanionHttpResponse) {
        try {
            val response = try {
                val deadline = System.nanoTime() + requestTimeoutMillis * 1_000_000L
                val input = socket.getInputStream().buffered()
                val request = CompanionHttpRequestReader.read(input) {
                    val remaining = deadline - System.nanoTime()
                    if (remaining <= 0L) throw SocketTimeoutException()
                    socket.soTimeout = ((remaining + 999_999L) / 1_000_000L).toInt().coerceAtLeast(1)
                }
                route(request)
            } catch (_: SocketTimeoutException) {
                CompanionHttpResponse(408, """{"error":"request_timeout"}""")
            } catch (_: EOFException) {
                CompanionHttpResponse(400, """{"error":"bad_request"}""")
            } catch (_: IllegalArgumentException) {
                CompanionHttpResponse(400, """{"error":"bad_request"}""")
            }
            val reason = when (response.status) {
                200 -> "OK"
                400 -> "Bad Request"
                401 -> "Unauthorized"
                408 -> "Request Timeout"
                else -> "Not Found"
            }
            val body = response.body.toByteArray(Charsets.UTF_8)
            socket.getOutputStream().apply {
                write(("HTTP/1.1 ${response.status} $reason\r\nContent-Type: application/json\r\n" +
                    "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
                write(body)
                flush()
            }
        } catch (_: IOException) {
            // A disconnected or reset peer must not tear down the listening socket.
        }
    }
}
