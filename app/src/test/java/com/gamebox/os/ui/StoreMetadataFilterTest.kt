package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreMetadataFilterTest {
    @Test fun allFiltersAcceptMissingMetadata() {
        assertTrue(matchesStoreMetadataFilters(null, null, null, null, null, null))
    }

    @Test fun filtersAreCaseInsensitiveAndRequireKnownMatchingValues() {
        assertTrue(matchesStoreMetadataFilters("Racing", "US", "English", "racing", "us", "english"))
        assertFalse(matchesStoreMetadataFilters("Racing", null, "English", "Racing", "US", "English"))
        assertFalse(matchesStoreMetadataFilters("RPG", "EU", "French", "Racing", null, null))
    }

    @Test fun filterCyclingDeduplicatesAndAlwaysReturnsToAll() {
        val values = listOf("US", "", "EU", "us")
        assertEquals(listOf("EU", "US"), metadataFilterOptions(values))
        assertEquals("EU", nextMetadataFilterValue(null, values))
        assertEquals("US", nextMetadataFilterValue("eu", values))
        assertNull(nextMetadataFilterValue("US", values))
        assertNull(nextMetadataFilterValue("Removed", values))
        assertNull(nextMetadataFilterValue("US", emptyList()))
    }

    @Test fun selectionsSurviveNavigationAndSavedStateWithoutCrossingDestinations() {
        val state = GameBoxUiState.create()
        state.rememberScreenValue("library.region", "EU")
        state.rememberScreenValue("library.language", "French")
        state.rememberScreenValue("store.region", "US")
        state.rememberScreenValue("store.genre", "Racing")
        state.openDestination("STORE")
        state.openGame("example")
        val restored = GameBoxUiState.decode(state.encode())
        restored.clearSelection()
        assertEquals("US", restored.screenValue("store.region"))
        assertEquals("Racing", restored.screenValue("store.genre"))
        restored.openDestination("LIBRARY")
        assertEquals("EU", restored.screenValue("library.region"))
        assertEquals("French", restored.screenValue("library.language"))
    }
}
