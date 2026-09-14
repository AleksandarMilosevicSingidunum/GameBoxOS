package com.gamebox.os.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutVisibilityTest {
    @Test
    fun visibilityCanBeHiddenAndRestoredIndependently() {
        val hidden = updateShortcutVisibility(emptySet(), "com.example.media", visible = false)
        assertTrue("com.example.media" in hidden)

        val restored = updateShortcutVisibility(hidden, "com.example.media", visible = true)
        assertFalse("com.example.media" in restored)
    }

    @Test
    fun persistedPackagesAreSortedDeduplicatedAndCorruptionSafe() {
        val decoded = decodeHiddenShortcutPackages(
            "org.videolan.vlc\ncom.spotify.music\norg.videolan.vlc\nhttps://unsafe"
        )

        assertEquals(setOf("org.videolan.vlc", "com.spotify.music"), decoded)
        assertEquals("com.spotify.music\norg.videolan.vlc", encodeHiddenShortcutPackages(decoded))
    }

    @Test(expected = IllegalArgumentException::class)
    fun mutationRejectsUnsafePackageName() {
        updateShortcutVisibility(emptySet(), "package/name", visible = false)
    }
}
