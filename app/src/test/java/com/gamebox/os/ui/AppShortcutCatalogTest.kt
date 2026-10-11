package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShortcutCatalogTest {
    @Test
    fun pcHubSpecialPresentationUsesExplicitRolesNotDisplayNames() {
        val featured = pcShortcuts.single { it.role == AppShortcutRole.FEATURED_PC_STREAMING }
        val fileManager = pcShortcuts.single { it.role == AppShortcutRole.FILE_MANAGER }

        assertEquals("com.limelight", featured.packageName)
        assertEquals("com.google.android.documentsui", fileManager.packageName)
        assertTrue(mediaShortcuts.all { it.role == AppShortcutRole.STANDARD })
    }
}
