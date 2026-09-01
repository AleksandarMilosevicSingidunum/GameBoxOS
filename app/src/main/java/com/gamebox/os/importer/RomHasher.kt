package com.gamebox.os.importer

import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32

data class RomHashes(
    val crc32: String,
    val md5: String,
    val sha1: String,
    val sha256: String,
    val sizeBytes: Long,
)

object RomHasher {
    fun hash(
        input: InputStream,
        maxBytes: Long = 64L * 1024 * 1024 * 1024,
        onChunk: ((buffer: ByteArray, count: Int) -> Unit)? = null,
    ): RomHashes {
        require(maxBytes > 0) { "Maximum ROM size must be positive" }
        val crc32 = CRC32()
        val md5 = MessageDigest.getInstance("MD5")
        val sha1 = MessageDigest.getInstance("SHA-1")
        val sha256 = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            require(total <= maxBytes) { "ROM exceeds the configured import limit" }
            crc32.update(buffer, 0, count)
            md5.update(buffer, 0, count)
            sha1.update(buffer, 0, count)
            sha256.update(buffer, 0, count)
            onChunk?.invoke(buffer, count)
        }
        return RomHashes(
            crc32 = crc32.value.toString(16).padStart(8, '0'),
            md5 = md5.digest().toHex(),
            sha1 = sha1.digest().toHex(),
            sha256 = sha256.digest().toHex(),
            sizeBytes = total,
        )
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
}
