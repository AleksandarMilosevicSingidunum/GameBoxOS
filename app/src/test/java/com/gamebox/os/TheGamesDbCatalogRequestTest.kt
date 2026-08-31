package com.gamebox.os

import com.gamebox.os.catalog.TheGamesDbCatalogRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TheGamesDbCatalogRequestTest {
    @Test
    fun buildsCredentialSafeHttpsPlatformRequest() {
        val uri = TheGamesDbCatalogRequest.gamesByPlatform("secret key", "11", 2)

        assertEquals("https", uri.scheme)
        assertEquals("api.thegamesdb.net", uri.host)
        assertTrue(uri.rawQuery.contains("apikey=secret+key"))
        assertTrue(uri.rawQuery.contains("page=2"))
        assertTrue(uri.rawQuery.contains("include=boxart"))
    }

    @Test
    fun rejectsUnboundedOrNonNumericInputs() {
        assertThrows(IllegalArgumentException::class.java) {
            TheGamesDbCatalogRequest.gamesByPlatform("key", "../11", 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TheGamesDbCatalogRequest.gamesByPlatform("key", "11", 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TheGamesDbCatalogRequest.platforms(" ")
        }
    }
}
