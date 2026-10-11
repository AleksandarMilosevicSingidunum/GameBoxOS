package com.gamebox.os.settings

import android.content.Context
import com.gamebox.os.catalog.CatalogCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

/** Owns credential naming and secure persistence, separate from ordinary settings preferences. */
internal class SettingsCredentialStore(context: Context) {
    private val secrets = AndroidKeystoreSecretStore(context.applicationContext)

    suspend fun cloudSaveCredentials(provider: String): CatalogCredentials? = withContext(Dispatchers.IO) {
        when (provider.uppercase()) {
            "WEBDAV" -> CatalogCredentials(
                username = secrets.get(CLOUD_SAVE_USERNAME),
                password = secrets.get(CLOUD_SAVE_PASSWORD),
            ).takeIf(CatalogCredentials::hasBasicAuth)
            "S3" -> CatalogCredentials(
                accessKey = secrets.get(CLOUD_SAVE_ACCESS_KEY),
                secretKey = secrets.get(CLOUD_SAVE_SECRET_KEY),
            ).takeIf(CatalogCredentials::hasS3Auth)
            else -> null
        }
    }

    suspend fun setCloudSaveCredentials(provider: String, identity: String?, secret: String?) =
        withContext(Dispatchers.IO) {
            when (provider.uppercase()) {
                "WEBDAV" -> {
                    secrets.put(CLOUD_SAVE_USERNAME, identity)
                    secrets.put(CLOUD_SAVE_PASSWORD, secret)
                    secrets.put(CLOUD_SAVE_ACCESS_KEY, null)
                    secrets.put(CLOUD_SAVE_SECRET_KEY, null)
                }
                "S3" -> {
                    secrets.put(CLOUD_SAVE_ACCESS_KEY, identity)
                    secrets.put(CLOUD_SAVE_SECRET_KEY, secret)
                    secrets.put(CLOUD_SAVE_USERNAME, null)
                    secrets.put(CLOUD_SAVE_PASSWORD, null)
                }
                else -> require(false) { "Unsupported cloud save provider" }
            }
        }

    suspend fun clearCloudSaveCredentials() = withContext(Dispatchers.IO) {
        secrets.put(CLOUD_SAVE_USERNAME, null)
        secrets.put(CLOUD_SAVE_PASSWORD, null)
        secrets.put(CLOUD_SAVE_ACCESS_KEY, null)
        secrets.put(CLOUD_SAVE_SECRET_KEY, null)
    }

    suspend fun companionPairingSecret(): String? = withContext(Dispatchers.IO) {
        secrets.get(COMPANION_PAIRING_SECRET)
    }

    suspend fun rotateCompanionPairingSecret(): String = withContext(Dispatchers.IO) {
        val secret = ByteArray(32).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        secrets.put(COMPANION_PAIRING_SECRET, secret)
        secret
    }

    suspend fun clearCompanionPairingSecret() = withContext(Dispatchers.IO) {
        secrets.put(COMPANION_PAIRING_SECRET, null)
    }

    fun catalogCredentials(key: String): CatalogCredentials? = when (key) {
        CATALOG_WEBDAV_CREDENTIALS, CATALOG_HTTPS_CREDENTIALS -> CatalogCredentials(
            username = secrets.get(CATALOG_USERNAME),
            password = secrets.get(CATALOG_PASSWORD),
        ).takeIf(CatalogCredentials::hasBasicAuth)
        CATALOG_S3_CREDENTIALS -> CatalogCredentials(
            accessKey = secrets.get(CATALOG_ACCESS_KEY),
            secretKey = secrets.get(CATALOG_SECRET_KEY),
        ).takeIf(CatalogCredentials::hasS3Auth)
        else -> null
    }

    suspend fun hasCatalogCredentials(transport: String): Boolean = withContext(Dispatchers.IO) {
        catalogCredentials(catalogCredentialKey(transport)) != null
    }

    suspend fun setCatalogCredentials(transport: String, identity: String?, secret: String?) =
        withContext(Dispatchers.IO) {
            when (transport.uppercase()) {
                "S3" -> {
                    secrets.put(CATALOG_ACCESS_KEY, identity)
                    secrets.put(CATALOG_SECRET_KEY, secret)
                    secrets.put(CATALOG_USERNAME, null)
                    secrets.put(CATALOG_PASSWORD, null)
                }
                "HTTPS", "WEBDAV" -> {
                    secrets.put(CATALOG_USERNAME, identity)
                    secrets.put(CATALOG_PASSWORD, secret)
                    secrets.put(CATALOG_ACCESS_KEY, null)
                    secrets.put(CATALOG_SECRET_KEY, null)
                }
                else -> require(false) { "Unsupported catalog transport" }
            }
        }

    suspend fun clearCatalogCredentials() = withContext(Dispatchers.IO) {
        secrets.put(CATALOG_USERNAME, null)
        secrets.put(CATALOG_PASSWORD, null)
        secrets.put(CATALOG_ACCESS_KEY, null)
        secrets.put(CATALOG_SECRET_KEY, null)
    }

    internal fun catalogCredentialKey(transport: String): String = when (transport.uppercase()) {
        "S3" -> CATALOG_S3_CREDENTIALS
        "WEBDAV" -> CATALOG_WEBDAV_CREDENTIALS
        else -> CATALOG_HTTPS_CREDENTIALS
    }

    private companion object {
        const val CATALOG_USERNAME = "catalog_username"
        const val CATALOG_PASSWORD = "catalog_password"
        const val CATALOG_ACCESS_KEY = "catalog_access_key"
        const val CATALOG_SECRET_KEY = "catalog_secret_key"
        const val CATALOG_HTTPS_CREDENTIALS = "catalog-https"
        const val CATALOG_WEBDAV_CREDENTIALS = "catalog-webdav"
        const val CATALOG_S3_CREDENTIALS = "catalog-s3"
        const val CLOUD_SAVE_USERNAME = "cloud_save_username"
        const val CLOUD_SAVE_PASSWORD = "cloud_save_password"
        const val CLOUD_SAVE_ACCESS_KEY = "cloud_save_access_key"
        const val CLOUD_SAVE_SECRET_KEY = "cloud_save_secret_key"
        const val COMPANION_PAIRING_SECRET = "companion_pairing_secret"
    }
}
