package com.gamebox.os.companion

import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionRemotePairingTest {
    @Test
    fun codePairsOnceAndExpiresAfterTwoMinutes() {
        var now = 1_000L
        val window = CompanionRemotePairingWindow(nowMillis = { now })
        val code = window.open()
        assertTrue(code.matches(Regex("[0-9]{6}")))
        assertTrue(window.consume(code))
        assertFalse(window.consume(code))

        val expiring = window.open()
        now += 120_001L
        assertFalse(window.consume(expiring))
        assertEquals(0L, window.remainingSeconds())
    }

    @Test
    fun failedAttemptsAreBoundedAndTheWindowCanBeRevoked() {
        val window = CompanionRemotePairingWindow()
        val code = window.open()
        val wrongCode = if (code == "000000") "000001" else "000000"
        repeat(5) { assertFalse(window.consume(wrongCode)) }
        assertFalse(window.consume(code))
        window.open()
        window.close()
        assertFalse(window.consume(code))
    }

    @Test
    fun pairingRouteIssuesOneTimeTokenAndRejectsReplays() {
        val window = CompanionRemotePairingWindow()
        val code = window.open()
        val token = "a".repeat(64)
        val request = pairRequest(code)
        val response = CompanionRemotePairRoute.handle(request, window, createToken = { token })
        assertEquals(200, response.status)
        assertTrue(response.body.contains(token))
        assertEquals(401, CompanionRemotePairRoute.handle(request, window, createToken = { token }).status)
        assertEquals(400, CompanionRemotePairRoute.handle(
            request = request.copy(method = "POST"),
            pairingWindow = window,
            createToken = { token },
        ).status)
    }

    @Test
    fun pairingAndRevocationMutationsDoNotOverlap() {
        val storedToken = AtomicReference<String?>(null)
        val pairingEntered = CountDownLatch(1)
        val commitPairing = CountDownLatch(1)
        val revokeStarted = CountDownLatch(1)
        val revokeEntered = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>(null)
        val pairThread = Thread {
            runCatching {
                CompanionRemoteControlRouteLock.withLock {
                    pairingEntered.countDown()
                    check(commitPairing.await(2, TimeUnit.SECONDS))
                    storedToken.set("new-token")
                }
            }.onFailure(failure::set)
        }
        val revokeThread = Thread {
            runCatching {
                check(pairingEntered.await(2, TimeUnit.SECONDS))
                revokeStarted.countDown()
                CompanionRemoteControlRouteLock.withLock {
                    revokeEntered.countDown()
                    storedToken.set(null)
                }
            }.onFailure(failure::set)
        }

        pairThread.start()
        revokeThread.start()
        assertTrue(revokeStarted.await(2, TimeUnit.SECONDS))
        assertFalse(revokeEntered.await(50, TimeUnit.MILLISECONDS))
        commitPairing.countDown()
        pairThread.join(2_000)
        revokeThread.join(2_000)

        assertFalse(pairThread.isAlive)
        assertFalse(revokeThread.isAlive)
        assertEquals(null, failure.get())
        assertEquals(null, storedToken.get())
    }

    @Test
    fun controlPeerMustBeLoopbackOrPrivateLan() {
        assertTrue(CompanionPeerPolicy.isLocalNetwork(InetAddress.getByName("127.0.0.1")))
        assertTrue(CompanionPeerPolicy.isLocalNetwork(InetAddress.getByName("192.168.1.20")))
        assertTrue(CompanionPeerPolicy.isLocalNetwork(InetAddress.getByName("10.0.0.20")))
        assertTrue(CompanionPeerPolicy.isLocalNetwork(InetAddress.getByName("172.20.1.4")))
        assertTrue(CompanionPeerPolicy.isLocalNetwork(InetAddress.getByName("fc00::1")))
        assertFalse(CompanionPeerPolicy.isLocalNetwork(InetAddress.getByName("8.8.8.8")))
        assertFalse(CompanionPeerPolicy.isLocalNetwork(InetAddress.getByName("2001:4860::1")))
    }

    @Test
    fun remoteHttpPortStaysNextToTheWindowsCompanionPort() {
        assertEquals(49_501, CompanionEndpointService.remoteControlPort(49_500))
        assertEquals(65_535, CompanionEndpointService.remoteControlPort(65_534))
        assertEquals(65_534, CompanionEndpointService.remoteControlPort(65_535))
    }

    private fun pairRequest(code: String): CompanionHttpRequest {
        val body = "{\"code\":\"$code\"}".toByteArray()
        return CompanionHttpRequest(
            method = "PUT", path = CompanionRemotePairRoute.PATH, authorization = null, body = body,
            bodyLength = body.size.toLong(),
        )
    }
}
