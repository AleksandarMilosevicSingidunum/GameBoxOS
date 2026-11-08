package com.gamebox.remote

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.content.Context
import android.util.Base64
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URL
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64 as JavaBase64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.net.HttpURLConnection

internal data class RemotePairing(
    val host: String,
    val port: Int,
    val token: String,
)

internal class RemoteUnauthorizedException(message: String) : java.io.IOException(message)
internal class RemoteSearchUnavailableException(message: String) : java.io.IOException(message)

internal fun remoteHttpError(status: Int, response: String): java.io.IOException =
    if (status == 409 && response.contains("\"search_not_active\"")) {
        RemoteSearchUnavailableException("Open an app, channel, or country search on GameBox before editing text.")
    } else java.io.IOException("GameBox returned HTTP $status: $response")

internal fun remoteControlPort(companionPort: Int): Int = if (companionPort == 65_535) 65_534 else companionPort + 1

internal fun isAllowedRemoteAddress(address: InetAddress): Boolean {
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

internal fun privateRemoteAddress(host: String, addresses: List<InetAddress>): InetAddress {
    require(host.matches(Regex("[A-Za-z0-9.:-]{1,253}"))) { "Host must be an IP address or local hostname" }
    require(addresses.isNotEmpty() && addresses.all(::isAllowedRemoteAddress)) {
        "GameBox must resolve to a private local-network address"
    }
    return addresses.first()
}

internal fun encryptRemotePairing(pairing: RemotePairing, key: SecretKey): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val plaintext = listOf(pairing.host, pairing.port.toString(), pairing.token)
        .joinToString("\n").toByteArray(Charsets.UTF_8)
    return cipher.iv + cipher.doFinal(plaintext)
}

internal fun decryptRemotePairing(value: ByteArray, key: SecretKey): RemotePairing {
    require(value.size > 12)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, value.copyOfRange(0, 12)))
    val parts = cipher.doFinal(value.copyOfRange(12, value.size)).toString(Charsets.UTF_8).split('\n')
    return when (parts.size) {
        3 -> RemotePairing(parts[0], parts[1].toInt(), parts[2])
        4 -> RemotePairing(parts[0], remoteControlPort(parts[1].toInt()), parts[2])
        else -> error("Invalid saved GameBox pairing")
    }
}

internal data class DiscoveredGameBox(val host: String, val port: Int, val name: String)

internal fun discoverGameBox(): DiscoveredGameBox {
    val nonce = ByteArray(16).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    val request = "GAMEBOX_DISCOVER_V1:$nonce".toByteArray(Charsets.US_ASCII)
    return DatagramSocket().use { socket ->
        socket.broadcast = true
        socket.soTimeout = 3_000
        socket.send(DatagramPacket(request, request.size, java.net.InetAddress.getByName("255.255.255.255"), 49_499))
        val bytes = ByteArray(512)
        val response = DatagramPacket(bytes, bytes.size)
        socket.receive(response)
        val parts = response.data.copyOf(response.length).toString(Charsets.US_ASCII).split(':')
        require(parts.size == 4 && parts[0] == "GAMEBOX_HERE_V1" && parts[1] == nonce) { "Invalid GameBox discovery response" }
        val companionPort = parts[2].toIntOrNull()?.takeIf { it in 10_240..65_535 } ?: error("Invalid GameBox companion port")
        val name = JavaBase64.getDecoder().decode(parts[3]).toString(Charsets.UTF_8).take(192)
        DiscoveredGameBox(response.address.hostAddress ?: error("Discovered host address unavailable"), remoteControlPort(companionPort), name)
    }
}

internal fun reconnectRemembered(
    pairing: RemotePairing,
    discover: () -> DiscoveredGameBox = ::discoverGameBox,
): RemotePairing = runCatching {
    RemoteClient(pairing).send("status")
    pairing
}.getOrElse { failure ->
    if (failure is RemoteUnauthorizedException) throw failure
    val found = discover()
    pairing.copy(host = found.host, port = found.port).also { RemoteClient(it).send("status") }
}

