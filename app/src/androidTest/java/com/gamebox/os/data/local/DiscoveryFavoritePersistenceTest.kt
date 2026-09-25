package com.gamebox.os.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoveryFavoritePersistenceTest {
    @Test
    fun favoriteIdsQueryReturnsOnlyExistingFavoriteRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, GameBoxDatabase::class.java).build()
        try {
            val rows = listOf(
                game("favorite", true),
                game("normal", false),
            )
            database.catalogDiscoveryDao().upsertGames(rows)

            assertEquals(
                listOf("favorite"),
                database.catalogDiscoveryDao()
                    .favoriteGameIds(listOf("favorite", "normal", "missing")),
            )
        } finally {
            database.close()
        }
    }

    private fun game(id: String, favorite: Boolean) = CatalogGameEntity(
        id = id,
        title = id,
        normalizedTitle = id,
        platformId = "ps2",
        region = null,
        releaseDate = null,
        description = null,
        developer = null,
        publisher = null,
        players = null,
        rating = null,
        coverUrl = null,
        backgroundUrl = null,
        logoUrl = null,
        screenshotsJson = null,
        favorite = favorite,
        updatedAtMillis = 1L,
    )
}
