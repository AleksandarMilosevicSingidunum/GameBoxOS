package com.gamebox.os.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageAvailabilityTest {
    @Test
    fun usesAndroidAllocatableCapacityWhenAvailable() {
        val bytes = resolveAvailableStorageBytes(
            allocatable = { 4096L },
            fallback = { error("fallback must not be queried") },
        )

        assertEquals(4096L, bytes)
    }

    @Test
    fun fallsBackToFilesystemCapacityWhenAndroidQueryFails() {
        val bytes = resolveAvailableStorageBytes(
            allocatable = { throw java.io.IOException("storage service unavailable") },
            fallback = { 2048L },
        )

        assertEquals(2048L, bytes)
    }

    @Test
    fun clampsInvalidNegativeCapacityToZero() {
        val bytes = resolveAvailableStorageBytes(
            allocatable = { -1L },
            fallback = { error("negative successful result must be clamped") },
        )

        assertEquals(0L, bytes)
    }
}
