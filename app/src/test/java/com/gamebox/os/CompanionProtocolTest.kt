package com.gamebox.os

import com.gamebox.os.companion.CompanionProtocol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionProtocolTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test fun verifiesVersionTwoNonceBoundRequest() {
        val nonce = "0123456789abcdef0123456789abcdef"
        val authorization = CompanionProtocol.createAuthorization(
            secret, "GET", "/v1/status", 1_700_000_000, nonce = nonce,
        )
        assertTrue(authorization.endsWith("$nonce:a2dec6ccb749ba2c91fdd59f963e92059156e9e481798ca56235458bd06f2b63"))
        assertTrue(CompanionProtocol.verifyAuthorization(secret, "GET", "/v1/status", authorization, 1_700_000_030))
    }

    @Test fun rejectsChangedMethodPathAndExpiredRequests() {
        val authorization = CompanionProtocol.createAuthorization(secret, "GET", "/v1/status", 1_700_000_000)
        assertFalse(CompanionProtocol.verifyAuthorization(secret, "POST", "/v1/status", authorization, 1_700_000_030))
        assertFalse(CompanionProtocol.verifyAuthorization(secret, "GET", "/v1/library", authorization, 1_700_000_030))
        assertFalse(CompanionProtocol.verifyAuthorization(secret, "GET", "/v1/status", authorization, 1_700_000_121))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsTraversalWhenSigning() {
        CompanionProtocol.createAuthorization(secret, "GET", "/v1/../status", 1_700_000_000)
    }
    @Test fun uploadAuthorizationBindsExactBodyChecksum() {
        val first = "a".repeat(64)
        val second = "b".repeat(64)
        val authorization = CompanionProtocol.createAuthorization(
            secret, "PUT", "/v1/saves/game-one", 1_700_000_000, first
        )
        assertTrue(
            CompanionProtocol.verifyAuthorization(
                secret, "PUT", "/v1/saves/game-one", authorization, 1_700_000_030,
                bodySha256 = first,
            )
        )
        assertFalse(
            CompanionProtocol.verifyAuthorization(
                secret, "PUT", "/v1/saves/game-one", authorization, 1_700_000_030,
                bodySha256 = second,
            )
        )
    }

}

