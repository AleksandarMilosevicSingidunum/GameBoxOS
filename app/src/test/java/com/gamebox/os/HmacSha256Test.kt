package com.gamebox.os

import com.gamebox.os.catalog.HmacSha256
import org.junit.Assert.assertEquals
import org.junit.Test

class HmacSha256Test {
    @Test
    fun computesKnownVector() {
        val digest = HmacSha256.digest(
            "key".toByteArray(),
            "The quick brown fox jumps over the lazy dog",
        )

        assertEquals(
            "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
            HmacSha256.hex(digest),
        )
    }
}
