package com.gamebox.os

import com.gamebox.os.companion.CompanionConfiguration
import com.gamebox.os.companion.CompanionConfigurationRoute
import com.gamebox.os.companion.CompanionConfigurationStore
import com.gamebox.os.companion.CompanionHttpRequest
import com.gamebox.os.companion.CompanionProtocol
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

class CompanionConfigurationRouteTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val now = 1_800_000_000L

    @Test fun authenticatedGetReturnsCurrentConfiguration() = runBlocking {
        val expected = CompanionConfiguration(true, false, true, false)
        val store = FakeStore(expected)
        val request = request("GET")
        val response = CompanionConfigurationRoute.handle(request, secret, store, now)

        assertEquals(200, response.status)
        assertEquals(
            """{"protocolVersion":1,"reducedMotion":true,"showUnavailableGames":false,"showUnavailableShortcuts":true,"downloadsUnmeteredOnly":false}""",
            response.body,
        )
        assertEquals(expected, store.value)
    }

    @Test fun authenticatedPutAtomicallyStoresSignedFlags() = runBlocking {
        val flags = "1010"
        val hash = flags.sha256()
        val expected = CompanionConfiguration(true, false, true, false)
        val store = FakeStore(CompanionConfiguration(false, false, false, false))
        val request = request("PUT", flags, hash)
        val response = CompanionConfigurationRoute.handle(request, secret, store, now)

        assertEquals(200, response.status)
        assertEquals(expected, store.value)
        assertEquals(1, store.writeCount)
    }

    @Test fun changedFlagsWithoutMatchingSignedHashAreRejected() = runBlocking {
        val signedFlags = "1010"
        val hash = signedFlags.sha256()
        val store = FakeStore(CompanionConfiguration(false, false, false, false))
        val response = CompanionConfigurationRoute.handle(
            request("PUT", "1111", hash), secret, store, now,
        )

        assertEquals(400, response.status)
        assertEquals(0, store.writeCount)
    }

    @Test fun invalidFlagsAndBodiesAreRejected() = runBlocking {
        val store = FakeStore(CompanionConfiguration(false, false, false, false))
        assertEquals(
            400,
            CompanionConfigurationRoute.handle(
                request("PUT", "10x0", "10x0".sha256()), secret, store, now,
            ).status,
        )
        assertEquals(
            404,
            CompanionConfigurationRoute.handle(
                request("PUT", "1010", "1010".sha256()).copy(bodyLength = 1), secret, store, now,
            ).status,
        )
        assertEquals(0, store.writeCount)
    }

    @Test fun missingOrPathMismatchedAuthorizationIsRejected() = runBlocking {
        val store = FakeStore(CompanionConfiguration(false, false, false, false))
        assertEquals(
            401,
            CompanionConfigurationRoute.handle(
                CompanionHttpRequest("GET", CompanionConfigurationRoute.PATH, null),
                secret, store, now,
            ).status,
        )
        val authorization = CompanionProtocol.createAuthorization(
            secret, "GET", "/v1/other", now,
        )
        assertEquals(
            401,
            CompanionConfigurationRoute.handle(
                CompanionHttpRequest("GET", CompanionConfigurationRoute.PATH, authorization),
                secret, store, now,
            ).status,
        )
    }

    private fun request(method: String, flags: String? = null, hash: String? = null): CompanionHttpRequest {
        val authorization = CompanionProtocol.createAuthorization(
            secret, method, CompanionConfigurationRoute.PATH, now, bodySha256 = hash,
        )
        return CompanionHttpRequest(
            method = method,
            path = CompanionConfigurationRoute.PATH,
            authorization = authorization,
            bodySha256 = hash,
            configurationFlags = flags,
        )
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.US_ASCII))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private class FakeStore(initial: CompanionConfiguration) : CompanionConfigurationStore {
        var value = initial
        var writeCount = 0
        override suspend fun read(): CompanionConfiguration = value
        override suspend fun write(configuration: CompanionConfiguration) {
            value = configuration
            writeCount++
        }
    }
}
