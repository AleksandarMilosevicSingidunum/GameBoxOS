package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.GameId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogFeaturedHeroTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyCatalogShowsHelpfulFeatureCopyAndDisablesDetailsAction() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(800.dp)) {
                    CatalogFeaturedHero(null, null, Modifier.fillMaxWidth(), {}, {})
                }
            }
        }

        compose.onNodeWithText("Find your next adventure").assertIsDisplayed()
        compose.onNodeWithText("Browse local-first metadata and authorized sources.").assertIsDisplayed()
        compose.onNodeWithContentDescription("View featured game details").assertIsNotEnabled()
    }

    @Test
    fun activatingFeaturedDiscoveryOpensItsDetails() {
        val featuredId = GameId("featured-adventure")
        val featured = DiscoveryGame(
            id = featuredId,
            title = "Featured Adventure",
            platformId = "nes",
            region = null,
            releaseDate = null,
            description = "A locally cached discovery title",
            players = "1",
            rating = 8.5,
            coverUrl = null,
            backgroundUrl = null,
            logoUrl = null,
            screenshots = emptyList(),
            favorite = false,
        )
        var openedId: GameId? = null
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(800.dp)) {
                    CatalogFeaturedHero(
                        discoveryGame = featured,
                        authorizedGame = null,
                        modifier = Modifier.fillMaxWidth(),
                        onOpenDiscovery = { openedId = it.id },
                        onOpenAuthorized = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Featured Adventure").assertIsDisplayed()
        compose.onNodeWithContentDescription("View featured game details")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(featuredId, openedId) }
    }

    @Test
    fun featuredArtworkFallbackUsesGameTitleRatherThanSelectedFilter() {
        val featured = DiscoveryGame(
            id = GameId("cave-story"),
            title = "Cave Story",
            platformId = "retro",
            region = null,
            releaseDate = null,
            description = "A compact freeware adventure classic.",
            players = "1",
            rating = null,
            coverUrl = null,
            backgroundUrl = null,
            logoUrl = null,
            screenshots = emptyList(),
            favorite = false,
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(800.dp)) {
                    CatalogFeaturedHero(featured, null, Modifier.fillMaxWidth(), {}, {})
                }
            }
        }

        compose.onNodeWithText("CS").assertIsDisplayed()
        compose.onNodeWithText("GB").assertDoesNotExist()
    }

    @Test
    fun wideFeaturedHeroShowsDedicatedCoverWhileCompactHeroStaysUncluttered() {
        val featured = DiscoveryGame(
            id = GameId("featured-cover"),
            title = "Featured Cover Game",
            platformId = "nes",
            region = null,
            releaseDate = null,
            description = "A featured title with box art.",
            players = "1",
            rating = null,
            coverUrl = "https://example.org/featured-cover.png",
            backgroundUrl = null,
            logoUrl = null,
            screenshots = emptyList(),
            favorite = false,
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(800.dp)) {
                    CatalogFeaturedHero(featured, null, Modifier.fillMaxWidth(), {}, {})
                }
            }
        }

        compose.onNodeWithContentDescription("Featured Cover Game cover art").assertIsDisplayed()

        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(600.dp)) {
                    CatalogFeaturedHero(featured, null, Modifier.fillMaxWidth(), {}, {})
                }
            }
        }

        compose.onNodeWithContentDescription("Featured Cover Game cover art").assertDoesNotExist()
    }
}
