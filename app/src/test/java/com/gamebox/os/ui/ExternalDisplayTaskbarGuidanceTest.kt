package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalDisplayTaskbarGuidanceTest {
    @Test
    fun honorGuidanceAppearsOnlyForPhysicalExternalDisplays() {
        assertEquals(
            "GameBox requests fullscreen, but HONOR Desktop Mode may keep its Dock over the app. Open the app's Dock menu and choose Open in full screen mode; repeat for emulators as needed. On the tested MagicOS 10 build, Settings search found no Global Taskbar or Dock-hide switch, and the fullscreen choice did not persist after relaunch. GameBox cannot hide this separate HONOR overlay.",
            externalDisplayTaskbarGuidance("HONOR", listOf("Living room TV · 1920×1080")),
        )
        assertNull(externalDisplayTaskbarGuidance("HONOR", listOf("studio.screen.sharing:0")))
        assertNull(externalDisplayTaskbarGuidance("Google", listOf("Living room TV · 1920×1080")))
        assertNull(externalDisplayTaskbarGuidance("HONOR", emptyList()))
    }
}
