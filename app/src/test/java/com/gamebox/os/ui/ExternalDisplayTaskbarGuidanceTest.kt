package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalDisplayTaskbarGuidanceTest {
    @Test
    fun honorGuidanceAppearsOnlyForPhysicalExternalDisplays() {
        assertEquals(
            "For GameBox and each emulator on HONOR Desktop Mode, open the app's Dock menu and choose Open in full screen mode to hide the Dock. This is an HONOR per-app setting; GameBox cannot change it or force third-party emulators into fullscreen. To hide the Dock globally, use Settings > Large screen features > Global Taskbar when available.",
            externalDisplayTaskbarGuidance("HONOR", listOf("Living room TV · 1920×1080")),
        )
        assertNull(externalDisplayTaskbarGuidance("HONOR", listOf("studio.screen.sharing:0")))
        assertNull(externalDisplayTaskbarGuidance("Google", listOf("Living room TV · 1920×1080")))
        assertNull(externalDisplayTaskbarGuidance("HONOR", emptyList()))
    }
}
