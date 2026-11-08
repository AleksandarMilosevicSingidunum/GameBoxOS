package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RememberedGameFiltersTest {
    @Test
    fun clearRemovesEveryActiveFilter() {
        val filters = RememberedGameFilters("mario", "NES", "Action", "US", "English", true)

        filters.clear()

        assertEquals("", filters.query)
        assertNull(filters.platform)
        assertNull(filters.genre)
        assertNull(filters.region)
        assertNull(filters.language)
        assertFalse(filters.favoritesOnly)
    }
}
