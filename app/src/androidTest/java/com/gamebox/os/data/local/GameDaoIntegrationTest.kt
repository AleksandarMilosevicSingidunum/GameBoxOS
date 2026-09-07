package com.gamebox.os.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDaoIntegrationTest {
    private lateinit var database: GameBoxDatabase
    private lateinit var dao: GameDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GameBoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.gameDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun legacyCatalogRepairPreservesRealContentAndUserData() = runBlocking {
        fun sample(id: String) = GameEntity(
            id = id, title = id, platform = "Homebrew", year = 2026,
            genre = "Test", sizeMb = 1, installState = InstallState.INSTALLED.name,
            lastPlayed = "2026-09-01T10:00:00Z", minutesPlayed = 90, favorite = true,
        )
        dao.upsertAll(listOf(
            sample("cave-story"),
            sample("celeste").copy(localContentRelativePath = "imports/celeste/game.p8", localContentSha256 = "a".repeat(64)),
            sample("openarena").copy(sourceUrl = "https://example.test/game.apk", expectedSha256 = "b".repeat(64)),
            sample("supertuxkart").copy(localContentFilesJson = "retained local content"),
            sample("galaxy-patrol"),
            sample("user-game"),
            sample("luanti").copy(installState = InstallState.QUEUED.name),
            sample("openmw").copy(installState = InstallState.PAUSED.name),
        ))

        assertEquals(3, dao.clearLegacyCatalogInstallClaims())
        assertEquals(0, dao.clearLegacyCatalogInstallClaims()) // Safe on every startup.
        listOf("cave-story", "luanti", "openmw").forEach { id ->
            val game = requireNotNull(dao.getById(id))
            assertEquals(InstallState.NOT_INSTALLED.name, game.installState)
            assertTrue(game.favorite)
            assertEquals(90, game.minutesPlayed)
            assertEquals("2026-09-01T10:00:00Z", game.lastPlayed)
        }
        listOf("celeste", "openarena", "supertuxkart", "galaxy-patrol", "user-game").forEach { id ->
            assertEquals(InstallState.INSTALLED.name, dao.getById(id)?.installState)
        }
    }

    @Test
    fun upsertAndUpdatePreserveRichMetadata() = runBlocking {
        dao.upsertAll(
            listOf(
                GameEntity(
                    id = "integration-game",
                    title = "Integration Game",
                    platform = "Homebrew",
                    year = 2026,
                    genre = "Platformer",
                    sizeMb = 12,
                    installState = InstallState.INSTALLED.name,
                    lastPlayed = null,
                    minutesPlayed = 0,
                    artworkUrl = "https://example.com/art.jpg",
                    description = "DAO integration fixture",
                    players = "1-2",
                    language = "English",
                    region = "Worldwide"
                )
            )
        )

        assertEquals(1, dao.count())
        val stored = dao.getAllOnce().single()
        assertEquals("https://example.com/art.jpg", stored.artworkUrl)
        assertEquals("DAO integration fixture", stored.description)

        dao.updateFavorite("integration-game", true)
        dao.updateInstallState("integration-game", InstallState.UPDATE_AVAILABLE.name)
        val updated = dao.getAllOnce().single()
        assertTrue(updated.favorite)
        assertEquals(InstallState.UPDATE_AVAILABLE.name, updated.installState)
        assertEquals("1-2", updated.players)
    }
}
