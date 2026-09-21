package com.gamebox.os.data

import com.gamebox.os.data.local.toDomain
import com.gamebox.os.data.local.toEntity
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.GameMetadataOverrides
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GameMetadataOverridesTest {
    private fun game(
        title: String = "Provider title",
        year: Int = 2001,
        genre: String = "Adventure",
        artwork: String? = "https://cdn.example.test/provider.jpg",
        description: String? = "Provider description",
    ) = Game(
        id = GameId("metadata-game"),
        title = title,
        platform = "PS2",
        year = year,
        genre = genre,
        sizeMb = 1,
        state = InstallState.NOT_INSTALLED,
        artworkUrl = artwork,
        description = description,
    )

    @Test fun normalizationTrimsValuesAndTreatsBlankAsProviderFallback() {
        val result = normalizeMetadataOverrides(
            GameMetadataOverrides(
                title = "  Corrected title  ",
                year = 2002,
                genre = "  Racing ",
                artworkUrl = " https://cdn.example.test/custom.jpg ",
                description = "  Corrected description ",
            )
        )
        assertEquals("Corrected title", result.title)
        assertEquals(2002, result.year)
        assertEquals("Racing", result.genre)
        assertEquals("https://cdn.example.test/custom.jpg", result.artworkUrl)
        assertEquals("Corrected description", result.description)
        assertNull(normalizeMetadataOverrides(GameMetadataOverrides(title = " ")).title)
    }

    @Test fun unsafeArtworkAndInvalidYearAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            normalizeMetadataOverrides(GameMetadataOverrides(artworkUrl = "http://example.test/cover.jpg"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            normalizeMetadataOverrides(GameMetadataOverrides(artworkUrl = "https://user:secret@example.test/cover.jpg"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            normalizeMetadataOverrides(GameMetadataOverrides(year = 1800))
        }
    }

    @Test fun entityKeepsProviderValuesSeparateFromEffectiveOverrides() {
        val source = game().copy(
            title = "My title",
            year = 2002,
            artworkUrl = "https://cdn.example.test/custom.jpg",
            metadataOverrides = GameMetadataOverrides(
                title = "My title",
                year = 2002,
                artworkUrl = "https://cdn.example.test/custom.jpg",
            ),
        )
        val entity = source.toEntity()
        assertEquals("Provider title", entity.title)
        assertEquals(2001, entity.year)
        assertEquals("https://cdn.example.test/provider.jpg", entity.artworkUrl)
        val restored = entity.toDomain()
        assertEquals("My title", restored.title)
        assertEquals(2002, restored.year)
        assertEquals("https://cdn.example.test/custom.jpg", restored.artworkUrl)
        assertEquals("Provider title", restored.providerTitle)
    }

    @Test fun providerRefreshChangesBaseButPreservesUserCorrections() {
        val local = game().copy(
            title = "My title",
            metadataOverrides = GameMetadataOverrides(title = "My title"),
        )
        val refreshed = game(
            title = "Updated provider title",
            year = 2004,
            description = "Updated provider description",
        )
        val merged = mergeCatalogPreservingLocalState(listOf(local), listOf(refreshed)).single()
        assertEquals("My title", merged.title)
        assertEquals("Updated provider title", merged.providerTitle)
        assertEquals(2004, merged.year)
        assertEquals("Updated provider description", merged.description)
        assertEquals("My title", merged.metadataOverrides.title)
    }
    @Test fun providerProvenanceSurvivesEntityRoundTripAndCatalogRefresh() {
        val local = game().copy(
            metadataProvider = "THE_GAMES_DB",
            metadataExternalId = "12345",
            metadataMatchedAtMillis = 42L,
            metadataOverrides = GameMetadataOverrides(title = "My title"),
            title = "My title",
        )
        val restored = local.toEntity().toDomain()
        assertEquals("THE_GAMES_DB", restored.metadataProvider)
        assertEquals("12345", restored.metadataExternalId)
        assertEquals(42L, restored.metadataMatchedAtMillis)

        val merged = mergeCatalogPreservingLocalState(
            listOf(restored),
            listOf(game(title = "Refreshed provider title")),
        ).single()
        assertEquals("THE_GAMES_DB", merged.metadataProvider)
        assertEquals("12345", merged.metadataExternalId)
        assertEquals("My title", merged.title)
        assertEquals("Refreshed provider title", merged.providerTitle)
    }
}
