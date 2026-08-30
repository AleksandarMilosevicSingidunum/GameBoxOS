package com.gamebox.os

import com.gamebox.os.launch.EmulatorReadinessPolicy
import com.gamebox.os.launch.EmulatorReadinessState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorReadinessPolicyTest {
    private val options = listOf("org.ppsspp.ppsspp")
    private val name: (String) -> String = { "PPSSPP" }

    @Test
    fun automaticSelectionIsReadyWhenApprovedDefaultIsInstalled() {
        val readiness = EmulatorReadinessPolicy.evaluate(
            approvedOptions = options,
            selectedPackage = null,
            installedPackages = options.toSet(),
            displayName = name,
        )

        assertEquals(EmulatorReadinessState.READY, readiness.state)
        assertEquals("org.ppsspp.ppsspp", readiness.selectedPackage)
        assertTrue(readiness.message.contains("ready"))
    }

    @Test
    fun automaticSelectionExplainsMissingRequiredPackage() {
        val readiness = EmulatorReadinessPolicy.evaluate(
            approvedOptions = options,
            selectedPackage = null,
            installedPackages = emptySet(),
            displayName = name,
        )

        assertEquals(EmulatorReadinessState.NONE_INSTALLED, readiness.state)
        assertTrue(readiness.message.contains("Install PPSSPP"))
    }

    @Test
    fun missingExplicitSelectionOffersInstalledAlternative() {
        val readiness = EmulatorReadinessPolicy.evaluate(
            approvedOptions = listOf("emulator.preferred", "emulator.alternative"),
            selectedPackage = "emulator.preferred",
            installedPackages = setOf("emulator.alternative"),
            displayName = { it.substringAfterLast('.') },
        )

        assertEquals(EmulatorReadinessState.MISSING_SELECTED, readiness.state)
        assertEquals(listOf("emulator.alternative"), readiness.installedOptions)
        assertTrue(readiness.message.contains("Choose an installed emulator"))
    }

    @Test
    fun unapprovedSelectionFallsBackToApprovedDefault() {
        val readiness = EmulatorReadinessPolicy.evaluate(
            approvedOptions = options,
            selectedPackage = "untrusted.package",
            installedPackages = options.toSet(),
            displayName = name,
        )

        assertEquals(EmulatorReadinessState.READY, readiness.state)
        assertEquals("org.ppsspp.ppsspp", readiness.selectedPackage)
    }

    @Test
    fun unsupportedPlatformHasExplicitState() {
        val readiness = EmulatorReadinessPolicy.evaluate(
            approvedOptions = emptyList(),
            selectedPackage = null,
            installedPackages = emptySet(),
            displayName = name,
        )

        assertEquals(EmulatorReadinessState.UNSUPPORTED, readiness.state)
        assertTrue(readiness.message.contains("No approved emulator"))
    }
}
