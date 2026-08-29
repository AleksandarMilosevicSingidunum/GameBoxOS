package com.gamebox.os.catalog

class CatalogProviderFactory(
    private val parser: CatalogParser = CatalogParser(),
    private val credentials: CatalogCredentialStore? = null,
    private val client: CatalogTransportClient = HttpsCatalogTransportClient()
) {
    fun create(config: suspend () -> CatalogProviderConfig): CatalogProvider =
        CatalogTransportProvider(client, parser, config, credentials)
}