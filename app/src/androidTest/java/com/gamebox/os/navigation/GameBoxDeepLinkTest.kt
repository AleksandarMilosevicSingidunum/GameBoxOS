package com.gamebox.os.navigation

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameBoxDeepLinkTest {
    @Test fun downloadAndGameDestinationsRoundTripThroughValidatedIntent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val downloads = GameBoxDeepLink.parse(
            GameBoxDeepLink.intent(context, "DOWNLOADS")
        )
        val completed = GameBoxDeepLink.parse(
            GameBoxDeepLink.intent(context, "LIBRARY", "galaxy-patrol")
        )

        assertEquals("DOWNLOADS", downloads?.destination)
        assertNull(downloads?.gameId)
        assertEquals("LIBRARY", completed?.destination)
        assertEquals("galaxy-patrol", completed?.gameId)
    }

    @Test fun untrustedDestinationsAreRejectedAndInvalidGameIdsAreIgnored() {
        val invalidDestination = Intent()
            .putExtra(GameBoxDeepLink.EXTRA_DESTINATION, "ADMIN")
            .putExtra(GameBoxDeepLink.EXTRA_GAME_ID, "galaxy-patrol")
        val invalidGame = Intent()
            .putExtra(GameBoxDeepLink.EXTRA_DESTINATION, "LIBRARY")
            .putExtra(GameBoxDeepLink.EXTRA_GAME_ID, "../another-game")

        assertNull(GameBoxDeepLink.parse(invalidDestination))
        assertEquals(
            GameBoxNavigationRequest("LIBRARY"),
            GameBoxDeepLink.parse(invalidGame),
        )
    }
}
