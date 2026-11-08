package com.gamebox.os.ui

import com.gamebox.os.catalog.LibretroThumbnails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class VimmThumbnailLookupTest {
    @Test
    fun thumbnailFailureDoesNotFailVimmDetails() = runBlocking {
        assertNull(lookupVimmThumbnailsOrNull { error("thumbnail request failed") })
    }

    @Test
    fun thumbnailLookupPreservesSuccessfulArtwork() = runBlocking {
        val thumbnails = LibretroThumbnails("box.png", "shot.png")
        assertEquals(thumbnails, lookupVimmThumbnailsOrNull { thumbnails })
    }

    @Test
    fun thumbnailLookupPropagatesCancellation() = runBlocking {
        val cancelled = CancellationException("cancelled")
        try {
            lookupVimmThumbnailsOrNull { throw cancelled }
            throw AssertionError("cancellation was swallowed")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }
}
