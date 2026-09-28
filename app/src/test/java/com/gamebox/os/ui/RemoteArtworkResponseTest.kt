package com.gamebox.os.ui

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteArtworkResponseTest {
    @Test
    fun acceptsUnknownLengthWithinBound() {
        val bytes = readBoundedArtworkResponse(
            ByteArrayInputStream(byteArrayOf(1, 2, 3)),
            declaredLength = -1L,
            maxBytes = 4,
        )

        assertArrayEquals(byteArrayOf(1, 2, 3), bytes)
    }

    @Test
    fun rejectsTruncatedDeclaredLength() {
        val error = assertThrows(IOException::class.java) {
            readBoundedArtworkResponse(
                ByteArrayInputStream(byteArrayOf(1, 2, 3)),
                declaredLength = 4L,
                maxBytes = 8,
            )
        }

        assertEquals("Artwork response ended with an unexpected length (3/4)", error.message)
    }

    @Test
    fun rejectsBodyBeyondMaximum() {
        assertThrows(IllegalArgumentException::class.java) {
            readBoundedArtworkResponse(
                ByteArrayInputStream(ByteArray(5)),
                declaredLength = -1L,
                maxBytes = 4,
            )
        }
    }
}
