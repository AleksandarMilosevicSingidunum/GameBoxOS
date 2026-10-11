package com.gamebox.os.companion

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.LinkedHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Shared authenticated request format for the paired Windows companion endpoint. */
object CompanionProtocol {
    const val VERSION = 1
    const val AUTHORIZATION_HEADER = "X-GameBox-Authorization"
    private const val AUTHORIZATION_VERSION = 2
    private val random = SecureRandom()

    fun createAuthorization(
        secret: String,
        method: String,
        requestPath: String,
        unixTimeSeconds: Long,
        bodySha256: String? = null,
        nonce: String = newNonce(),
    ): String {
        require(validSecret(secret)) { "Pairing secret is invalid" }
        require(method.isNotBlank()) { "Method is required" }
        require(requestPath.startsWith('/') && !requestPath.contains("..")) {
            "Request path must be absolute and traversal-free"
        }
        require(unixTimeSeconds > 0) { "Timestamp is required" }
        require(bodySha256 == null || validSha256(bodySha256)) { "Body checksum is invalid" }
        require(validNonce(nonce)) { "Request nonce is invalid" }
        val basePayload = "v$AUTHORIZATION_VERSION\n${method.trim().uppercase()}\n$requestPath\n$unixTimeSeconds\n$nonce"
        val payload = bodySha256?.let { "$basePayload\n${it.lowercase()}" } ?: basePayload
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hexToBytes(secret), "HmacSHA256"))
        return "v$AUTHORIZATION_VERSION:$unixTimeSeconds:$nonce:${mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)).toHex()}"
    }

    fun verifyAuthorization(
        secret: String,
        method: String,
        requestPath: String,
        authorization: String?,
        nowUnixTimeSeconds: Long,
        allowedSkewSeconds: Long = 120,
        bodySha256: String? = null,
        acceptNonce: (String, Long) -> Boolean = { _, _ -> true },
    ): Boolean {
        if (!validSecret(secret) || authorization == null || allowedSkewSeconds < 0) return false
        val parts = authorization.split(':')
        if (parts.size != 4 || parts[0] != "v$AUTHORIZATION_VERSION" || !validNonce(parts[2])) return false
        val timestamp = parts[1].toLongOrNull() ?: return false
        if (kotlin.math.abs(nowUnixTimeSeconds - timestamp) > allowedSkewSeconds) return false
        val authenticated = runCatching {
            val expected = createAuthorization(secret, method, requestPath, timestamp, bodySha256, parts[2])
            constantTimeEquals(expected, authorization)
        }.getOrDefault(false)
        return authenticated && runCatching { acceptNonce(parts[2], timestamp) }.getOrDefault(false)
    }

    private fun newNonce(): String = ByteArray(16).also(random::nextBytes).toHex()

    private fun validSecret(value: String): Boolean = value.length == 64 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    private fun validSha256(value: String): Boolean =
        value.length == 64 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    private fun validNonce(value: String): Boolean =
        value.length == 32 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    private fun hexToBytes(value: String): ByteArray = ByteArray(value.length / 2) { index ->
        value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        val leftBytes = left.toByteArray(StandardCharsets.UTF_8)
        val rightBytes = right.toByteArray(StandardCharsets.UTF_8)
        if (leftBytes.size != rightBytes.size) return false
        var difference = 0
        leftBytes.indices.forEach { index -> difference = difference or (leftBytes[index].toInt() xor rightBytes[index].toInt()) }
        return difference == 0
    }
}

/** Bounded per-service replay window; authenticated requests only consume a nonce once. */
internal class CompanionAuthorizationReplayGuard(
    private val maxEntries: Int = 4_096,
) {
    private val seen = LinkedHashMap<String, Long>()

    init { require(maxEntries > 0) }

    @Synchronized
    fun accept(nonce: String, timestamp: Long, nowUnixTimeSeconds: Long): Boolean {
        seen.entries.removeAll { it.value < nowUnixTimeSeconds - 120 }
        if (nonce in seen || seen.size >= maxEntries) return false
        seen[nonce] = timestamp
        return true
    }
}

