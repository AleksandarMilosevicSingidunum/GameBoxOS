package com.gamebox.os.companion

import java.util.Base64

internal data class CompanionDiscoveryResponse(
    val nonce: String,
    val port: Int,
    val deviceName: String,
)

internal object CompanionDiscoveryProtocol {
    const val PORT = 49_499
    private const val REQUEST_PREFIX = "GAMEBOX_DISCOVER_V1:"
    private const val RESPONSE_PREFIX = "GAMEBOX_HERE_V1:"
    private val noncePattern = Regex("^[a-f0-9]{32}$")

    fun request(nonce: String): ByteArray {
        require(noncePattern.matches(nonce)) { "Discovery nonce is invalid" }
        return (REQUEST_PREFIX + nonce).toByteArray(Charsets.US_ASCII)
    }

    fun parseRequest(bytes: ByteArray, length: Int = bytes.size): String? {
        if (length !in 1..128 || length > bytes.size) return null
        val value = runCatching { bytes.copyOf(length).toString(Charsets.US_ASCII) }.getOrNull() ?: return null
        if (!value.startsWith(REQUEST_PREFIX)) return null
        return value.removePrefix(REQUEST_PREFIX).takeIf(noncePattern::matches)
    }

    fun response(nonce: String, port: Int, deviceName: String): ByteArray {
        require(noncePattern.matches(nonce)) { "Discovery nonce is invalid" }
        require(port in 10_240..65_535) { "Companion port is invalid" }
        val nameBytes = deviceName.trim().toByteArray(Charsets.UTF_8)
        require(nameBytes.size in 1..192) { "Device name is invalid" }
        return (RESPONSE_PREFIX + nonce + ":" + port + ":" +
            Base64.getEncoder().encodeToString(nameBytes)).toByteArray(Charsets.US_ASCII)
    }

    fun parseResponse(bytes: ByteArray, length: Int = bytes.size): CompanionDiscoveryResponse? {
        if (length !in 1..512 || length > bytes.size) return null
        val parts = bytes.copyOf(length).toString(Charsets.US_ASCII).split(':')
        if (parts.size != 4 || parts[0] != RESPONSE_PREFIX.removeSuffix(":") ||
            !noncePattern.matches(parts[1])) return null
        val port = parts[2].toIntOrNull()?.takeIf { it in 10_240..65_535 } ?: return null
        val name = runCatching {
            Base64.getDecoder().decode(parts[3]).toString(Charsets.UTF_8).trim()
        }.getOrNull()?.takeIf { it.isNotEmpty() && it.toByteArray(Charsets.UTF_8).size <= 192 } ?: return null
        return CompanionDiscoveryResponse(parts[1], port, name)
    }
}
