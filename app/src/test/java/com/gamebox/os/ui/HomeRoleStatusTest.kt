package com.gamebox.os.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRoleStatusTest {
    @Test
    fun matchesOnlyTheGameBoxPackage() {
        assertTrue(defaultHomeMatchesApp("com.gamebox.os", "com.gamebox.os"))
        assertFalse(defaultHomeMatchesApp("com.android.launcher", "com.gamebox.os"))
        assertFalse(defaultHomeMatchesApp(null, "com.gamebox.os"))
        assertFalse(defaultHomeMatchesApp("", "com.gamebox.os"))
    }
}
