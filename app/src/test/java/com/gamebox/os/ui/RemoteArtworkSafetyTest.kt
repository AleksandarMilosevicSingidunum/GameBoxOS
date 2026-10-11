package com.gamebox.os.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteArtworkSafetyTest {
    @Test
    fun acceptsCredentialFreeHttpsArtworkUrls() {
        assertTrue(isSafeArtworkUrl("https://cdn.example/art/game.jpg"))
        assertTrue(isSafeArtworkUrl("https://cdn.example/art/game.jpg?size=large"))
    }

    @Test
    fun normalizesArtworkUrlBeforePassingItToTheLoader() {
        assertEquals(
            "https://cdn.example/covers/game.jpg",
            normalizeArtworkUrl("  https://cdn.example/art/../covers/game.jpg  "),
        )
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

    @Test
    fun resolvesRelativeHttpsRedirects() {
        assertEquals(
            "https://cdn.example/covers/game.jpg",
            nextArtworkRedirect("https://cdn.example/images/start", "/covers/game.jpg"),
        )
        assertEquals(
            "https://other.example/covers/game.jpg",
            nextArtworkRedirect("https://cdn.example/images/start", "https://other.example/covers/game.jpg"),
        )
    }

    @Test
    fun rejectsRedirectsThatDowngradeTransportOrContainCredentials() {
        assertNull(nextArtworkRedirect("https://cdn.example/image", "http://cdn.example/image"))
        assertNull(nextArtworkRedirect("https://cdn.example/image", "https://user:pass@cdn.example/image"))
    }
}
