package com.gamebox.os

import com.gamebox.os.launch.EmulatorPackageResolver
import com.gamebox.os.launch.EmulatorReadinessPolicy
import com.gamebox.os.launch.EmulatorReadinessState
import org.junit.Assert.assertEquals
import org.junit.Test

class EmulatorReadinessPolicyTest {
    @Test fun installedSavedChoiceWins() {
        assertEquals("a", EmulatorPackageResolver.resolve(listOf("a", "b"), "a", setOf("a", "b")))
    }

    @Test fun unavailableSavedChoiceUsesInstalledApprovedAlternative() {
        assertEquals("b", EmulatorPackageResolver.resolve(listOf("a", "b"), "a", setOf("b")))
        val readiness = EmulatorReadinessPolicy.evaluate(listOf("a", "b"), "a", setOf("b")) { it }
        assertEquals(EmulatorReadinessState.READY, readiness.state)
        assertEquals("b", readiness.selectedPackage)
    }

    @Test fun unapprovedChoiceNeverLaunches() {
        assertEquals(null, EmulatorPackageResolver.resolve(listOf("a"), "evil", emptySet()))
    }

    @Test fun missingApprovedPackagesIsExplicit() {
        val readiness = EmulatorReadinessPolicy.evaluate(listOf("a"), null, emptySet()) { it }
        assertEquals(EmulatorReadinessState.NONE_INSTALLED, readiness.state)
        assertEquals("a", readiness.selectedPackage)
    }

    @Test fun unsupportedPlatformHasExplicitState() {
        val readiness = EmulatorReadinessPolicy.evaluate(emptyList(), null, emptySet()) { it }
        assertEquals(EmulatorReadinessState.UNSUPPORTED, readiness.state)
    }
}
