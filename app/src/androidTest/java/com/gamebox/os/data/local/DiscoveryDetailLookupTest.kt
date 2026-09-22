package com.gamebox.os.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
            val restored = requireNotNull(db.catalogDiscoveryDao().observeGame("game-300").first())
            assertEquals(rows.last(), restored)
            assertNull(db.catalogDiscoveryDao().observeGame("missing").first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
