package com.gamebox.os.companion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.settings.SettingsRepository
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompanionEndpointServiceLifecycleTest {
    @Test
    fun foregroundServiceServesAuthenticatedStatusAndStopsCleanly(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsRepository(context)
        val before = settings.settings.first()
        val existingSecret = settings.companionPairingSecret()
        val secret = existingSecret ?: settings.rotateCompanionPairingSecret()
        val port = 40_000 + (System.nanoTime().toInt() and 0x0fff)

        CompanionEndpointService.stop(context)
        awaitUnavailable(before.companionPort)
        val fingerprint = CompanionTlsIdentity.fingerprint()
        assertEquals(fingerprint, CompanionTlsIdentity.fingerprint())
        settings.setCompanionConfiguration(enabled = true, port = port)
        try {
            CompanionEndpointService.start(context)
            assertPlainHttpRejected(port)
            val response = awaitStatus(port, secret, fingerprint)
            assertTrue(response.startsWith("HTTP/1.1 200"))
            assertTrue(response.contains("\"status\":\"ready\""))
        } finally {
            CompanionEndpointService.stop(context)
            awaitUnavailable(port)
            settings.setCompanionConfiguration(before.companionEnabled, before.companionPort)
            if (existingSecret == null) settings.clearCompanionPairingSecret()
            if (before.companionEnabled && existingSecret != null) {
                CompanionEndpointService.start(context)
            }
        }
    }

    private fun awaitStatus(port: Int, secret: String, fingerprint: String): String {
        val deadline = System.currentTimeMillis() + 5_000L
        val path = CompanionStatusRoute.PATH
        while (System.currentTimeMillis() < deadline) {
            runCatching {
                (pinnedSocketFactory(fingerprint).createSocket() as SSLSocket).use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 250)
                    socket.soTimeout = 1_000
                    socket.startHandshake()
                    val authorization = CompanionProtocol.createAuthorization(
                        secret,
                        "GET",
                        path,
                        System.currentTimeMillis() / 1_000L,
                    )
                    val writer = socket.getOutputStream().bufferedWriter()
                    writer.write(
                        "GET $path HTTP/1.1\r\n" +
                            "Host: localhost\r\n" +
                            "${CompanionProtocol.AUTHORIZATION_HEADER}: $authorization\r\n\r\n"
                    )
                    writer.flush()
                    return socket.getInputStream().bufferedReader().readText()
                }
            }
            Thread.sleep(100)
        }
        throw AssertionError("Companion service did not answer on port $port")
    }

    private fun pinnedSocketFactory(fingerprint: String) =
        SSLContext.getInstance("TLS").apply {
            val expected = fingerprint.replace(":", "").lowercase()
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    val actual = MessageDigest.getInstance("SHA-256").digest(chain.first().encoded)
                        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                    assertEquals(expected, actual)
                }
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            init(null, arrayOf(trustManager), null)
        }.socketFactory

    private fun assertPlainHttpRejected(port: Int) {
        val response = runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), 500)
                socket.soTimeout = 1_000
                socket.getOutputStream().write("GET /v1/status HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray())
                socket.getInputStream().bufferedReader().readLine().orEmpty()
            }
        }.getOrDefault("")
        assertFalse("Companion must not answer a plaintext HTTP request", response.startsWith("HTTP/1.1"))
    }

    private fun awaitUnavailable(port: Int) {
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline) {
            val connected = runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 250)
                }
            }.isSuccess
            if (!connected) return
            Thread.sleep(100)
        }
        assertFalse("Companion service is still listening on port $port", true)
    }
}
