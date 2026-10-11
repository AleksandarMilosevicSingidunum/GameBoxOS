package com.gamebox.os.companion

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.net.ServerSocket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.security.auth.x500.X500Principal

/** Persistent, self-signed server identity pinned by the paired Windows client. */
internal object CompanionTlsIdentity {
    private const val KEY_ALIAS = "gamebox-companion-tls-v5"

    fun fingerprint(): String = MessageDigest.getInstance("SHA-256")
        .digest(certificate().encoded)
        .joinToString(":") { "%02X".format(it.toInt() and 0xff) }

    fun createServerSocket(): ServerSocket {
        val keyStore = keyStore()
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, null)
        }
        val context = SSLContext.getInstance("TLS").apply { init(keyManagers.keyManagers, null, null) }
        return context.serverSocketFactory.createServerSocket() as SSLServerSocket
    }

    private fun certificate(): java.security.cert.Certificate = keyStore().getCertificate(KEY_ALIAS)
        ?: error("Companion TLS certificate is unavailable")

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
        if (!containsAlias(KEY_ALIAS)) {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or
                        KeyProperties.PURPOSE_DECRYPT,
                )
                    .setKeySize(2_048)
                    .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setSignaturePaddings(
                        KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                        KeyProperties.SIGNATURE_PADDING_RSA_PSS,
                    )
                    .setCertificateSubject(X500Principal("CN=GameBox Companion"))
                    .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis().coerceAtLeast(1L)))
                    .setCertificateNotBefore(Date(System.currentTimeMillis() - 86_400_000L))
                    .setCertificateNotAfter(Date(4_102_444_800_000L))
                    .build(),
            )
            generator.generateKeyPair()
        }
    }
}
