package com.gamebox.os

import com.gamebox.os.companion.CompanionProtocol
import com.gamebox.os.companion.CompanionStatusRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionStatusRouteTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test fun statusRouteRequiresMatchingAuthorization() {
        val authorization = CompanionProtocol.createAuthorization(secret, "GET", "/v1/status", 1_700_000_000)

        val response = CompanionStatusRoute.handle(
            "GET", "/v1/status", authorization, secret, "GameBox", 1_700_000_030,
        )

        assertEquals(200, response.status)
        assertTrue(response.body.contains("\"protocolVersion\":1"))
    }

    @Test fun statusRouteRejectsUnauthenticatedAndUnknownRequests() {
        assertEquals(
            401,
            CompanionStatusRoute.handle("GET", "/v1/status", null, secret, "GameBox", 1_700_000_000).status,
        )
        assertEquals(
            404,
            CompanionStatusRoute.handle("POST", "/v1/status", null, secret, "GameBox", 1_700_000_000).status,
        )
    }
}
