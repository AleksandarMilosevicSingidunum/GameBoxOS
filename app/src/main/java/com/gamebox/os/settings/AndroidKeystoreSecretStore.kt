package com.gamebox.os.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AndroidKeystoreSecretStore(context: Context) {
    private val preferences = context.getSharedPreferences("gamebox_encrypted_secrets", Context.MODE_PRIVATE)

    @Synchronized
    fun put(name: String, value: String?) {
        require(name.matches(Regex("^[a-z0-9_.-]{1,64}$"))) { "Secret name is invalid" }
        val normalized = value?.trim().orEmpty()
        if (normalized.isEmpty()) {
            preferences.edit().remove(name).apply()
            return
        }
        require(normalized.length <= 4_096) { "Secret is too large" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(normalized.toByteArray(Charsets.UTF_8))
        val envelope = ByteArray(1 + cipher.iv.size + ciphertext.size)
        envelope[0] = cipher.iv.size.toByte()
        cipher.iv.copyInto(envelope, 1)
        ciphertext.copyInto(envelope, 1 + cipher.iv.size)
        preferences.edit().putString(name, Base64.encodeToString(envelope, Base64.NO_WRAP)).apply()
    }

    @Synchronized
    fun get(name: String): String? {
        val encoded = preferences.getString(name, null) ?: return null
        return runCatching {
            val envelope = Base64.decode(encoded, Base64.NO_WRAP)
            require(envelope.isNotEmpty())
            val ivSize = envelope[0].toInt() and 0xff
            require(ivSize in 12..16 && envelope.size > 1 + ivSize)
            val iv = envelope.copyOfRange(1, 1 + ivSize)
            val ciphertext = envelope.copyOfRange(1 + ivSize, envelope.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    fun contains(name: String): Boolean = get(name)?.isNotBlank() == true

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "gamebox_provider_secrets_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
