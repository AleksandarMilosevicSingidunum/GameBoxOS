package com.gamebox.os.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteArtworkSafetyTest {
    @Test
    fun acceptsCredentialFreeHttpsArtworkUrls() {
        assertTrue(isSafeArtworkUrl("https://cdn.example/art/game.jpg"))
        assertTrue(isSafeArtworkUrl("https://cdn.example/art/game.jpg?size=large"))
    }

    @Test
    fun rejectsUnsafeOrMalformedArtworkUrls() {
        assertFalse(isSafeArtworkUrl(null))
        assertFalse(isSafeArtworkUrl(""))
        assertFalse(isSafeArtworkUrl("http://cdn.example/art/game.jpg"))
        assertFalse(isSafeArtworkUrl("https://user:pass@cdn.example/art/game.jpg"))
        assertFalse(isSafeArtworkUrl("https://cdn.example/art/game.jpg#fragment"))
        assertFalse(isSafeArtworkUrl("https:///missing-host.jpg"))
        assertFalse(isSafeArtworkUrl("javascript:alert(1)"))
    }
}
