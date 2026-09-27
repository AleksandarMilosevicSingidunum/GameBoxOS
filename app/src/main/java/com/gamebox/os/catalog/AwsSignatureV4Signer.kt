package com.gamebox.os.catalog

import java.net.URI
import java.nio.charset.StandardCharsets
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
        val canonicalUri = canonicalUriForSigning(uri)
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

/**
 * Canonicalizes an already parsed URI path without double-encoding existing escapes.
 * S3 object keys may legitimately contain spaces, plus signs, or non-ASCII bytes.
 */
internal fun canonicalUriForSigning(uri: String): String {
    val rawPath = URI(uri).rawPath.ifEmpty { "/" }
    return rawPath.split('/').joinToString("/") { canonicalPathSegment(it) }
}

private fun canonicalPathSegment(segment: String): String {
    val result = StringBuilder(segment.length)
    var index = 0
    while (index < segment.length) {
        val character = segment[index]
        if (character == '%' && index + 2 < segment.length &&
            hexValue(segment[index + 1]) >= 0 && hexValue(segment[index + 2]) >= 0
        ) {
            result.append('%')
                .append(segment[index + 1].uppercaseChar())
                .append(segment[index + 2].uppercaseChar())
            index += 3
            continue
        }

        val codePoint = segment.codePointAt(index)
        String(Character.toChars(codePoint)).toByteArray(StandardCharsets.UTF_8).forEach { byte ->
            val value = byte.toInt() and 0xff
            val decoded = value.toChar()
            if (value < 128 && isUnreserved(decoded)) result.append(decoded)
            else result.append('%').append(HEX[value ushr 4]).append(HEX[value and 0x0f])
        }
        index += Character.charCount(codePoint)
    }
    return result.toString()
}

private fun isUnreserved(value: Char): Boolean =
    value.isLetterOrDigit() || value == '-' || value == '_' || value == '.' || value == '~'

private fun hexValue(value: Char): Int = when (value) {
    in '0'..'9' -> value - '0'
    in 'a'..'f' -> value - 'a' + 10
    in 'A'..'F' -> value - 'A' + 10
    else -> -1
}

private val HEX = "0123456789ABCDEF"
