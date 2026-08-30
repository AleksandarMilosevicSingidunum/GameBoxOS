package com.gamebox.os

import com.gamebox.os.launch.EmulatorIntentPolicy
import com.gamebox.os.launch.EmulatorIntentStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorIntentPolicyTest {
    private val uri = "content://com.gamebox.os.files/installed/game.iso"

    @Test
    fun rejectsMalformedOrCredentialBearingContentUris() {
        assertThrows(IllegalArgumentException::class.java) {
            EmulatorIntentPolicy.plan(EmulatorIntentPolicy.PPSSPP_PACKAGE, "content://", "Balanced")
        }
        assertThrows(IllegalArgumentException::class.java) {
            EmulatorIntentPolicy.plan(EmulatorIntentPolicy.PPSSPP_PACKAGE, "content://user:pass@files/game.iso", "Balanced")
        }
        assertThrows(IllegalArgumentException::class.java) {
            EmulatorIntentPolicy.plan("", uri, "Balanced")
        }
    }

    @Test
    fun ppssppAppliesSupportedGraphicsProfilesToOfficialArgsExtra() {
        val performance = EmulatorIntentPolicy.plan(EmulatorIntentPolicy.PPSSPP_PACKAGE, uri, "Performance")
        val compatibility = EmulatorIntentPolicy.plan(EmulatorIntentPolicy.PPSSPP_PACKAGE, uri, "Compatibility")

        assertEquals(EmulatorIntentStyle.LAUNCHER_EXTRAS, performance.style)
        assertEquals("--graphics=vulkan \"$uri\"", performance.stringExtras[EmulatorIntentPolicy.PPSSPP_ARGS])
        assertEquals("--graphics=gles \"$uri\"", compatibility.stringExtras[EmulatorIntentPolicy.PPSSPP_ARGS])
        assertTrue(performance.graphicsProfileApplied)
    }

    @Test
    fun balancedPpssppLaunchPreservesEmulatorDefaultBackend() {
        val plan = EmulatorIntentPolicy.plan(EmulatorIntentPolicy.PPSSPP_PACKAGE, uri, "Balanced")

        assertEquals("\"$uri\"", plan.stringExtras[EmulatorIntentPolicy.PPSSPP_ARGS])
        assertFalse(plan.graphicsProfileApplied)
    }

    @Test
    fun dolphinUsesOfficialAutoStartFilesContract() {
        val plan = EmulatorIntentPolicy.plan(EmulatorIntentPolicy.DOLPHIN_PACKAGE, uri, "Performance")

        assertEquals(EmulatorIntentStyle.LAUNCHER_EXTRAS, plan.style)
        assertEquals(listOf(uri), plan.stringArrayExtras[EmulatorIntentPolicy.DOLPHIN_AUTO_START_FILES])
        assertFalse(plan.graphicsProfileApplied)
    }

    @Test
    fun retroArchUsesLauncherRomExtraForScopedContent() {
        val plan = EmulatorIntentPolicy.plan("com.retroarch.aarch64", uri, "Balanced")

        assertEquals(EmulatorIntentStyle.LAUNCHER_EXTRAS, plan.style)
        assertEquals(uri, plan.stringExtras[EmulatorIntentPolicy.RETROARCH_ROM])
    }

    @Test
    fun adaptersWithoutDocumentedExtrasKeepActionViewFallback() {
        val plan = EmulatorIntentPolicy.plan("com.retroarch.aarch64", uri, "Performance")

        assertEquals(EmulatorIntentStyle.ACTION_VIEW, plan.style)
        assertTrue(plan.stringExtras.isEmpty())
    }
}
