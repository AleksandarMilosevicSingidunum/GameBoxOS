package com.gamebox.os.catalog

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Base64

class HttpsCatalogTransportClient(private val maxResponseBytes: Int = 1_048_576) : CatalogTransportClient {
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
        credentials?.username?.let { user -> credentials.password?.let { pass ->
            val token = Base64.getEncoder().encodeToString((user + ":" + pass).toByteArray())
            connection.setRequestProperty("Authorization", "Basic $token")
        } }
        try {
            require(connection.responseCode in 200..299) { "catalog request failed with HTTP ${connection.responseCode}" }
            val bytes = connection.inputStream.use { it.readNBytes(maxResponseBytes + 1) }
            require(bytes.size <= maxResponseBytes) { "catalog response is too large" }
            return bytes.toString(Charsets.UTF_8)
        } finally { connection.disconnect() }
    }
}