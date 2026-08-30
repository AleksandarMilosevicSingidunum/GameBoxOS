package com.gamebox.os.save

import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.S3RequestSigner
import com.gamebox.os.provider.ProviderRecoveryDecision
import com.gamebox.os.provider.ProviderRecoveryPolicy
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.Base64

class CloudSaveTransportException(
    val recovery: ProviderRecoveryDecision,
    cause: Throwable? = null,
) : IOException(recovery.userMessage, cause)

class CloudSaveIntegrityException(
    val expectedSha256: String,
    val actualSha256: String,
) : IOException("Cloud save checksum mismatch")

class HttpsCloudSaveTransportClient(
    private val s3Signer: S3RequestSigner? = null,
    private val maxResponseBytes: Int = CloudSaveSyncContract.MAX_PAYLOAD_BYTES.toInt(),
    private val connectionFactory: (URI) -> HttpURLConnection = {
        URL(it.toString()).openConnection() as HttpURLConnection
    },
) {
    init {
        require(maxResponseBytes in 1..CloudSaveSyncContract.MAX_PAYLOAD_BYTES.toInt())
    }

    suspend fun upload(
        request: CloudSaveSyncRequest,
        payload: ByteArray,
        credentials: CatalogCredentials,
    ) {
        require(request.payloadBytes == payload.size.toLong()) { "Save payload size does not match request metadata" }
        requireReady(request, credentials)
        verifyIntegrity(request, payload)
        execute("PUT", request.endpoint, credentials, payload) { connection ->
            val status = connection.responseCode
            if (status !in 200..299) throw transportFailure(status)
        }
    }

    suspend fun download(
        request: CloudSaveSyncRequest,
        credentials: CatalogCredentials,
    ): ByteArray {
        requireReady(request.copy(payloadBytes = 0), credentials)
        return execute("GET", request.endpoint, credentials, ByteArray(0)) { connection ->
            val status = connection.responseCode
            if (status !in 200..299) throw transportFailure(status)
            val bytes = connection.inputStream.use { it.readNBytes(maxResponseBytes + 1) }
            if (bytes.size > maxResponseBytes) {
                throw IllegalArgumentException("Cloud save response exceeds the 16 MiB limit")
            }
            verifyIntegrity(request, bytes)
            bytes
        }
    }

    private fun requireReady(request: CloudSaveSyncRequest, credentials: CatalogCredentials) {
        val basicPairComplete = (credentials.username == null) == (credentials.password == null)
        val s3PairComplete = (credentials.accessKey == null) == (credentials.secretKey == null)
        require(basicPairComplete && s3PairComplete) { "Cloud credentials contain an incomplete pair" }
        require(credentials.hasBasicAuth() xor credentials.hasS3Auth()) {
            "Cloud credentials must contain exactly one supported authentication method"
        }
        val validation = CloudSaveSyncContract.validate(
            request = request,
            networkAvailable = true,
            credentialsAvailable = true,
        )
        require(validation.state == CloudSaveSyncState.READY) { validation.message }
    }

    private fun verifyIntegrity(request: CloudSaveSyncRequest, payload: ByteArray) {
        val expected = request.expectedSha256 ?: return
        val actual = sha256(payload)
        if (!actual.equals(expected, ignoreCase = true)) {
            throw CloudSaveIntegrityException(expected.lowercase(), actual)
        }
    }

    private fun <T> execute(
        method: String,
        endpoint: URI,
        credentials: CatalogCredentials,
        payload: ByteArray,
        block: (HttpURLConnection) -> T,
    ): T {
        val connection = connectionFactory(endpoint)
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/octet-stream")
        connection.setRequestProperty("Content-Type", "application/octet-stream")
        applyAuthentication(connection, method, endpoint, payload, credentials)
        if (method == "PUT") {
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(payload.size)
        }
        return try {
            if (method == "PUT") connection.outputStream.use { it.write(payload) }
            block(connection)
        } catch (error: CloudSaveTransportException) {
            throw error
        } catch (error: CloudSaveIntegrityException) {
            throw error
        } catch (error: IOException) {
            throw transportFailure(error = error)
        } finally {
            connection.disconnect()
        }
    }

    private fun applyAuthentication(
        connection: HttpURLConnection,
        method: String,
        endpoint: URI,
        payload: ByteArray,
        credentials: CatalogCredentials,
    ) {
        if (credentials.hasBasicAuth()) {
            val raw = credentials.username!! + ":" + credentials.password!!
            val token = Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
            connection.setRequestProperty("Authorization", "Basic $token")
            return
        }
        val signer = s3Signer ?: throw IllegalArgumentException("S3 signer required for access-key credentials")
        val payloadHash = sha256(payload)
        val signed = signer.sign(method, endpoint.toString(), payloadHash, credentials)
        connection.setRequestProperty("x-amz-date", signed.date)
        connection.setRequestProperty("x-amz-content-sha256", payloadHash)
        connection.setRequestProperty("Authorization", signed.authorization)
    }

    private fun sha256(payload: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }

    private fun transportFailure(status: Int? = null, error: Throwable? = null) =
        CloudSaveTransportException(ProviderRecoveryPolicy.classify(status, error), error)
}
