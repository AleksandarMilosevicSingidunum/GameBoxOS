package com.gamebox.os.catalog

import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AwsSignatureV4Signer(
    private val region: String,
    private val service: String = "s3",
    private val clock: Clock = Clock.systemUTC(),
) : S3RequestSigner {
    init { require(region.isNotBlank()) { "region must not be blank" } }

    override fun sign(method: String, uri: String, payloadSha256: String, credentials: CatalogCredentials): SignedRequest {
        require(method.isNotBlank() && payloadSha256.matches(Regex("[0-9a-fA-F]{64}")))
        val instant = Instant.now(clock)
        val amzDate = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(instant)
        val date = amzDate.substring(0, 8)
        val parsed = URI(uri)
        val host = parsed.host ?: throw IllegalArgumentException("S3 URI must include a host")
        val canonicalUri = (parsed.rawPath.ifEmpty { "/" }).split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        val canonicalQuery = parsed.rawQuery.orEmpty().split("&").filter { it.isNotBlank() }.sorted().joinToString("&")
        val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadSha256\nx-amz-date:$amzDate\n"
        val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
        val canonicalRequest = listOf(method.uppercase(), canonicalUri, canonicalQuery, canonicalHeaders, signedHeaders, payloadSha256).joinToString("\n")
        val scope = date + "/" + region + "/" + service + "/aws4_request"
        val kDate = HmacSha256.digest(("AWS4" + credentials.secretKey).toByteArray(), date)
        val kRegion = HmacSha256.digest(kDate, region)
        val kService = HmacSha256.digest(kRegion, service)
        val signingKey = HmacSha256.digest(kService, "aws4_request")
        val hash = HmacSha256.hex(java.security.MessageDigest.getInstance("SHA-256").digest(canonicalRequest.toByteArray()))
        val signature = HmacSha256.hex(HmacSha256.digest(signingKey, "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n" + hash))
        return SignedRequest("AWS4-HMAC-SHA256 Credential=" + credentials.accessKey + "/" + scope + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature, amzDate)
    }
}
