package com.gamebox.os.catalog

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogResponseFramingTest {
    @Test
    fun acceptsUnknownLengthWhenResponseStaysWithinBound() {
        val bytes = readBoundedCatalogResponse(
            input = ByteArrayInputStream("catalog".toByteArray()),
            declaredLength = -1L,
            maxResponseBytes = 32,
        )

        assertArrayEquals("catalog".toByteArray(), bytes)
    }

    @Test
    fun rejectsResponseShorterThanDeclaredContentLength() {
        val error = assertThrows(IOException::class.java) {
            readBoundedCatalogResponse(
                input = ByteArrayInputStream("short".toByteArray()),
                declaredLength = 12L,
                maxResponseBytes = 32,
            )
        }

        assertEquals("Catalog response ended with an unexpected length (5/12)", error.message)
    }

    @Test
    fun rejectsResponseLongerThanDeclaredContentLength() {
        assertThrows(IOException::class.java) {
            readBoundedCatalogResponse(
                input = ByteArrayInputStream("too-long".toByteArray()),
                declaredLength = 3L,
                maxResponseBytes = 32,
            )
        }
    }
}
