package com.gamebox.os.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformEmulatorDefaultsTest {
    @Test
    fun encodingIsStableAndRoundTripsPlatformDefaults() {
        val defaults = mapOf("psp" to "org.example.emulator", "gba" to "com.example.gba")

        val encoded = encodePlatformDefaults(defaults)

        assertEquals("gba\tcom.example.gba\npsp\torg.example.emulator", encoded)
        assertEquals(defaults, decodePlatformDefaults(encoded))
    }

    @Test
    fun decoderDropsMalformedEntriesAndPlatformKeysNormalize() {
        assertEquals(
            mapOf("psp" to "org.example.emulator"),
            decodePlatformDefaults("psp\torg.example.emulator\ninvalid key\tbad\npsx\tbad/name"),
        )
        assertTrue(decodePlatformDefaults(null).isEmpty())
        assertEquals("gameboyadvance", normalizePlatformKey("Game Boy Advance"))
    }
}
