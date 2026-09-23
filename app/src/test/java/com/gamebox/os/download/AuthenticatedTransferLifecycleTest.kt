package com.gamebox.os.download

import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.CatalogProviderConfig
import com.gamebox.os.catalog.CatalogTransport
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AuthenticatedTransferLifecycleTest {
    @get:Rule val temporary = TemporaryFolder()
    private val payload = "Authorized homebrew transfer fixture".toByteArray()
    private val url = "https://games.test/library/demo.nes"
    private val config = CatalogProviderConfig(CatalogTransport.WebDav("https://games.test/library"), "dav")
    private val auth = CatalogDownloadAuthorization(config, CatalogCredentials(username = "player", password = "secret"))
    private val hash get() = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }

    @Test fun authenticatedResumeVerifiesAndCommitsFullContent() {
        val target = FileStagingTarget(temporary.newFolder(), "game/content.nes")
        target.openOutput().use { it.write(payload, 0, 7) }
        val connection = AuthorizedConnection()
        val source = HttpsTransferSource(url, payload.size.toLong(), hash, auth::headers, { connection })
        val result = ResumableTransferEngine().transfer(source, target, 1024, { false }, {})
        assertEquals(ResumableTransferResult.Success(payload.size.toLong()), result)
        assertEquals("bytes=7-", connection.getRequestProperty("Range"))
        assertArrayEquals(payload, target.finalFile.readBytes())
        assertFalse(target.stagingFile.exists())
        assertTrue(connection.disconnected)
    }

    @Test fun rejectedCredentialsKeepPartialFileWithoutInstalling() {
        val target = FileStagingTarget(temporary.newFolder(), "game/content.nes")
        target.openOutput().use { it.write(payload, 0, 7) }
        val connection = AuthorizedConnection()
        val source = HttpsTransferSource(url, null, hash, connectionFactory = { connection })
        val result = ResumableTransferEngine().transfer(source, target, 1024, { false }, {})
        assertTrue(result is ResumableTransferResult.Failed)
        assertTrue((result as ResumableTransferResult.Failed).reason.contains("401"))
        assertEquals(7L, target.stagedBytes)
        assertFalse(target.finalFile.exists())
        assertTrue(connection.disconnected)
    }

    @Test fun authenticatedContentStillRequiresMatchingChecksum() {
        val target = FileStagingTarget(temporary.newFolder(), "game/content.nes")
        val source = HttpsTransferSource(url, null, "0".repeat(64), auth::headers, { AuthorizedConnection() })
        val result = ResumableTransferEngine().transfer(source, target, 1024, { false }, {})
        assertTrue(result is ResumableTransferResult.ChecksumMismatch)
        assertFalse(target.finalFile.exists())
        assertFalse(target.stagingFile.exists())
    }

    private inner class AuthorizedConnection : HttpURLConnection(URL(url)) {
        var disconnected = false
        private val offset get() = getRequestProperty("Range")?.removePrefix("bytes=")?.removeSuffix("-")?.toInt() ?: 0
        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getResponseCode(): Int =
            if (getRequestProperty("Authorization") != "Basic cGxheWVyOnNlY3JldA==") 401
            else if (offset > 0) 206 else 200
        override fun getContentLengthLong() = (payload.size - offset).toLong()
        override fun getHeaderField(name: String): String? =
            if (name == "Content-Range") "bytes " + offset + "-" + (payload.size - 1) + "/" + payload.size else null
        override fun getInputStream() = ByteArrayInputStream(payload.copyOfRange(offset, payload.size))
    }
}
