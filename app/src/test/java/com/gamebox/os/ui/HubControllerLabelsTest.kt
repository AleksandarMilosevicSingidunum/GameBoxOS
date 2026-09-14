package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HubControllerLabelsTest {
    @Test
    fun pcHubOffersVisibilityAndDesktopActions() {
        assertEquals(
            HubControllerLabels("Installed only", "Desktop mode"),
            hubControllerLabels("PC Hub", showUnavailable = true),
        )
        assertEquals(
            HubControllerLabels("Show setup", "Desktop mode"),
            hubControllerLabels("PC Hub", showUnavailable = false),
        )
    }

    @Test
    fun mediaHubOffersVisibilityAndAudioActions() {
        assertEquals(
            HubControllerLabels("Installed only", "Audio"),
            hubControllerLabels("Media", showUnavailable = true),
        )
        assertEquals(
            HubControllerLabels("Show setup", "Audio"),
            hubControllerLabels("Media", showUnavailable = false),
        )
    }
}