internal class RemotePairingStore(context: Context) {
    private val preferences = context.getSharedPreferences("paired_host", Context.MODE_PRIVATE)

    fun save(pairing: RemotePairing) {
        val value = encryptRemotePairing(pairing, key())
        check(preferences.edit().putString("config", Base64.encodeToString(value, Base64.NO_WRAP)).commit())
    }

    fun load(): RemotePairing? = runCatching {
        val value = Base64.decode(preferences.getString("config", null), Base64.NO_WRAP)
        decryptRemotePairing(value, key())
    }.getOrNull()

    fun clear() { preferences.edit().remove("config").apply() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    companion object { private const val KEY_ALIAS = "gamebox-remote-pairing-v1" }
}

internal class RemoteClient(private val pairing: RemotePairing) {
    fun pair(code: String): RemotePairing {
        require(code.matches(Regex("[0-9]{6}"))) { "Pairing code must contain six digits" }
        val response = request(
            path = "/v1/pair/remote",
            method = "PUT",
            bytes = "{\"code\":\"$code\"}".toByteArray(Charsets.UTF_8),
            token = null,
        )
        val token = Regex("\\\"token\\\":\\\"([0-9a-fA-F]{64})\\\"")
            .find(response)?.groupValues?.get(1) ?: error("GameBox did not return a remote token")
        return pairing.copy(token = token)
    }

    fun send(action: String, text: String? = null): String {
        require(action in setOf("status", "up", "down", "left", "right", "select", "back", "home", "gamebox", "backspace", "play_pause", "text"))
        if (action == "text") require(!text.isNullOrEmpty() && text.length <= 128 && text.none(Char::isISOControl))
        require(pairing.token.matches(Regex("[0-9a-fA-F]{64}"))) { "This phone is not paired with GameBox" }
        val body = if (action == "status") null else
            "{\"action\":\"$action\"" + (text?.let { ",\"text\":${jsonString(it)}" } ?: "") + "}"
        val bytes = body?.toByteArray(Charsets.UTF_8)
        return request("/v1/remote", if (bytes == null) "GET" else "PUT", bytes, pairing.token)
    }

    fun revoke() {
        require(pairing.token.matches(Regex("[0-9a-fA-F]{64}"))) { "This phone is not paired with GameBox" }
        request("/v1/pair/remote", "DELETE", null, pairing.token)
    }

    private fun request(path: String, method: String, bytes: ByteArray?, token: String?): String {
        val address = validateHost()
        val hash = bytes?.sha256()
        val authorization = token?.let {
            val timestamp = System.currentTimeMillis() / 1_000L
            val nonce = ByteArray(16).also(SecureRandom()::nextBytes).toHex()
            val payload = "v2\n$method\n$path\n$timestamp\n$nonce" + (hash?.let { value -> "\n$value" } ?: "")
            val key = SecretKeySpec(it.hexToBytes(), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256").apply { init(key) }
                .doFinal(payload.toByteArray(Charsets.UTF_8)).toHex()
            "v2:$timestamp:$nonce:$mac"
        }
        val urlHost = requireNotNull(address.hostAddress).let {
            if (address is Inet6Address) "[${it.replace("%", "%25")}]" else it
        }
        val connection = URL("http://$urlHost:${pairing.port}$path").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = method
            authorization?.let { connection.setRequestProperty("X-GameBox-Authorization", it) }
            if (bytes != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/octet-stream")
                connection.setRequestProperty("X-GameBox-Content-SHA256", hash)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            val response = (if (status < 400) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText().take(512) }.orEmpty()
            if (status == 401) throw RemoteUnauthorizedException("GameBox returned HTTP 401: $response")
            if (status !in 200..299) throw remoteHttpError(status, response)
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun validateHost(): InetAddress {
        require(pairing.port in 10_240..65_535)
        return privateRemoteAddress(pairing.host, InetAddress.getAllByName(pairing.host).toList())
    }
    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private fun String.hexToBytes(): ByteArray = ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private fun jsonString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
}
