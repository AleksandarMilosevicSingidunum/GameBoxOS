package com.gamebox.os.catalog

import java.net.HttpURLConnection
import java.io.IOException
import com.gamebox.os.provider.ProviderRecoveryDecision
import com.gamebox.os.provider.ProviderRecoveryPolicy
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.Base64

class CatalogTransportException(
    val recovery: ProviderRecoveryDecision,
    cause: Throwable? = null
) : IOException(recovery.userMessage, cause)

internal fun catalogTransportFailure(status: Int? = null, error: Throwable? = null): CatalogTransportException =
    CatalogTransportException(ProviderRecoveryPolicy.classify(status, error), error)

class HttpsCatalogTransportClient(
    private val maxResponseBytes: Int = 1_048_576,
    private val s3Signer: S3RequestSigner? = null,
) : CatalogTransportClient {
    override suspend fun fetch(transport: CatalogTransport, credentials: CatalogCredentials?): String {
        val uri = when (transport) {
            is CatalogTransport.Https -> URI(transport.url)
            is CatalogTransport.WebDav -> transport.catalogUri()
            is CatalogTransport.S3 -> transport.objectUri()
        }
        require(uri.scheme.equals("https", true)) { "catalog transport requires HTTPS" }
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/json")
        require((credentials?.username == null) == (credentials?.password == null)) { "WebDAV credentials must include both username and password" }
        credentials?.takeIf { it.hasBasicAuth() }?.let { auth ->
            val user = auth.username!!
            val pass = auth.password!!
            val token = Base64.getEncoder().encodeToString((user + ":" + pass).toByteArray())
            connection.setRequestProperty("Authorization", "Basic $token")
        }
        if (transport is CatalogTransport.S3 && credentials?.hasS3Auth() == true) {
            val signer = s3Signer ?: throw IllegalArgumentException("S3 signer required for access-key credentials")
            val emptyHash = MessageDigest.getInstance("SHA-256").digest(ByteArray(0)).joinToString("") { "%02x".format(it) }
            val signed = signer.sign("GET", uri.toString(), emptyHash, credentials)
            connection.setRequestProperty("x-amz-date", signed.date)
            connection.setRequestProperty("x-amz-content-sha256", emptyHash)
            connection.setRequestProperty("Authorization", signed.authorization)
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw catalogTransportFailure(status)
            val bytes = connection.inputStream.use { it.readNBytes(maxResponseBytes + 1) }
            require(bytes.size <= maxResponseBytes) { "catalog response is too large" }
            return bytes.toString(Charsets.UTF_8)
        } catch (error: CatalogTransportException) {
            throw error
        } catch (error: IOException) {
            throw catalogTransportFailure(error = error)
        } finally { connection.disconnect() }
    }
}