package com.gamebox.os.download

import java.io.InputStream
import java.security.MessageDigest

sealed interface VerificationResult {
    data object Verified : VerificationResult
    data class ChecksumMismatch(val actualHex: String) : VerificationResult
}

class Sha256Verifier {
    fun verify(input: InputStream, expectedHex: String): VerificationResult {
        require(SHA256_HEX.matches(expectedHex)) { "Expected checksum must be 64 hexadecimal characters" }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) digest.update(buffer, 0, count)
        }
        val actual = digest.digest()
        val expected = expectedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return if (MessageDigest.isEqual(actual, expected)) {
            VerificationResult.Verified
        } else {
            VerificationResult.ChecksumMismatch(actual.toHex())
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        val SHA256_HEX = Regex("[0-9a-fA-F]{64}")
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}
