package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.GameMetadataOverrides
import com.gamebox.os.domain.InstallState
import org.junit.Assert.*
import org.junit.Test

class LibraryDiscoveryDetailsTest {
    private val library = Game(GameId("game"), "My title", "PSP", 2006, "Racing", 10,
        InstallState.INSTALLED, artworkUrl = "cover", description = "My description")
    private val cached = DiscoveryGame(
        GameId("game"), "Provider title", "psp", "EU", "2006-01-01", "Provider description",
        "2", 9.0, "cover", "background", null, listOf("shot", "shot", "second"), false,
    )

    @Test fun installedDetailsRetainGalleryWithoutReplacingUserMetadata() {
        val detail = GameDetailPresentation.from(library, cached)
        assertEquals("My title", detail.title)
        assertEquals("My description", detail.description)
        assertEquals("background", detail.heroUrl)
        assertEquals(listOf("shot", "second"), detail.screenshots)
        assertEquals("9", detail.ratingLabel)
        assertTrue(detail.installed)
        assertEquals(GameDetailSource.LIBRARY, detail.source)
    }

    @Test fun unrelatedOrRematchedIdentityCannotSupplyOldGallery() {
        assertTrue(GameDetailPresentation.from(library, cached.copy(id = GameId("other"))).screenshots.isEmpty())
        assertTrue(GameDetailPresentation.from(library.copy(metadataExternalId = "different"), cached).screenshots.isEmpty())
        assertEquals("cover", GameDetailPresentation.from(library, null).heroUrl)
    }

    @Test fun correctedArtworkWinsAndMissingBackgroundUsesScreenshot() {
        val corrected = library.copy(metadataOverrides = GameMetadataOverrides(artworkUrl = "custom"))
        assertEquals("custom", GameDetailPresentation.from(corrected, cached).heroUrl)
        assertEquals("shot", GameDetailPresentation.from(library, cached.copy(backgroundUrl = null)).heroUrl)
    }
}
