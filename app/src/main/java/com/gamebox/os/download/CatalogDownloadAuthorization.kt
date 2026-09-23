package com.gamebox.os.download

import com.gamebox.os.catalog.*
import java.net.URI
import java.util.Base64

/**
 * Credentials authorize only the configured catalog directory, never arbitrary
 * URLs supplied by a manifest. Secrets remain in memory, not WorkManager Data.
 */
internal class CatalogDownloadAuthorization(
    private val config: CatalogProviderConfig,
    private val credentials: CatalogCredentials?,
    private val signerFactory: (String) -> S3RequestSigner = ::AwsSignatureV4Signer,
) {
    fun headers(sourceUrl: String): Map<String, String> {
        val source = URI(validateAuthorizedCatalogUrl(sourceUrl))
        val catalog = when (val transport = config.transport) {
            is CatalogTransport.Https -> URI(validateAuthorizedCatalogUrl(transport.url))
            is CatalogTransport.WebDav -> transport.catalogUri()
            is CatalogTransport.S3 -> transport.objectUri()
        }
        if (!withinCatalogDirectory(source, catalog)) return emptyMap()
        // Pre-signed object URLs carry their own scoped authorization.
        if (source.rawQuery.orEmpty().split('&').any {
                it.substringBefore('=').equals("X-Amz-Signature", ignoreCase = true)
            }) return emptyMap()
        require(config.credentialKey == null || credentials != null) {
            "Configured download credentials are unavailable; check catalog settings"
        }
        val auth = credentials ?: return emptyMap()
        return when (val transport = config.transport) {
            is CatalogTransport.S3 -> {
                require(auth.hasS3Auth() && !auth.hasBasicAuth()) { "S3 download credentials are invalid" }
                val signed = signerFactory(transport.region).sign("GET", source.toASCIIString(), EMPTY_SHA256, auth)
                mapOf("Authorization" to signed.authorization, "x-amz-date" to signed.date,
                    "x-amz-content-sha256" to EMPTY_SHA256)
            }
            else -> {
                require(auth.hasBasicAuth() && !auth.hasS3Auth()) { "Download credentials are invalid" }
                val token = Base64.getEncoder().encodeToString(
                    (auth.username + ":" + auth.password).toByteArray(Charsets.UTF_8))
                mapOf("Authorization" to "Basic $token")
            }
        }
    }

    private fun withinCatalogDirectory(source: URI, catalog: URI): Boolean {
        if (!catalog.scheme.equals("https", true) || catalog.userInfo != null || catalog.fragment != null ||
            !source.host.equals(catalog.host, true) ||
            effectivePort(source) != effectivePort(catalog)) return false
        // Reject ambiguous paths rather than letting server decoding escape the credential scope.
        fun safePath(uri: URI): String? {
            val raw = uri.rawPath.orEmpty()
            if (Regex("%(2f|5c|25)", RegexOption.IGNORE_CASE).containsMatchIn(raw)) return null
            val path = uri.path.orEmpty().ifEmpty { "/" }
            if (path.contains('\\') || path.split('/').any { it == "." || it == ".." }) return null
            return path
        }
        val sourcePath = safePath(source) ?: return false
        val catalogPath = safePath(catalog) ?: return false
        val directory = catalogPath.substringBeforeLast('/', "") + "/"
        return sourcePath.startsWith(directory)
    }

    private fun effectivePort(uri: URI): Int = if (uri.port == -1) 443 else uri.port

    private companion object {
        const val EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
}
