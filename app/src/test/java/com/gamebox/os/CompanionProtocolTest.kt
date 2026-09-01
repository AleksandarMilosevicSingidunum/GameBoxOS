package com.gamebox.os

import com.gamebox.os.companion.CompanionProtocol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionProtocolTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test fun verifiesMatchingVersionOneRequest() {
        val authorization = CompanionProtocol.createAuthorization(secret, "GET", "/v1/status", 1_700_000_000)
        assertTrue(authorization.endsWith(":69acae0c7272bf32ddb759bb84ae0476177867864848ea6113589efe963afff6"))
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
}

