package com.gamebox.os

import com.gamebox.os.ui.SettingsNavigationPolicy
import com.gamebox.os.ui.SettingsSection
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsNavigationPolicyTest {
    private val anchors = mapOf(
        SettingsSection.STORAGE to 180,
        SettingsSection.CONTROLLERS to 620,
        SettingsSection.DOWNLOADS to 860,
        SettingsSection.SAVES_CLOUD to 1_500,
        SettingsSection.SYSTEM to 2_600,
    )

    @Test fun `defaults to first available section above first anchor`() {
        assertEquals(SettingsSection.STORAGE, SettingsNavigationPolicy.selectedSection(0, anchors))
    }

    @Test fun `selects most recent anchor as content scrolls`() {
        assertEquals(SettingsSection.CONTROLLERS, SettingsNavigationPolicy.selectedSection(610, anchors))
        assertEquals(SettingsSection.SAVES_CLOUD, SettingsNavigationPolicy.selectedSection(1_500, anchors))
        assertEquals(SettingsSection.SYSTEM, SettingsNavigationPolicy.selectedSection(4_000, anchors))
    }

    @Test fun `lead distance activates an approaching heading`() {
        assertEquals(SettingsSection.DOWNLOADS, SettingsNavigationPolicy.selectedSection(820, anchors, 40))
    }
}

