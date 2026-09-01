package com.gamebox.os.companion

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Shared protocol-v1 verification for a future paired Windows companion endpoint. */
object CompanionProtocol {
    const val VERSION = 1
    const val AUTHORIZATION_HEADER = "X-GameBox-Authorization"

    fun createAuthorization(secret: String, method: String, requestPath: String, unixTimeSeconds: Long): String {
        require(validSecret(secret)) { "Pairing secret is invalid" }
        require(method.isNotBlank()) { "Method is required" }
        require(requestPath.startsWith('/') && !requestPath.contains("..")) {
            "Request path must be absolute and traversal-free"
        }
        require(unixTimeSeconds > 0) { "Timestamp is required" }
        val payload = "v$VERSION\n${method.trim().uppercase()}\n$requestPath\n$unixTimeSeconds"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hexToBytes(secret), "HmacSHA256"))
        return "v$VERSION:$unixTimeSeconds:${mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)).toHex()}"
    }

    fun verifyAuthorization(
        secret: String,
        method: String,
        requestPath: String,
        authorization: String?,
        nowUnixTimeSeconds: Long,
        allowedSkewSeconds: Long = 120,
    ): Boolean {
        if (!validSecret(secret) || authorization == null || allowedSkewSeconds < 0) return false
        val parts = authorization.split(':')
        if (parts.size != 3 || parts[0] != "v$VERSION") return false
        val timestamp = parts[1].toLongOrNull() ?: return false
        if (kotlin.math.abs(nowUnixTimeSeconds - timestamp) > allowedSkewSeconds) return false
        return runCatching {
            val expected = createAuthorization(secret, method, requestPath, timestamp)
            constantTimeEquals(expected, authorization)
        }.getOrDefault(false)
    }

    private fun validSecret(value: String): Boolean = value.length == 64 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    private fun hexToBytes(value: String): ByteArray = ByteArray(value.length / 2) { index ->
        value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        val leftBytes = left.toByteArray(StandardCharsets.UTF_8)
        val rightBytes = right.toByteArray(StandardCharsets.UTF_8)
        if (leftBytes.size != rightBytes.size) return false
        var difference = 0
        leftBytes.indices.forEach { index -> difference = difference or (leftBytes[index].toInt() xor rightBytes[index].toInt()) }
        return difference == 0
    }
}

