package com.gamebox.os.download

import com.gamebox.os.catalog.validateAuthorizedCatalogUrl
import java.io.FilterInputStream
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class RangeNotSupportedException(message: String) : IllegalStateException(message)

data class OpenedRange(val input: InputStream, val totalBytes: Long?)

class HttpsTransferSource(
    sourceUrl: String,
    override val totalBytes: Long?,
    override val expectedSha256: String
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
        val connection = URL(validatedUrl).openConnection() as HttpsURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/octet-stream")
        connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
        if (offset > 0L) connection.setRequestProperty("Range", "bytes=$offset-")
        val status = connection.responseCode
        if (status !in 200..299) {
            connection.disconnect()
            throw IllegalStateException(
                if (status in 300..399) "Download redirects are not accepted"
                else "Download request failed with HTTP " + status
            )
        }
        if (offset > 0L && status != HttpsURLConnection.HTTP_PARTIAL) {
            connection.disconnect()
            throw RangeNotSupportedException("Server did not honor the resume offset")
        }
        val contentRange = connection.getHeaderField("Content-Range")
        val responseTotal = if (offset > 0L) {
            val match = Regex("^bytes (\\d+)-(\\d+)/(\\d+)$").matchEntire(contentRange.orEmpty())
                ?: run {
                    connection.disconnect()
                    throw RangeNotSupportedException("Server returned an invalid Content-Range")
                }
            val start = match.groupValues[1].toLong()
            if (start != offset) {
                connection.disconnect()
                throw RangeNotSupportedException("Server resumed from the wrong offset")
            }
            match.groupValues[3].toLong()
        } else {
            connection.contentLengthLong.takeIf { it >= 0L }
        }
        if (totalBytes != null && responseTotal != null && responseTotal != totalBytes) {
            connection.disconnect()
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
    }
}
