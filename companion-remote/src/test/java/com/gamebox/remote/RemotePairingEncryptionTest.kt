package com.gamebox.remote

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class RemotePairingEncryptionTest {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    private val pairing = RemotePairing("192.168.1.8", 49_501, "ab".repeat(32))

    @Test
    fun encryptsAndRestoresRememberedPairing() {
        val ciphertext = encryptRemotePairing(pairing, key)

        assertFalse(ciphertext.toString(StandardCharsets.ISO_8859_1).contains(pairing.token))
        assertEquals(pairing, decryptRemotePairing(ciphertext, key))
    }

    @Test
    fun rejectsTamperedRememberedPairing() {
        val ciphertext = encryptRemotePairing(pairing, key).also { it[20] = (it[20].toInt() xor 1).toByte() }

        assertThrows(Exception::class.java) { decryptRemotePairing(ciphertext, key) }
    }

    @Test
    fun migratesPreviousCompanionPortFormat() {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        val legacy = cipher.iv + cipher.doFinal("192.168.1.8\n49500\n${pairing.token}\n".toByteArray())

        assertEquals(pairing, decryptRemotePairing(legacy, key))
    }
}
