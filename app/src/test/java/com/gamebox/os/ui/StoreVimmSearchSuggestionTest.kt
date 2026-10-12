package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreVimmSearchSuggestionTest {
    @Test
    fun rerunsOnlyTheCorrectionThatMatchesTheCommittedQuery() {
        assertEquals("Freedom", pendingVimmSearchForQuery("Freedom", "Freedom"))
        assertNull(pendingVimmSearchForQuery("Freedom", "Freedoom"))
        assertNull(pendingVimmSearchForQuery("Freedom", null))
    }
}
