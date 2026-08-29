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
    }
}