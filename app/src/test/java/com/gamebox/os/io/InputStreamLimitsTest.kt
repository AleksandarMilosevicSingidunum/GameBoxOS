package com.gamebox.os.io

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class InputStreamLimitsTest {
    @Test
    fun readsShortStreamsAndStopsAtTheRequestedLimit() {
        assertArrayEquals(byteArrayOf(1, 2), ByteArrayInputStream(byteArrayOf(1, 2)).readAtMost(4))
        assertArrayEquals(byteArrayOf(1, 2), ByteArrayInputStream(byteArrayOf(1, 2, 3)).readAtMost(2))
    }

    @Test
    fun continuesWhenAnInputStreamTemporarilyReturnsZero() {
        val stream = object : InputStream() {
            private var firstRead = true
            private val source = ByteArrayInputStream(byteArrayOf(7, 8))

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (firstRead) {
                    firstRead = false
                    return 0
                }
                return source.read(buffer, offset, length)
            }

            override fun read(): Int = source.read()
        }

        assertArrayEquals(byteArrayOf(7, 8), stream.readAtMost(2))
    }
}
