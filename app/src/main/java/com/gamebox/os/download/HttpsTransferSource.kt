package com.gamebox.os.download

import com.gamebox.os.catalog.validateAuthorizedCatalogUrl
import com.gamebox.os.provider.ProviderRecoveryPolicy
import java.io.FilterInputStream
import java.io.InputStream
import java.net.URL
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection

class RangeNotSupportedException(message: String) : IllegalStateException(message)

class DownloadRequestException(val status: Int) : IllegalStateException(
    if (status in 300..399) "Download redirects are not accepted"
    else ProviderRecoveryPolicy.classify(status).userMessage + " (HTTP " + status + ")"
)

data class OpenedRange(val input: InputStream, val totalBytes: Long?)

class HttpsTransferSource(
    sourceUrl: String,
    override val totalBytes: Long?,
    override val expectedSha256: String,
    private val requestHeaders: (String) -> Map<String, String> = { emptyMap() },
    private val connectionFactory: (String) -> HttpURLConnection = {
        URL(it).openConnection() as HttpsURLConnection
    },
) : TransferSource {
    private val validatedUrl = validateAuthorizedCatalogUrl(sourceUrl)

    init {
        require(expectedSha256.matches(Regex("^[a-fA-F0-9]{64}$"))) {
            "Expected SHA-256 must contain exactly 64 hexadecimal characters"
        }
        require(totalBytes == null || totalBytes >= 0L) { "Total size cannot be negative" }
    }

    override fun openInput(): InputStream = openInputAt(0L).input

    fun openInputAt(offset: Long): OpenedRange {
        require(offset >= 0L) { "Range offset cannot be negative" }
        val headers = requestHeaders(validatedUrl)
        val connection = connectionFactory(validatedUrl)
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/octet-stream")
            connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
            if (offset > 0L) connection.setRequestProperty("Range", "bytes=$offset-")
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val status = connection.responseCode
            if (offset > 0L && status == 416) {
                throw RangeNotSupportedException("Server cannot resume this partial download")
            }
            if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_PARTIAL) {
                throw DownloadRequestException(status)
            }
            if (offset > 0L && status != HttpURLConnection.HTTP_PARTIAL) {
                throw RangeNotSupportedException("Server did not honor the resume offset")
            }
            val responseTotal = if (status == HttpURLConnection.HTTP_PARTIAL) {
                val match = Regex("^bytes (\\d+)-(\\d+)/(\\d+)$")
                    .matchEntire(connection.getHeaderField("Content-Range").orEmpty())
                    ?: throw RangeNotSupportedException("Server returned an invalid Content-Range")
                val start = match.groupValues[1].toLongOrNull()
                val end = match.groupValues[2].toLongOrNull()
                val total = match.groupValues[3].toLongOrNull()
                if (start != offset || end == null || total == null || end < offset || end >= total) {
                    throw RangeNotSupportedException("Server returned an invalid resume range")
                }
                val length = connection.contentLengthLong
                if (length >= 0 && length != end - offset + 1) {
                    throw RangeNotSupportedException("Server returned an inconsistent range length")
                }
                total
            } else {
                connection.contentLengthLong.takeIf { it >= 0L }
            }
            if (totalBytes != null && responseTotal != null && responseTotal != totalBytes) {
                throw IllegalStateException("Download size does not match the catalog")
            }
            val stream = object : FilterInputStream(connection.inputStream) {
                override fun close() {
                    try {
                        super.close()
                    } finally {
                        connection.disconnect()
                    }
                }
            }
            return OpenedRange(stream, responseTotal)
        } catch (error: Exception) {
            connection.disconnect()
            throw error
        }
    }
}
