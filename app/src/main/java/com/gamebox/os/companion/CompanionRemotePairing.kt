package com.gamebox.os.companion

import java.security.SecureRandom
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class CompanionRemotePairingWindow(
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
) {
    private var code: String? = null
    private var expiresAtMillis = 0L
    private var attempts = 0

    @Synchronized
    fun open(): String {
        code = (random.nextInt(1_000_000)).toString().padStart(6, '0')
        expiresAtMillis = nowMillis() + PAIRING_WINDOW_MILLIS
        attempts = 0
        return requireNotNull(code)
    }

    @Synchronized
    fun remainingSeconds(): Long = ((expiresAtMillis - nowMillis()).coerceAtLeast(0) + 999) / 1_000

    @Synchronized
    fun close() = clear()

    @Synchronized
    fun consume(candidate: String): Boolean {
        val expected = code ?: return false
        if (nowMillis() >= expiresAtMillis || attempts >= MAX_ATTEMPTS) {
            clear()
            return false
        }
        attempts++
        if (candidate != expected || candidate.length != 6 || candidate.any { !it.isDigit() }) {
            if (attempts >= MAX_ATTEMPTS) clear()
            return false
        }
        clear()
        return true
    }

    @Synchronized
    private fun clear() {
        code = null
        expiresAtMillis = 0L
        attempts = 0
    }

    private companion object {
        const val PAIRING_WINDOW_MILLIS = 120_000L
        const val MAX_ATTEMPTS = 5
    }
}

internal object ActiveCompanionRemotePairing {
    internal val window = CompanionRemotePairingWindow()
    fun open(): String = window.open()
    fun remainingSeconds(): Long = window.remainingSeconds()
    fun consume(code: String): Boolean = window.consume(code)
    fun close() = window.close()
}

internal object CompanionRemoteControlRouteLock {
    private val lock = Any()

    fun <T> withLock(block: () -> T): T = synchronized(lock, block)
}

internal object CompanionPeerPolicy {
    fun isLocalNetwork(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isLinkLocalAddress) return true
        return when (address) {
            is Inet4Address -> {
                val bytes = address.address.map { it.toInt() and 0xff }
                bytes[0] == 10 || bytes[0] == 192 && bytes[1] == 168 ||
                    bytes[0] == 172 && bytes[1] in 16..31
            }
            is Inet6Address -> (address.address[0].toInt() and 0xfe) == 0xfc
            else -> false
        }
    }
}

internal object CompanionRemotePairRoute {
    const val PATH = "/v1/pair/remote"
    private val json = Json { ignoreUnknownKeys = false }

    fun handle(
        request: CompanionHttpRequest,
        pairingWindow: CompanionRemotePairingWindow = ActiveCompanionRemotePairing.window,
        createToken: () -> String,
        revokeToken: () -> Unit = {},
    ): CompanionHttpResponse {
        if (request.path != PATH) {
            return CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
        if (request.method == "DELETE" && request.bodyLength == 0L) {
            revokeToken()
            return CompanionHttpResponse(200, """{"revoked":true}""")
        }
        if (request.method != "PUT" || request.bodyLength !in 1..128) {
            return CompanionHttpResponse(400, """{"error":"invalid_pairing_request"}""")
        }
        val candidate = runCatching {
                json.parseToJsonElement(request.body.toString(Charsets.UTF_8)).jsonObject
                    .also { require(it.keys == setOf("code")) }
                    .getValue("code").jsonPrimitive.contentOrNull.orEmpty()
            }.getOrDefault("")
        if (!pairingWindow.consume(candidate)) {
            return CompanionHttpResponse(401, """{"error":"pairing_rejected"}""")
        }
        val token = runCatching(createToken).getOrElse {
            return CompanionHttpResponse(500, """{"error":"pairing_failed"}""")
        }
        if (!token.matches(Regex("[0-9a-fA-F]{64}"))) {
            return CompanionHttpResponse(500, """{"error":"pairing_failed"}""")
        }
        return CompanionHttpResponse(200, """{"token":"$token"}""")
    }
}
