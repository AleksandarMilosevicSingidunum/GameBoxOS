package com.gamebox.os.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.data.RoomCatalogDiscoveryRepository
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoveryDetailLookupTest {
    @Test fun exactLookupSurvivesDatabaseReopenOutsideFirstPage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "discovery-detail-" + java.util.UUID.randomUUID() + ".db"
        fun open() = Room.databaseBuilder(context, GameBoxDatabase::class.java, name).build()
        var db = open()
        try {
            val rows = (0..300).map { index ->
                val id = "game-" + index.toString().padStart(3, '0')
                CatalogGameEntity(
                    id = id, title = id, normalizedTitle = id, platformId = "psp",
                    region = "EU", releaseDate = "2006-01-01", description = "Description",
                    developer = null, publisher = null, players = "2", rating = 9.0,
                    coverUrl = "https://example.test/cover.jpg",
                    backgroundUrl = "https://example.test/hero.jpg", logoUrl = null,
                    screenshotsJson = "https://example.test/a.jpg\nhttps://example.test/b.jpg",
                    favorite = false, updatedAtMillis = 1L,
                )
            }
            db.catalogDiscoveryDao().upsertGames(rows)
            assertFalse(db.catalogDiscoveryDao().observeGames(null, "", 250, 0).first().any { it.id == "game-300" })
            db.close()
            db = open()
            val repository = RoomCatalogDiscoveryRepository(db.catalogDiscoveryDao())
            val restored = requireNotNull(repository.observeGame(GameId("game-300")).first())
            assertEquals("game-300", restored.id.value)
            assertEquals("game-300", restored.title)
            assertEquals("psp", restored.platformId)
            assertEquals("EU", restored.region)
            assertEquals(listOf("https://example.test/a.jpg", "https://example.test/b.jpg"), restored.screenshots)
            assertNull(repository.observeGame(GameId("missing")).first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
