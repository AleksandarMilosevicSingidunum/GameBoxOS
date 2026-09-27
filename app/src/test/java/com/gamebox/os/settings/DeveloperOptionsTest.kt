package com.gamebox.os.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import com.gamebox.os.domain.CatalogRefreshState

class DeveloperOptionsTest {
    @Test
    fun invalidStoredOptionsFailClosedToProductionDefaults() {
        assertEquals(DeveloperLayoutMode.AUTO, DeveloperLayoutMode.fromStored("UNKNOWN"))
        assertEquals(CatalogFailureSimulation.LIVE, CatalogFailureSimulation.fromStored("ERROR "))
        assertEquals(DeveloperLayoutMode.AUTO, DeveloperLayoutMode.fromStored(null))
        assertEquals(CatalogFailureSimulation.LIVE, CatalogFailureSimulation.fromStored(null))
    }

    @Test
    fun knownStoredOptionsRoundTrip() {
        DeveloperLayoutMode.entries.forEach {
            assertEquals(it, DeveloperLayoutMode.fromStored(it.name))
        }
        CatalogFailureSimulation.entries.forEach {
            assertEquals(it, CatalogFailureSimulation.fromStored(it.name))
        }
    }

    @Test
    fun catalogFailureSimulationIsIgnoredOutsideDebugMode() {
        assertEquals(
            CatalogRefreshState.SUCCESS,
            effectiveCatalogRefreshState(
                CatalogRefreshState.SUCCESS,
                CatalogFailureSimulation.ERROR,
                allowSimulation = false,
            ),
        )
        assertEquals(
            CatalogRefreshState.ERROR,
            effectiveCatalogRefreshState(
                CatalogRefreshState.SUCCESS,
                CatalogFailureSimulation.ERROR,
                allowSimulation = true,
            ),
        )
    }

    @Test
    fun layoutOverrideIsIgnoredOutsideDebugMode() {
        assertEquals(
            DeveloperLayoutMode.AUTO,
            effectiveDeveloperLayoutMode(DeveloperLayoutMode.WIDE, allowDeveloperOptions = false),
        )
        assertEquals(
            DeveloperLayoutMode.WIDE,
            effectiveDeveloperLayoutMode(DeveloperLayoutMode.WIDE, allowDeveloperOptions = true),
        )
    }
}
