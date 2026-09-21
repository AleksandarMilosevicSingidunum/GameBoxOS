package com.gamebox.os.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderMetadataMatchDaoTest {
    @Test
    fun confirmedMatchUpdatesProviderValuesAndPreservesUserCorrections() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, GameBoxDatabase::class.java).build()
        try {
            database.gameDao().upsert(
                GameEntity(
                    id = "matched-game",
                    title = "Original provider title",
                    platform = "PS1",
                    year = 1998,
                    genre = "Adventure",
                    sizeMb = 1,
                    installState = "INSTALLED",
                    lastPlayed = null,
                    minutesPlayed = 0,
                    userTitle = "My corrected title",
                )
            )

            assertEquals(
                1,
                database.gameDao().applyProviderMetadataMatch(
                    id = "matched-game",
                    provider = "THE_GAMES_DB",
                    externalId = "202",
                    title = "Confirmed provider title",
                    year = 1999,
                    genre = "Racing",
                    artworkUrl = "https://cdn.example/cover.jpg",
                    description = "Confirmed description",
                    matchedAtMillis = 1234L,
                )
            )

            val stored = requireNotNull(database.gameDao().getById("matched-game"))
            assertEquals("Confirmed provider title", stored.title)
            assertEquals("My corrected title", stored.userTitle)
            assertEquals("THE_GAMES_DB", stored.metadataProvider)
            assertEquals("202", stored.metadataExternalId)
            assertEquals(1234L, stored.metadataMatchedAtMillis)
            val game = stored.toDomain()
            assertEquals("My corrected title", game.title)
            assertEquals("Confirmed provider title", game.providerTitle)
            assertEquals("https://cdn.example/cover.jpg", game.artworkUrl)
        } finally {
            database.close()
        }
    }

    @Test
    fun unknownGameCannotCreateOrphanedProvenance() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, GameBoxDatabase::class.java).build()
        try {
            assertEquals(
                0,
                database.gameDao().applyProviderMetadataMatch(
                    id = "missing",
                    provider = "THE_GAMES_DB",
                    externalId = "1",
                    title = "Missing",
                    year = null,
                    genre = null,
                    artworkUrl = null,
                    description = null,
                    matchedAtMillis = 1L,
                )
            )
            assertNull(database.gameDao().getById("missing"))
        } finally {
            database.close()
        }
    }
}
