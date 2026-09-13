package com.gamebox.os.companion

import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CompanionSaveTransferStoreTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private class FakeGames : GameRepository {
        private val game = Game(GameId("test-game"), "Test", "NES", 2000, "Test", 1, InstallState.INSTALLED)
        private val games = MutableStateFlow(listOf(game))
        var registered: List<Any> = emptyList()
        override fun observeGames(): StateFlow<List<Game>> = games
        override fun game(id: GameId): Game? = game.takeIf { it.id == id }
        override fun setFavorite(id: GameId, favorite: Boolean) = Unit
        override fun setEmulatorSettings(id: GameId, packageName: String?, graphicsProfile: String) = Unit
        override fun setInstallState(id: GameId, state: InstallState) = Unit
        override fun recordPlaySession(id: GameId, endedAtMillis: Long, minutesPlayed: Int) = Unit
        override fun observeCatalogRefreshState(): StateFlow<CatalogRefreshState> =
            MutableStateFlow(CatalogRefreshState.IDLE)
        override fun refreshCatalog() = Unit
        override suspend fun registerImportedGame(imported: ImportedGameRegistration) = Unit
        override suspend fun registerManagedSave(
            id: GameId,
            relativePath: String,
            updatedAtMillis: Long,
            sizeBytes: Long,
        ) {
            registered = listOf(id, relativePath, updatedAtMillis, sizeBytes)
        }
    }

    @Test fun importExportAndConflictPreservationAreVerified(): Unit = runBlocking {
        val root = Files.createTempDirectory("companion-save-").toFile()
        try {
            val games = FakeGames()
            val store = CompanionSaveTransferStore(root, games) { 1234L }
            val first = store.import("test-game", "FIRST".toByteArray())
            assertFalse(first.conflictPreserved)
            assertEquals(listOf(GameId("test-game"), "test-game/save.dat", 1234L, 5L), games.registered)
            val exported = requireNotNull(store.export("test-game"))
            assertArrayEquals("FIRST".toByteArray(), exported.bytes)
            assertEquals(CompanionSaveTransferStore.sha256(exported.bytes), exported.sha256)

            val second = store.import("test-game", "SECOND".toByteArray())
            assertTrue(second.conflictPreserved)
            assertEquals("SECOND", root.resolve("saves/test-game/save.dat").readText())
            assertEquals(
                "FIRST",
                root.resolve("save-conflicts/test-game/windows-1234.save").readText(),
            )
            assertFalse(root.resolve("saves/test-game/save.dat.companion-partial").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsTraversalUnknownGamesAndInvalidSizes(): Unit = runBlocking {
        val root = Files.createTempDirectory("companion-save-invalid-").toFile()
        try {
            val store = CompanionSaveTransferStore(root, FakeGames())
            listOf("../game", "bad/game", "").forEach { id ->
                assertTrue(runCatching { store.import(id, byteArrayOf(1)) }.isFailure)
            }
            assertTrue(runCatching { store.import("missing", byteArrayOf(1)) }.isFailure)
            assertTrue(runCatching { store.import("test-game", byteArrayOf()) }.isFailure)
        } finally {
            root.deleteRecursively()
        }
    }
    @Test fun authenticatedRouteTransfersExactGameScopedBytes(): Unit = runBlocking {
        val root = Files.createTempDirectory("companion-save-route-").toFile()
        try {
            val store = CompanionSaveTransferStore(root, FakeGames()) { 1234L }
            val uploaded = "ROUTE-SAVE".toByteArray()
            val path = "/v1/saves/test-game"
            val uploadHash = CompanionSaveTransferStore.sha256(uploaded)
            val uploadAuthorization = CompanionProtocol.createAuthorization(
                secret, "PUT", path, 1_700_000_000, uploadHash
            )
            val upload = CompanionSaveRoute.handle(
                CompanionHttpRequest("PUT", path, uploadAuthorization, uploaded),
                secret,
                store,
                1_700_000_030,
            )
            assertEquals(200, upload.status)
            assertTrue(upload.body.contains(uploadHash))

            val downloadAuthorization = CompanionProtocol.createAuthorization(
                secret, "GET", path, 1_700_000_000
            )
            val download = CompanionSaveRoute.handle(
                CompanionHttpRequest("GET", path, downloadAuthorization),
                secret,
                store,
                1_700_000_030,
            )
            assertEquals(200, download.status)
            assertTrue(download.body.contains(java.util.Base64.getEncoder().encodeToString(uploaded)))

            val tampered = CompanionSaveRoute.handle(
                CompanionHttpRequest("PUT", path, uploadAuthorization, "ALTERED".toByteArray()),
                secret,
                store,
                1_700_000_030,
            )
            assertEquals(401, tampered.status)
            assertEquals("ROUTE-SAVE", root.resolve("saves/test-game/save.dat").readText())
        } finally {
            root.deleteRecursively()
        }
    }

}
