package com.gamebox.os.ui

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomePresentationTest {
    @Test
    fun emptyCatalogHasNoHomePresentation() {
        assertNull(homePresentationState(emptyList(), restoreGameId = null, compact = false))
    }

    @Test
    fun heroPrefersMostRecentlyPlayedInstalledGame() {
        val older = game("older", InstallState.INSTALLED, lastPlayed = "2026-09-30T10:00:00Z")
        val newest = game("newest", InstallState.INSTALLED, lastPlayed = "2026-10-01T10:00:00Z")

        val home = requireNotNull(homePresentationState(listOf(older, newest), restoreGameId = null, compact = false))

        assertEquals(newest.id, home.hero.id)
        assertEquals(listOf(newest, older), home.recentlyPlayed)
    }

    @Test
    fun installedCollectionIncludesUpdatesButNotPendingOrFailedTitles() {
        val installed = game("installed", InstallState.INSTALLED)
        val update = game("update", InstallState.UPDATE_AVAILABLE)
        val queued = game("queued", InstallState.QUEUED)
        val failed = game("failed", InstallState.FAILED)

        val home = requireNotNull(homePresentationState(listOf(installed, update, queued, failed), null, compact = false))

        assertEquals(listOf(installed, update), home.installedGames)
        assertEquals(installed.id, home.hero.id)
    }

    @Test
    fun emptyCollectionUsesLegalSampleWhenPresentAndRestoresCatalogFocus() {
        val first = game("first", InstallState.NOT_INSTALLED)
        val sample = game("galaxy-patrol", InstallState.NOT_INSTALLED)
        val focus = first.id

        val home = requireNotNull(homePresentationState(listOf(first, sample), focus, compact = false))

        assertEquals(sample.id, home.hero.id)
        assertEquals(focus, home.focusTarget)
    }

    @Test
    fun wideLayoutDoesNotRestoreFocusToAnInstalledCardThatIsNotRendered() {
        val recent = game("recent", InstallState.INSTALLED, lastPlayed = "2026-10-01T10:00:00Z")
        val olderTitles = (1..9).map { game("older-$it", InstallState.INSTALLED) }
        val hidden = olderTitles.last()

        val home = requireNotNull(
            homePresentationState(listOf(recent) + olderTitles, hidden.id, compact = false),
        )

        assertEquals(recent.id, home.hero.id)
        assertEquals(recent.id, home.focusTarget)
    }

    @Test
    fun compactLayoutCanRestoreFocusToAnyInstalledCardInItsCollection() {
        val recent = game("recent", InstallState.INSTALLED, lastPlayed = "2026-10-01T10:00:00Z")
        val installed = (1..9).map { game("installed-$it", InstallState.INSTALLED) }
        val target = installed.last().id

        val home = requireNotNull(
            homePresentationState(listOf(recent) + installed, target, compact = true),
        )

        assertEquals(target, home.focusTarget)
    }

    @Test
    fun secondaryHomeRowDoesNotRepeatContinuePlayingHero() {
        val hero = game("recent", InstallState.INSTALLED, lastPlayed = "2026-10-01T10:00:00Z")
        val otherRecent = game("other-recent", InstallState.INSTALLED, lastPlayed = "2026-09-30T10:00:00Z")

        val section = requireNotNull(homeSecondarySection(listOf(hero, otherRecent), listOf(hero, otherRecent), hero))

        assertEquals("Recently played", section.title)
        assertEquals(listOf(otherRecent), section.games)
    }

    @Test
    fun secondaryHomeRowFallsBackToOtherGamesOrDisappearsWhenHeroIsAlone() {
        val hero = game("hero", InstallState.INSTALLED)
        val discoverable = game("discoverable", InstallState.NOT_INSTALLED)

        val section = requireNotNull(homeSecondarySection(listOf(hero, discoverable), emptyList(), hero))

        assertEquals("Explore games", section.title)
        assertEquals(listOf(discoverable), section.games)
        assertNull(homeSecondarySection(listOf(hero), emptyList(), hero))
    }

    private fun game(id: String, state: InstallState, lastPlayed: String? = null) =
        Game(
            id = GameId(id),
            title = id,
            platform = "Retro",
            year = 2026,
            genre = "Arcade",
            sizeMb = 1,
            state = state,
            lastPlayed = lastPlayed,
        )
}
