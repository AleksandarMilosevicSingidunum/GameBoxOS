package com.gamebox.os.companion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.settings.SettingsRepository
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
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
        settings.setCompanionConfiguration(enabled = true, port = port)
        try {
            CompanionEndpointService.start(context)
            val response = awaitStatus(port, secret)
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

    private fun awaitStatus(port: Int, secret: String): String {
        val deadline = System.currentTimeMillis() + 5_000L
        val path = CompanionStatusRoute.PATH
        while (System.currentTimeMillis() < deadline) {
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 250)
                    socket.soTimeout = 1_000
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
