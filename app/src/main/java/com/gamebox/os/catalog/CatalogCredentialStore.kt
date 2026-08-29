package com.gamebox.os.catalog

data class CatalogCredentials(
    val username: String? = null,
    val password: String? = null,
    val accessKey: String? = null,
    val secretKey: String? = null
)

interface CatalogCredentialStore {
    fun credentials(key: String): CatalogCredentials?
}

class InMemoryCatalogCredentialStore(
    private val values: Map<String, CatalogCredentials>
) : CatalogCredentialStore {
    override fun credentials(key: String): CatalogCredentials? = values[key]
}