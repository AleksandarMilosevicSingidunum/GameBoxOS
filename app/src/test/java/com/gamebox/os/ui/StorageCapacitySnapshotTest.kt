package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageCapacitySnapshotTest {
    @Test
    fun usedFractionUsesAvailableAndroidCapacity() {
        assertEquals(0.25f, StorageCapacitySnapshot(availableBytes = 75L, totalBytes = 100L).usedFraction)
    }

    @Test
    fun usedFractionClampsToValidRangeAndHandlesUnknownTotal() {
        assertEquals(0f, StorageCapacitySnapshot(availableBytes = 150L, totalBytes = 100L).usedFraction)
        assertEquals(1f, StorageCapacitySnapshot(availableBytes = -1L, totalBytes = 100L).usedFraction)
        assertEquals(0f, StorageCapacitySnapshot(availableBytes = 0L, totalBytes = 0L).usedFraction)
    }
}
