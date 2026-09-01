package com.gamebox.os.catalog

data class CatalogCredentials(
    val username: String? = null,
    val password: String? = null,
    val accessKey: String? = null,
    val secretKey: String? = null
)
 {
    override fun toString(): String = "CatalogCredentials(username=${username != null}, password=${password != null}, accessKey=${accessKey != null}, secretKey=${secretKey != null})"

    fun hasBasicAuth(): Boolean = !username.isNullOrBlank() && !password.isNullOrBlank()

    fun hasS3Auth(): Boolean = !accessKey.isNullOrBlank() && !secretKey.isNullOrBlank()
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

/** Connects the catalog transport layer to the encrypted application settings. */
class SettingsCatalogCredentialStore(
    private val load: (String) -> CatalogCredentials?
) : CatalogCredentialStore {
    override fun credentials(key: String): CatalogCredentials? = load(key)
}

