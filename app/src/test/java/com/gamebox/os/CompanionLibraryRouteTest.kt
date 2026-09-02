package com.gamebox.os

import com.gamebox.os.companion.CompanionLibraryItem
import com.gamebox.os.companion.CompanionLibraryRoute
import com.gamebox.os.companion.CompanionProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionLibraryRouteTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test fun libraryRouteReturnsOnlyPairedReadOnlyFields() {
        val authorization = CompanionProtocol.createAuthorization(secret, "GET", "/v1/library", 1_700_000_000)
        val response = CompanionLibraryRoute.handle(
            "GET", "/v1/library", authorization, secret,
            listOf(CompanionLibraryItem("nes-1", "Galaxy \"Patrol\"", "NES", "INSTALLED", true, 12, true)),
            1_700_000_001,
        )

        assertEquals(200, response.status)
        assertTrue(response.body.contains("\"title\":\"Galaxy \\\"Patrol\\\"\""))
        assertTrue(response.body.contains("\"installState\":\"INSTALLED\""))
        assertTrue(!response.body.contains("sourceUrl"))
    }

    @Test fun libraryRouteRejectsMismatchedAuthentication() {
        assertEquals(
            401,
            CompanionLibraryRoute.handle("GET", "/v1/library", null, secret, emptyList(), 1_700_000_000).status,
        )
        assertEquals(
            404,
            CompanionLibraryRoute.handle("POST", "/v1/library", null, secret, emptyList(), 1_700_000_000).status,
        )
    }
}
