package com.gamebox.os.companion

import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import java.nio.file.Files
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CompanionContentTransferTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    private class FakeGames : GameRepository {
        private val item = Game(
            GameId("galaxy-patrol"), "Galaxy Patrol", "NES", 2026,
            "Homebrew", 1, InstallState.NOT_INSTALLED, favorite = true,
        )
        private val games = MutableStateFlow(listOf(item))
        var registration: ImportedGameRegistration? = null
        override fun observeGames(): StateFlow<List<Game>> = games
        override fun game(id: GameId): Game? = item.takeIf { it.id == id }
        override fun setFavorite(id: GameId, favorite: Boolean) = Unit
        override fun setEmulatorSettings(id: GameId, packageName: String?, graphicsProfile: String) = Unit
        override fun setInstallState(id: GameId, state: InstallState) = Unit
        override fun recordPlaySession(id: GameId, endedAtMillis: Long, minutesPlayed: Int) = Unit
        override fun observeCatalogRefreshState(): StateFlow<CatalogRefreshState> =
            MutableStateFlow(CatalogRefreshState.IDLE)
        override fun refreshCatalog() = Unit
        override suspend fun registerImportedGame(imported: ImportedGameRegistration) {
            registration = imported
        }
    }

    @Test fun streamedOwnedCopyIsVerifiedInstalledAndRegistered(): Unit = runBlocking {
        val root = Files.createTempDirectory("companion-content-").toFile()
        try {
            val staged = root.resolve("request.partial").apply { writeBytes("NES-CONTENT".toByteArray()) }
            val hash = CompanionSaveTransferStore.sha256(staged.readBytes())
            val path = "/v1/content/galaxy-patrol"
            val fileName = Base64.getEncoder().encodeToString("Galaxy Patrol.nes".toByteArray())
            val games = FakeGames()
            val response = CompanionContentRoute.handle(
                CompanionHttpRequest(
                    method = "PUT",
                    path = path,
                    authorization = CompanionProtocol.createAuthorization(
                        secret, "PUT", path, 1_700_000_000, hash,
                    ),
                    bodyFile = staged,
                    bodyLength = staged.length(),
                    bodySha256 = hash,
                    fileName = fileName,
                ),
                secret,
                CompanionContentTransferStore(root, games),
                1_700_000_030,
            )
            assertEquals(200, response.status)
            assertEquals("NES-CONTENT", root.resolve("imports/galaxy-patrol/Galaxy Patrol.nes").readText())
            val registered = requireNotNull(games.registration)
            assertEquals("galaxy-patrol/Galaxy Patrol.nes", registered.relativePath)
            assertEquals(hash, registered.sha256)
            assertTrue(registered.favorite)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsWrongFormatChecksumUnknownGameAndTraversal(): Unit = runBlocking {
        val root = Files.createTempDirectory("companion-content-invalid-").toFile()
        try {
            val games = FakeGames()
            suspend fun rejected(id: String, file: String, hash: String): Boolean {
                val staged = root.resolve("request-" + System.nanoTime()).apply { writeText("CONTENT") }
                return runCatching {
                    CompanionContentTransferStore(root, games).import(
                        id,
                        Base64.getEncoder().encodeToString(file.toByteArray()),
                        staged,
                        staged.length(),
                        hash,
                    )
                }.isFailure
            }
            val validHash = CompanionSaveTransferStore.sha256("CONTENT".toByteArray())
            assertTrue(rejected("galaxy-patrol", "game.exe", validHash))
            assertTrue(rejected("galaxy-patrol", "../game.nes", "0".repeat(64)))
            assertTrue(rejected("missing-game", "game.nes", validHash))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun requestReaderAuthenticatesBeforeReadingAndStagesContent(): Unit {
        val root = Files.createTempDirectory("companion-reader-").toFile()
        try {
            val bytes = "STREAMED".toByteArray()
            val hash = CompanionSaveTransferStore.sha256(bytes)
            val path = "/v1/content/galaxy-patrol"
            val authorization = CompanionProtocol.createAuthorization(secret, "PUT", path, 1_700_000_000, hash)
            val raw = (
                "PUT " + path + " HTTP/1.1\r\n" +
                    CompanionProtocol.AUTHORIZATION_HEADER + ": " + authorization + "\r\n" +
                    CompanionHttpRequestReader.BODY_SHA256_HEADER + ": " + hash + "\r\n" +
                    CompanionHttpRequestReader.FILE_NAME_HEADER + ": " +
                    Base64.getEncoder().encodeToString("game.nes".toByteArray()) + "\r\n" +
                    "Content-Type: application/octet-stream\r\nContent-Length: " + bytes.size +
                    "\r\n\r\n"
                ).toByteArray() + bytes
            val request = CompanionHttpRequestReader.read(
                raw.inputStream(),
                bodyDirectory = root,
                authorizeHead = { head ->
                    CompanionProtocol.verifyAuthorization(
                        secret, head.method, head.path, head.authorization,
                        1_700_000_030, bodySha256 = head.declaredBodySha256,
                    )
                },
            )
            assertEquals(hash, request.bodySha256)
            assertArrayEquals(bytes, requireNotNull(request.bodyFile).readBytes())
            request.bodyFile?.delete()

            var bodyRead = false
            val bodyWatchingInput = object : java.io.ByteArrayInputStream(raw) {
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    bodyRead = true
                    return super.read(b, off, len)
                }
            }
            assertTrue(runCatching {
                CompanionHttpRequestReader.read(
                    bodyWatchingInput,
                    bodyDirectory = root,
                    authorizeHead = { false },
                )
            }.isFailure)
            assertFalse(bodyRead)
        } finally {
            root.deleteRecursively()
        }
    }
}
