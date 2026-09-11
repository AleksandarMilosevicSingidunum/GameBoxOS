package com.gamebox.os.ui

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
}
