package com.gamebox.os.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteArtworkSafetyTest {
    @Test
    fun acceptsOnlyCredentialFreeHttpsArtworkUrls() {
        assertTrue(isSafeArtworkUrl("https://cdn.example/art/game.jpg"))
        assertFalse(isSafeArtworkUrl("http://cdn.example/art/game.jpg"))
        assertFalse(isSafeArtworkUrl("https://user:pass@cdn.example/art/game.jpg"))
        assertFalse(isSafeArtworkUrl("https://cdn.example/art/game.jpg#fragment"))
        assertFalse(isSafeArtworkUrl("https:///missing-host.jpg"))
    }
}
