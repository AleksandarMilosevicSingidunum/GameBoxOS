package com.gamebox.os.catalog

interface CatalogTransportClient {
    suspend fun fetch(transport: CatalogTransport, credentials: CatalogCredentials?): String
}

class CatalogTransportProvider(
    private val client: CatalogTransportClient,
    private val parser: CatalogParser,
    private val config: suspend () -> CatalogProviderConfig,
    private val credentials: CatalogCredentialStore? = null
) : CatalogProvider {
    override suspend fun load(): CatalogSnapshot {
        val selected = config()
        val resolvedCredentials = selected.credentialKey?.let { key -> credentials?.credentials(key) }
        require(selected.credentialKey == null || resolvedCredentials != null) {
            "Configured catalog credentials are unavailable"
        }
        val payload = client.fetch(selected.transport, resolvedCredentials)
        return parser.parse(payload)
    }
}

class NoopCatalogTransportClient : CatalogTransportClient {
    override suspend fun fetch(transport: CatalogTransport, credentials: CatalogCredentials?): String {
        throw UnsupportedOperationException("No catalog transport client configured for $transport")
    }
}