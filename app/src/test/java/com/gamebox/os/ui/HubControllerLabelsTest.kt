package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HubControllerLabelsTest {
    @Test
    fun pcHubOffersVisibilityAndDesktopActions() {
        assertEquals(
            HubControllerLabels("Installed only", "Desktop mode"),
            hubControllerLabels(AppHubKind.PC, showUnavailable = true),
        )
        assertEquals(
            HubControllerLabels("Show setup", "Desktop mode"),
            hubControllerLabels(AppHubKind.PC, showUnavailable = false),
        )
    }

    @Test
    fun mediaHubOffersVisibilityAndAudioActions() {
        assertEquals(
            HubControllerLabels("Installed only", "Audio"),
            hubControllerLabels(AppHubKind.MEDIA, showUnavailable = true),
        )
        assertEquals(
            HubControllerLabels("Show setup", "Audio"),
            hubControllerLabels(AppHubKind.MEDIA, showUnavailable = false),
        )
    }
}
