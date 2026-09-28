package com.gamebox.os.source

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CancellableHttpsTextTransportTest {
    @Test
    fun acceptsUnknownLengthTextWithinBound() {
        assertEquals(
            "catalog",
            readBoundedHttpsTextResponse(
                ByteArrayInputStream("catalog".toByteArray()), -1L, 32, "Discovery",
            ),
        )
    }

    @Test
    fun rejectsTruncatedDeclaredTextLength() {
        val error = assertThrows(IOException::class.java) {
            readBoundedHttpsTextResponse(
                ByteArrayInputStream("short".toByteArray()), 12L, 32, "Discovery",
            )
        }

        assertEquals("Discovery response ended with an unexpected length (5/12)", error.message)
    }

    @Test
    fun rejectsTextBeyondMaximum() {
        assertThrows(IllegalArgumentException::class.java) {
            readBoundedHttpsTextResponse(
                ByteArrayInputStream(ByteArray(5)), -1L, 4, "Discovery",
            )
        }
    }
}
