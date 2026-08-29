package com.gamebox.os.download

import com.gamebox.os.catalog.validateAuthorizedCatalogUrl
import java.io.FilterInputStream
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

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

    override fun openInput(): InputStream {
        val connection = URL(validatedUrl).openConnection() as HttpsURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/octet-stream")
        connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
        val status = connection.responseCode
        if (status !in 200..299) {
            connection.disconnect()
            throw IllegalStateException(
                if (status in 300..399) "Download redirects are not accepted"
                else "Download request failed with HTTP " + status
            )
        }
        val declared = connection.contentLengthLong
        if (totalBytes != null && declared >= 0L && declared != totalBytes) {
            connection.disconnect()
            throw IllegalStateException("Download size does not match the catalog")
        }
        return object : FilterInputStream(connection.inputStream) {
            override fun close() {
                try {
                    super.close()
                } finally {
                    connection.disconnect()
                }
            }
        }
    }
}
