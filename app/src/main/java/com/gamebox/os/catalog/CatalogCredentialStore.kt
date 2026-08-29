package com.gamebox.os.catalog

data class CatalogCredentials(
    val username: String? = null,
    val password: String? = null,
    val accessKey: String? = null,
    val secretKey: String? = null
)
 {
    override fun toString(): String = "CatalogCredentials(username=${username != null}, password=${password != null}, accessKey=${accessKey != null}, secretKey=${secretKey != null})"
}

interface CatalogCredentialStore {
    fun credentials(key: String): CatalogCredentials?
}

class InMemoryCatalogCredentialStore(
    private val values: Map<String, CatalogCredentials>
) : CatalogCredentialStore {
    init { require(values.keys.all { it.isNotBlank() }) { "credential keys must not be blank" } }
    override fun credentials(key: String): CatalogCredentials? = values[key]
}