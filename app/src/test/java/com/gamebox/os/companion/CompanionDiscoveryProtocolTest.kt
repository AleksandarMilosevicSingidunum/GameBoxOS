package com.gamebox.os.companion

import org.junit.Assert.*
import org.junit.Test

class CompanionDiscoveryProtocolTest {
    private val nonce = "0123456789abcdef0123456789abcdef"

    @Test fun requestAndResponseRoundTripBoundedMetadata() {
        val request = CompanionDiscoveryProtocol.request(nonce)
        assertEquals(nonce, CompanionDiscoveryProtocol.parseRequest(request))
        val response = CompanionDiscoveryProtocol.response(nonce, 49_500, "Living Room GameBox")
        val parsed = requireNotNull(CompanionDiscoveryProtocol.parseResponse(response))
        assertEquals(nonce, parsed.nonce)
        assertEquals(49_500, parsed.port)
        assertEquals("Living Room GameBox", parsed.deviceName)
        assertTrue(response.size <= 512)
    }

    @Test fun malformedAndAmplificationInputsAreIgnored() {
        val invalid = listOf(
            byteArrayOf(),
            "GAMEBOX_DISCOVER_V1:../escape".toByteArray(),
            "GAMEBOX_DISCOVER_V2:$nonce".toByteArray(),
            ByteArray(129) { 'a'.code.toByte() },
        )
        invalid.forEach { assertNull(CompanionDiscoveryProtocol.parseRequest(it)) }
        assertNull(CompanionDiscoveryProtocol.parseResponse(
            "GAMEBOX_HERE_V1:$nonce:80:VGVzdA==".toByteArray(),
        ))
        assertTrue(runCatching {
            CompanionDiscoveryProtocol.response(nonce, 49_500, "x".repeat(193))
        }.isFailure)
    }
}
