package com.gamebox.os.settings

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
