package com.gamebox.os.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalSourceLinksTest {
    @Test
    fun pcLinksUseAuthorizedStorefrontSearches() {
        val links = legalSourceLinks("Celeste Classic", "PC")

        assertEquals(listOf("Steam", "GOG"), links.map(LegalSourceLink::label))
        assertTrue(links.all { it.url.startsWith("https://") })
        assertTrue(links.first().url.contains("Celeste%20Classic%20PC"))
    }

    @Test
    fun homebrewLinksPointToSourceAndIndieReleaseSearches() {
        val links = legalSourceLinks("Galaxy Patrol", "Homebrew")

        assertEquals(listOf("itch.io", "GitHub"), links.map(LegalSourceLink::label))
        assertTrue(links.all { it.url.startsWith("https://") })
        assertTrue(links[1].url.contains("type=repositories"))
    }

    @Test
    fun unsupportedPlatformsRemainImportYourOwnCopy() {
        assertTrue(legalSourceLinks("Galaxy Patrol", "Dreamcast").isEmpty())
        assertTrue(legalSourceLinks("", "PC").isEmpty())
    }
}

