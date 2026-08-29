package com.gamebox.os.catalog

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.Base64

class HttpsCatalogTransportClient(
    private val maxResponseBytes: Int = 1_048_576,
    private val s3Signer: S3RequestSigner? = null,
) : CatalogTransportClient {
    override suspend fun fetch(transport: CatalogTransport, credentials: CatalogCredentials?): String {
        val uri = when (transport) {
            is CatalogTransport.Https -> URI(transport.url)
            is CatalogTransport.WebDav -> transport.catalogUri()
            is CatalogTransport.S3 -> transport.objectUri()
        }
        require(uri.scheme.equals("https", true)) { "catalog transport requires HTTPS" }
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/json")
        require((credentials?.username == null) == (credentials?.password == null)) { "WebDAV credentials must include both username and password" }\n        credentials?.username?.let { user -> credentials.password?.let { pass ->
            val token = Base64.getEncoder().encodeToString((user + ":" + pass).toByteArray())
            connection.setRequestProperty("Authorization", "Basic $token")
        } }
        if (transport is CatalogTransport.S3 && credentials?.accessKey != null && credentials.secretKey != null) {
            val signer = s3Signer ?: throw IllegalArgumentException("S3 signer required for access-key credentials")
            val emptyHash = MessageDigest.getInstance("SHA-256").digest(ByteArray(0)).joinToString("") { "%02x".format(it) }
            val signed = signer.sign("GET", uri.toString(), emptyHash, credentials)
            connection.setRequestProperty("x-amz-date", signed.date)
            connection.setRequestProperty("x-amz-content-sha256", emptyHash)
            connection.setRequestProperty("Authorization", signed.authorization)
        }
        try {
            require(connection.responseCode in 200..299) { "catalog request failed with HTTP " + connection.responseCode }
            val bytes = connection.inputStream.use { it.readNBytes(maxResponseBytes + 1) }
            require(bytes.size <= maxResponseBytes) { "catalog response is too large" }
            return bytes.toString(Charsets.UTF_8)
        } finally { connection.disconnect() }
    }
}