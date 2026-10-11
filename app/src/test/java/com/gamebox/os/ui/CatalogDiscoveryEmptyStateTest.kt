package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogDiscoveryEmptyStateTest {
    @Test
    fun activeFiltersExplainTheEmptyResult() {
        assertEquals(
            "No discovered games match these filters.",
            catalogDiscoveryEmptyMessage(
                favoritesOnly = false,
                query = "metroid",
                hasConfiguredJsonSource = false,
            ),
        )
    }

    @Test
    fun configuredJsonSourceRemainsAnOptionWithoutProviderKey() {
        assertEquals(
            "No catalog matches yet. Try a different title, or search one of your configured game sources below.",
            catalogDiscoveryEmptyMessage(
                favoritesOnly = false,
                query = "",
                hasConfiguredJsonSource = true,
            ),
        )
    }

    @Test
    fun noConfiguredProviderPointsUsersToTheAvailableSourceSearches() {
        assertEquals(
            "No catalog titles match this console yet. Try a different console or search a configured source below.",
            catalogDiscoveryEmptyMessage(
                favoritesOnly = false,
                query = "",
                hasConfiguredJsonSource = false,
            ),
        )
    }
}
