package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutVisibilityTest {
    @Test
    fun unavailableShortcutsRemainVisibleWhenGuidanceIsEnabled() {
        assertEquals(
            listOf("media.one", "media.two"),
            visibleShortcutPackageNames(
                listOf("media.one", "media.two"),
                installedPackageNames = setOf("media.one"),
                showUnavailable = true
            )
        )
    }

    @Test
    fun unavailableShortcutsAreHiddenWhenPreferenceIsDisabled() {
        assertEquals(
            listOf("media.one"),
            visibleShortcutPackageNames(
                listOf("media.one", "media.two"),
                installedPackageNames = setOf("media.one"),
                showUnavailable = false
            )
        )
    }

    @Test
    fun policyDropsBlankAndDuplicateConfigurationEntries() {
        assertEquals(
            listOf("media.one", "media.two"),
            visibleShortcutPackageNames(
                listOf("", "media.one", "media.one", "media.two"),
                installedPackageNames = emptySet(),
                showUnavailable = true
            )
        )
    }
}
