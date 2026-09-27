package com.gamebox.os.ui

import com.gamebox.os.domain.CatalogRefreshState
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogRefreshSemanticsTest {
    @Test
    fun refreshingStateAnnouncesBusyActionAndOtherStatesAnnounceRetryAction() {
        assertEquals(
            "Refreshing catalog",
            catalogRefreshButtonDescription(CatalogRefreshState.REFRESHING),
        )
        CatalogRefreshState.entries
            .filterNot { it == CatalogRefreshState.REFRESHING }
            .forEach { state ->
                assertEquals(
                    "Refresh authorized catalog",
                    catalogRefreshButtonDescription(state),
                )
            }
    }
}
