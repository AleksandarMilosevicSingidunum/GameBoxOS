package com.gamebox.os

import com.gamebox.os.settings.normalizeMoonlightHost
import com.gamebox.os.settings.requireMoonlightPort
import org.junit.Assert.assertEquals
import org.junit.Test

class MoonlightHostSettingsTest {
    @Test fun normalizesDnsIpv4AndBracketedIpv6Hosts() {
        assertEquals("gaming-pc.local", normalizeMoonlightHost("  gaming-pc.local  "))
        assertEquals("192.168.1.20", normalizeMoonlightHost("192.168.1.20"))
        assertEquals("2001:db8::20", normalizeMoonlightHost("[2001:db8::20]"))
        assertEquals(47_984, requireMoonlightPort(47_984))
    }

    @Test fun rejectsSchemesPathsCredentialsWhitespaceAndBrokenBrackets() {
        listOf(
            "", "https://gaming-pc", "gaming-pc/path", "user@gaming-pc",
            "gaming pc", "[2001:db8::20", "2001:db8::20]",
        ).forEach { invalid ->
            expectInvalid { normalizeMoonlightHost(invalid) }
        }
        listOf(0, -1, 65_536).forEach { invalid ->
            expectInvalid { requireMoonlightPort(invalid) }
        }
    }

    private fun expectInvalid(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected invalid PC host configuration")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
