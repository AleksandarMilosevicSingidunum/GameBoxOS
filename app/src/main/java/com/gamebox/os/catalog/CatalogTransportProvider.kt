package com.gamebox.os.catalog

interface CatalogTransportClient {
    suspend fun fetch(transport: CatalogTransport, credentials: CatalogCredentials?): String
}

class CatalogTransportProvider(
    private val client: CatalogTransportClient,
    private val parser: CatalogParser,
    private val config: suspend () -> CatalogProviderConfig
) : CatalogProvider {
    override suspend fun load(): CatalogSnapshot {
        val selected = config()
        val credentials = selected.credentialKey?.let { key ->
            error("Credential resolution must be supplied by the host application for key: $key")
        }
        val payload = client.fetch(selected.transport, credentials)
        return parser.parse(payload)
    }
}

class NoopCatalogTransportClient : CatalogTransportClient {
    override suspend fun fetch(transport: CatalogTransport, credentials: CatalogCredentials?): String {
        throw UnsupportedOperationException("No catalog transport client configured for $transport")
    }
}