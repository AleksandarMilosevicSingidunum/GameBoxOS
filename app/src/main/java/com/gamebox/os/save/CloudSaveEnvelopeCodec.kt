package com.gamebox.os.save

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest

data class CloudSaveEnvelope(
    val gameId: String,
    val updatedAtMillis: Long,
    val payload: ByteArray,
    val payloadSha256: String,
) {
    override fun equals(other: Any?): Boolean = other is CloudSaveEnvelope &&
        gameId == other.gameId && updatedAtMillis == other.updatedAtMillis &&
        payload.contentEquals(other.payload) && payloadSha256 == other.payloadSha256

    override fun hashCode(): Int = 31 * (31 * gameId.hashCode() + updatedAtMillis.hashCode()) + payload.contentHashCode()
}

object CloudSaveEnvelopeCodec {
    private val MAGIC = "GBOX_SAVE_V1\n".toByteArray(Charsets.US_ASCII)
    const val MAX_RAW_PAYLOAD_BYTES: Int = 15 * 1024 * 1024

    fun encode(gameId: String, updatedAtMillis: Long, payload: ByteArray): ByteArray {
        require(gameId.matches(Regex("^[a-z0-9][a-z0-9-]{0,95}$"))) { "Cloud save game id is invalid" }
        require(updatedAtMillis >= 0L) { "Cloud save timestamp is invalid" }
        require(payload.size <= MAX_RAW_PAYLOAD_BYTES) { "Cloud save payload exceeds the 15 MiB limit" }
        val gameIdBytes = gameId.toByteArray(Charsets.UTF_8)
        val checksum = sha256Bytes(payload)
        return ByteArrayOutputStream(MAGIC.size + gameIdBytes.size + payload.size + 64).use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.write(MAGIC)
                output.writeShort(gameIdBytes.size)
                output.write(gameIdBytes)
                output.writeLong(updatedAtMillis)
                output.writeInt(payload.size)
                output.write(checksum)
                output.write(payload)
            }
            bytes.toByteArray()
        }
    }

    fun decode(expectedGameId: String, encoded: ByteArray): CloudSaveEnvelope {
        require(encoded.size <= CloudSaveSyncContract.MAX_PAYLOAD_BYTES) { "Cloud save envelope exceeds the 16 MiB limit" }
        return DataInputStream(ByteArrayInputStream(encoded)).use { input ->
            require(input.readNBytes(MAGIC.size).contentEquals(MAGIC)) { "Cloud save envelope header is invalid" }
            val gameIdLength = input.readUnsignedShort()
            require(gameIdLength in 1..96) { "Cloud save envelope game id is invalid" }
            val gameId = input.readNBytes(gameIdLength).toString(Charsets.UTF_8)
            require(gameId == expectedGameId) { "Cloud save belongs to a different game" }
            val updatedAtMillis = input.readLong()
            require(updatedAtMillis >= 0L) { "Cloud save timestamp is invalid" }
            val payloadSize = input.readInt()
            require(payloadSize in 0..MAX_RAW_PAYLOAD_BYTES) { "Cloud save payload size is invalid" }
            val expectedChecksum = input.readNBytes(32)
            require(expectedChecksum.size == 32) { "Cloud save checksum is missing" }
            val payload = input.readNBytes(payloadSize)
            require(payload.size == payloadSize && input.read() == -1) { "Cloud save envelope length is invalid" }
            val actualChecksum = sha256Bytes(payload)
            require(actualChecksum.contentEquals(expectedChecksum)) { "Cloud save payload checksum mismatch" }
            CloudSaveEnvelope(gameId, updatedAtMillis, payload, actualChecksum.toHex())
        }
    }

    fun sha256(payload: ByteArray): String = sha256Bytes(payload).toHex()

    private fun sha256Bytes(payload: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(payload)

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
