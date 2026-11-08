package com.gamebox.remote

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteHostPolicyTest {
    @Test
    fun acceptsPrivateLanAndLoopbackAddresses() {
        listOf("127.0.0.1", "192.168.1.8", "10.0.0.4", "172.31.0.2", "fc00::1", "fe80::1")
            .forEach { assertTrue(isAllowedRemoteAddress(InetAddress.getByName(it))) }
    }

    @Test
    fun rejectsPublicAndMixedDnsAnswersBeforeSendingPlainHttpCredentials() {
        val privateAddress = InetAddress.getByName("192.168.1.8")

        assertThrows(IllegalArgumentException::class.java) {
            privateRemoteAddress("8.8.8.8", listOf(InetAddress.getByName("8.8.8.8")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            privateRemoteAddress("gamebox.local", listOf(privateAddress, InetAddress.getByName("8.8.8.8")))
        }
        assertEquals(privateAddress, privateRemoteAddress("gamebox.local", listOf(privateAddress)))
    }
}
