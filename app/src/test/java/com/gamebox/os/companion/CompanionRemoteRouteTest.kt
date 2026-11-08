package com.gamebox.os.companion

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class CompanionRemoteRouteTest {
    private val secret = "1".repeat(64)
    private val now = 1_700_000_000L

    @Test
    fun authorizedNavigationCommandIsPublished() {
        var received: CompanionRemoteCommand? = null
        val response = route("{\"action\":\"right\"}", secret) { received = it; true }

        assertEquals(200, response.status)
        assertEquals(CompanionRemoteCommand.Key(KeyEvent.KEYCODE_DPAD_RIGHT), received)
    }

    @Test
    fun backUsesThePlatformBackDispatcherCommand() {
        var received: CompanionRemoteCommand? = null
        val response = route("{\"action\":\"back\"}", secret) { received = it; true }

        assertEquals(200, response.status)
        assertEquals(CompanionRemoteCommand.Back, received)
    }

    @Test
    fun gameboxRemoteTokenCanReadItsScopedReadyStatus() {
        val authorization = CompanionProtocol.createAuthorization(secret, "GET", CompanionRemoteRoute.PATH, now)
        val response = CompanionRemoteRoute.handle(
            request = CompanionHttpRequest(
                method = "GET", path = CompanionRemoteRoute.PATH, authorization = authorization,
            ),
            pairingSecret = secret,
            nowUnixTimeSeconds = now,
            acceptNonce = { _, _ -> true },
            publish = { false },
        )
        assertEquals(200, response.status)
        assertTrue(response.body.contains("ready"))
    }

    @Test
    fun invalidCommandAndInvalidAuthorizationAreRejected() {
        assertEquals(400, route("{\"action\":\"launch_anything\"}", secret) { true }.status)
        assertEquals(401, route("{\"action\":\"select\"}", "2".repeat(64)) { true }.status)
        assertEquals(503, route("{\"action\":\"select\"}", secret) { false }.status)
    }

    @Test
    fun textIsBoundedAndControlCharactersAreRejected() {
        assertTrue(route("{\"action\":\"text\",\"text\":\"hello\"}", secret) { it is CompanionRemoteCommand.Text }.status == 200)
        assertEquals(400, route("{\"action\":\"text\",\"text\":\"\\n\"}", secret) { true }.status)
        assertEquals(400, route("{\"action\":\"text\",\"text\":\"${"x".repeat(129)}\"}", secret) { true }.status)
    }

    @Test
    fun remoteTextRequiresAnActiveSearchField() {
        var published = false
        val response = route(
            "{\"action\":\"text\",\"text\":\"hello\"}",
            secret,
            textEntryAvailable = { false },
        ) { published = true; true }

        assertEquals(409, response.status)
        assertTrue(response.body.contains("search_not_active"))
        assertFalse(published)

        val backspace = route("{\"action\":\"backspace\"}", secret, textEntryAvailable = { false }) { true }
        assertEquals(409, backspace.status)
    }

    @Test
    fun remoteTextIsAcceptedWhenSearchFieldIsActive() {
        val response = route(
            "{\"action\":\"text\",\"text\":\"hello\"}",
            secret,
            textEntryAvailable = { true },
        ) { true }

        assertEquals(200, response.status)
    }

    @Test
    fun signedCommandNonceCanOnlyBeUsedOnce() {
        val body = "{\"action\":\"right\"}".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(body).joinToString("") { "%02x".format(it) }
        val auth = CompanionProtocol.createAuthorization(secret, "PUT", CompanionRemoteRoute.PATH, now, hash, "a".repeat(32))
        val replayGuard = CompanionAuthorizationReplayGuard()
        var published = 0
        fun send() = CompanionRemoteRoute.handle(
            request = CompanionHttpRequest(
                method = "PUT", path = CompanionRemoteRoute.PATH, authorization = auth,
                body = body, bodyLength = body.size.toLong(), bodySha256 = hash,
            ),
            pairingSecret = secret,
            nowUnixTimeSeconds = now,
            acceptNonce = { nonce, timestamp -> replayGuard.accept(nonce, timestamp, now) },
            publish = { published++; true },
        )

        assertEquals(200, send().status)
        assertEquals(401, send().status)
        assertEquals(1, published)
    }

    private fun route(
        bodyText: String,
        signingSecret: String,
        textEntryAvailable: () -> Boolean = { true },
        publish: (CompanionRemoteCommand) -> Boolean,
    ): CompanionHttpResponse {
        val body = bodyText.toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(body).joinToString("") { "%02x".format(it) }
        val authorization = CompanionProtocol.createAuthorization(signingSecret, "PUT", CompanionRemoteRoute.PATH, now, hash)
        return CompanionRemoteRoute.handle(
            request = CompanionHttpRequest(
                method = "PUT", path = CompanionRemoteRoute.PATH, authorization = authorization,
                body = body, bodyLength = body.size.toLong(), bodySha256 = hash,
            ),
            pairingSecret = secret,
            nowUnixTimeSeconds = now,
            acceptNonce = { _, _ -> true },
            textEntryAvailable = textEntryAvailable,
            publish = publish,
        )
    }
}
