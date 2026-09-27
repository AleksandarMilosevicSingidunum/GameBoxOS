package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class TheGamesDbCancellationTest {
    @Test
    fun catalogTransportDisconnectsWhenCancelled() = runBlocking {
        val connection = BlockingHttpsConnection()
        val created = CountDownLatch(1)
        val transport = HttpsTheGamesDbCatalogTransport(
            connectionFactory = {
                created.countDown()
                connection
            },
        )
        val operation = async(Dispatchers.IO) {
            transport.get(URI("https://api.thegamesdb.net/v1/Games/ByGameName"))
        }
        assertTrue(created.await(2, TimeUnit.SECONDS))
        operation.cancel()
        assertTrue(connection.disconnected.await(2, TimeUnit.SECONDS))
        operation.cancelAndJoin()
    }

    @Test
    fun metadataLookupDisconnectsWhenCancelledInsteadOfSwallowingCancellation() = runBlocking {
        val connection = BlockingHttpsConnection()
        val created = CountDownLatch(1)
        val client = TheGamesDbMetadataClient(
            apiKey = { "test-key" },
            connectionFactory = {
                created.countDown()
                connection
            },
        )
        val operation = async(Dispatchers.IO) {
            client.findCandidates(
                Game(
                    id = GameId("game"),
                    title = "Example",
                    platform = "NES",
                    year = 1980,
                    genre = "",
                    sizeMb = 0,
                    state = InstallState.NOT_INSTALLED,
                ),
            )
        }
        assertTrue(created.await(2, TimeUnit.SECONDS))
        operation.cancel()
        assertTrue(connection.disconnected.await(2, TimeUnit.SECONDS))
        operation.cancelAndJoin()
    }

    private class BlockingHttpsConnection : HttpsURLConnection(URL("https://api.thegamesdb.net")) {
        val disconnected = CountDownLatch(1)
        @Volatile private var closed = false

        override fun getResponseCode() = HTTP_OK
        override fun getInputStream(): InputStream = object : InputStream() {
            override fun read(): Int {
                while (!closed) Thread.sleep(5)
                throw IOException("connection disconnected")
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = read()
        }
        override fun disconnect() {
            closed = true
            disconnected.countDown()
        }
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite(): String? = null
        override fun getLocalCertificates() = null
        override fun getServerCertificates() = null
        override fun getPeerPrincipal() = null
        override fun getLocalPrincipal() = null
    }
}
