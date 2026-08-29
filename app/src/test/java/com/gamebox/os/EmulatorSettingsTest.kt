package com.gamebox.os

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmulatorSettingsTest {
    private val registry = EmulatorCapabilityRegistry()

    @Test
    fun platformOptionsProvideApprovedDefaults() {
        val psp = Game(GameId("psp-demo"), "PSP Demo", "PSP", 2020, "Action", 1, InstallState.INSTALLED,
            expectedSha256 = "abc")
        assertEquals("org.ppsspp.ppsspp", registry.forGame(psp)?.packageName)
    }

    @Test
    fun unknownOverrideFallsBackToPlatformDefault() {
        val retro = Game(GameId("retro-demo"), "Retro Demo", "Retro", 2020, "Action", 1, InstallState.INSTALLED,
            emulatorPackage = "com.example.untrusted", expectedSha256 = "abc")
        assertEquals("com.retroarch.aarch64", registry.forGame(retro)?.packageName)
    }

    @Test
    fun unsupportedGameWithoutDiagnosticCapabilityIsRejected() {
        val android = Game(GameId("android-demo"), "Android Demo", "Android", 2020, "Action", 1, InstallState.INSTALLED,
            expectedSha256 = "abc")
        assertNull(registry.forGame(android))
    }
}
