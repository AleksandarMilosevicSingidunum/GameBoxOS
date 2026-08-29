package com.gamebox.os.catalog

/** Configuration for catalog transports. Credentials are supplied out-of-band and never embedded in URLs. */
sealed interface CatalogTransport {
    data class Https(val url: String) : CatalogTransport
    data class WebDav(val baseUrl: String) : CatalogTransport
    data class S3(val endpoint: String, val bucket: String, val prefix: String = "") : CatalogTransport
}

data class CatalogProviderConfig(
    val transport: CatalogTransport,
    val credentialKey: String? = null
) {
    init {
        require(credentialKey == null || credentialKey.isNotBlank()) { "credential key must not be blank" }
        val endpoint = when (val value = transport) {
            is CatalogTransport.Https -> value.url
            is CatalogTransport.WebDav -> value.baseUrl
            is CatalogTransport.S3 -> value.endpoint
        }
        require(endpoint.startsWith("https://", ignoreCase = true)) { "catalog transports require HTTPS" }
        if (transport is CatalogTransport.S3) {
            require(transport.bucket.isNotBlank()) { "S3 bucket must not be blank" }
            require(!transport.bucket.contains("/") && !transport.bucket.contains(" ")) { "S3 bucket must be a single name" }
            require(!transport.prefix.split("/").any { it == ".." }) { "S3 prefix must not contain traversal segments" }
        }
    }
}