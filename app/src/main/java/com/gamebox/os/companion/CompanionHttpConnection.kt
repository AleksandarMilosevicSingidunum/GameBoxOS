package com.gamebox.os.companion

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.Locale

internal data class CompanionHttpRequestHead(
    val method: String,
    val path: String,
    val authorization: String?,
    val contentLength: Long,
    val contentType: String?,
    val declaredBodySha256: String?,
    val fileName: String?,
    val configurationFlags: String?,
)

internal data class CompanionHttpRequest(
    val method: String,
    val path: String,
    val authorization: String?,
    val body: ByteArray = byteArrayOf(),
    val bodyFile: File? = null,
    val bodyLength: Long = body.size.toLong(),
    val bodySha256: String? = null,
    val fileName: String? = null,
    val configurationFlags: String? = null,
)

private class CompanionUnauthorizedRequest : IllegalArgumentException()
private class CompanionPayloadTooLarge : IllegalArgumentException()

/** Bounded HTTP/1 request reader with checksum-verified disk staging for large content uploads. */
internal object CompanionHttpRequestReader {
    const val BODY_SHA256_HEADER = "X-GameBox-Content-SHA256"
    const val FILE_NAME_HEADER = "X-GameBox-File-Name"
    const val CONFIGURATION_HEADER = "X-GameBox-Configuration"
    private const val MAX_HEAD_BYTES = 16 * 1024
    private const val MAX_LINE_BYTES = 4096
    private const val MAX_HEADERS = 32
    private const val MAX_SAVE_BYTES = 16 * 1024 * 1024L
    private const val MAX_CONTENT_BYTES = 64L * 1024 * 1024 * 1024
    private const val CONTENT_PREFIX = "/v1/content/"
    private val headerName = Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
    private val sha256Pattern = Regex("^[a-fA-F0-9]{64}$")

    fun read(
        input: InputStream,
        beforeRead: () -> Unit = {},
        bodyDirectory: File? = null,
        authorizeHead: (CompanionHttpRequestHead) -> Boolean = { true },
    ): CompanionHttpRequest {
        var headBytes = 0
        fun nextHeadByte(): Int {
            require(++headBytes <= MAX_HEAD_BYTES) { "Request head too large" }
            beforeRead()
            return input.read().also { if (it < 0) throw EOFException("Incomplete request") }
        }
        fun line(): String {
            val result = StringBuilder()
            while (true) {
                val value = nextHeadByte()
                if (value == 13) {
                    require(nextHeadByte() == 10) { "Invalid line ending" }
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
            val value = line()
            if (value.isEmpty()) break
            require(++count <= MAX_HEADERS) { "Too many headers" }
            val colon = value.indexOf(':')
            require(colon > 0 && headerName.matches(value.substring(0, colon))) { "Invalid header" }
            val name = value.substring(0, colon).lowercase(Locale.ROOT)
            require(name !in headers) { "Duplicate header" }
            headers[name] = value.substring(colon + 1).trim()
        }
        require("transfer-encoding" !in headers) { "Transfer encoding is not supported" }
        val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L
        require(contentLength >= 0) { "Request body is invalid" }
        val contentType = headers["content-type"]?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        if (contentLength > 0L) {
            require(parts[0] == "PUT" && contentType == "application/octet-stream") {
                "Only binary PUT bodies are supported"
            }
        }
        val isContentUpload = parts[1].startsWith(CONTENT_PREFIX)
        val maximum = if (isContentUpload) MAX_CONTENT_BYTES else MAX_SAVE_BYTES
        if (contentLength > maximum) throw CompanionPayloadTooLarge()
        val declaredHash = headers[BODY_SHA256_HEADER.lowercase(Locale.ROOT)]
        if (contentLength > 0L) require(declaredHash != null && sha256Pattern.matches(declaredHash)) {
            "A valid content checksum is required"
        }
        val head = CompanionHttpRequestHead(
            method = parts[0],
            path = parts[1],
            authorization = headers[CompanionProtocol.AUTHORIZATION_HEADER.lowercase(Locale.ROOT)],
            contentLength = contentLength,
            contentType = contentType,
            declaredBodySha256 = declaredHash?.lowercase(Locale.ROOT),
            fileName = headers[FILE_NAME_HEADER.lowercase(Locale.ROOT)],
            configurationFlags = headers[CONFIGURATION_HEADER.lowercase(Locale.ROOT)],
        )
        if (!authorizeHead(head)) throw CompanionUnauthorizedRequest()

        if (contentLength == 0L) {
            return CompanionHttpRequest(
                head.method, head.path, head.authorization,
                fileName = head.fileName, configurationFlags = head.configurationFlags,
            )
        }
        val digest = MessageDigest.getInstance("SHA-256")
        var remaining = contentLength
        val buffer = ByteArray(1024 * 1024)
        if (isContentUpload) {
            val directory = requireNotNull(bodyDirectory) { "Content staging directory is unavailable" }
            require(directory.exists() || directory.mkdirs()) { "Content staging directory is unavailable" }
            val staged = File.createTempFile("companion-", ".partial", directory)
            try {
                staged.outputStream().buffered().use { output ->
                    while (remaining > 0) {
                        beforeRead()
                        val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                        if (read < 0) throw EOFException("Incomplete request body")
                        if (read == 0) continue
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                        remaining -= read
                    }
                }
                val actual = digest.digest().toHex()
                require(actual == head.declaredBodySha256) { "Content checksum does not match" }
                return CompanionHttpRequest(
                    head.method, head.path, head.authorization,
                    bodyFile = staged, bodyLength = contentLength, bodySha256 = actual, fileName = head.fileName,
                    configurationFlags = head.configurationFlags,
                )
            } catch (failure: Throwable) {
                staged.delete()
                throw failure
            }
        }

        val output = ByteArrayOutputStream(contentLength.toInt())
        while (remaining > 0) {
            beforeRead()
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw EOFException("Incomplete request body")
            if (read == 0) continue
            digest.update(buffer, 0, read)
            output.write(buffer, 0, read)
            remaining -= read
        }
        val actual = digest.digest().toHex()
        require(actual == head.declaredBodySha256) { "Content checksum does not match" }
        return CompanionHttpRequest(
            head.method, head.path, head.authorization,
            body = output.toByteArray(), bodyLength = contentLength, bodySha256 = actual, fileName = head.fileName,
            configurationFlags = head.configurationFlags,
        )
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

/** Client errors/timeouts are scoped to this connection, never the listening service. */
internal class CompanionHttpConnection(
    private val requestTimeoutMillis: Int = 5_000,
    private val bodyDirectory: File? = null,
) {
    init { require(requestTimeoutMillis > 0) }

    fun handle(
        socket: Socket,
        authorizeHead: (CompanionHttpRequestHead) -> Boolean = { true },
        route: (CompanionHttpRequest) -> CompanionHttpResponse,
    ) {
        var stagedBody: File? = null
        try {
            val response = try {
                val input = socket.getInputStream().buffered()
                val request = CompanionHttpRequestReader.read(input, {
                    // Bound inactivity, not total duration: large owned-copy transfers may take hours.
                    socket.soTimeout = requestTimeoutMillis
                }, bodyDirectory, authorizeHead)
                stagedBody = request.bodyFile
                route(request)
            } catch (_: CompanionUnauthorizedRequest) {
                CompanionHttpResponse(401, """{"error":"unauthorized"}""")
            } catch (_: CompanionPayloadTooLarge) {
                CompanionHttpResponse(413, """{"error":"payload_too_large"}""")
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
                413 -> "Payload Too Large"
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
        } finally {
            stagedBody?.delete()
        }
    }
}
