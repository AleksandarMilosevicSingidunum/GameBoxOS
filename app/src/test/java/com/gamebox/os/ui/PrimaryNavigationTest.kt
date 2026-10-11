package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PrimaryNavigationTest {
    @Test
    fun shoulderNavigationVisitsEveryVisibleTabIncludingPc() {
        val visited = generateSequence(Destination.HOME) { destinationAfterPrimaryTabMove(it, 1) }
            .take(primaryDestinations.size + 1)
            .toList()

        assertEquals(
            listOf(
                Destination.HOME,
                Destination.LIBRARY,
                Destination.STORE,
                Destination.DOWNLOADS,
                Destination.MEDIA,
                Destination.PC,
                Destination.SETTINGS,
                Destination.HOME,
            ),
            visited,
        )
    }

    @Test
    fun shoulderNavigationWrapsInBothDirections() {
        assertEquals(Destination.SETTINGS, destinationAfterPrimaryTabMove(Destination.HOME, -1))
        assertEquals(Destination.HOME, destinationAfterPrimaryTabMove(Destination.SETTINGS, 1))
    }

    @Test
    fun shoulderNavigationIncludesDownloadsInTheVisibleTabSequence() {
        assertEquals(Destination.MEDIA, destinationAfterPrimaryTabMove(Destination.DOWNLOADS, 1))
        assertEquals(Destination.STORE, destinationAfterPrimaryTabMove(Destination.DOWNLOADS, -1))
    }
}
