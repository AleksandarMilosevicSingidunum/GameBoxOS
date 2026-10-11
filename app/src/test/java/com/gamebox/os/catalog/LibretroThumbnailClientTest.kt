package com.gamebox.os.catalog

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibretroThumbnailClientTest {
    @Test
    fun mappedVimmPresetPlatformsUsePublishedArtworkDirectories() {
        val platforms = listOf(
            "Dreamcast", "Game Boy Advance", "Game Boy Color", "GameCube", "N64", "NES",
            "PS1", "PS2", "PS3", "PSP", "SNES", "Wii", "WiiWare", "Xbox",
            "Xbox 360", "DS", "3DS",
        )
        platforms.forEach { platform ->
            assertEquals(true, systemDirectory(platform) != null)
        }
    }

    @Test
    fun switchDoesNotInventAnUnpublishedLibretroDirectory() {
        assertNull(systemDirectory("Switch"))
        assertEquals("Nintendo - Wii", systemDirectory("WiiWare"))
    }

    @Test
    fun mapsLongFormCatalogPlatformNamesToTheirArtworkDirectories() {
        mapOf(
            "Nintendo Game Boy" to "Nintendo - Game Boy",
            "Nintendo Game Boy Color" to "Nintendo - Game Boy Color",
            "Nintendo Game Boy Advance" to "Nintendo - Game Boy Advance",
            "Sony PlayStation" to "Sony - PlayStation",
            "Sony PlayStation 2" to "Sony - PlayStation 2",
            "Sony PlayStation 3" to "Sony - PlayStation 3",
            "Sony PlayStation Portable" to "Sony - PlayStation Portable",
            "Sega Dreamcast" to "Sega - Dreamcast",
            "Microsoft Xbox" to "Microsoft - Xbox",
            "Microsoft Xbox 360" to "Microsoft - Xbox 360",
        ).forEach { (platform, directory) ->
            assertEquals(platform, directory, systemDirectory(platform))
        }
    }

    @Test
    fun normalizesPunctuationAndUsesRegionToMatchNamedImages() = runBlocking {
        val expected = thumbnailUrl(
            "Nintendo - Nintendo DS", "Named_Boxarts",
            "Mario _ Luigi - Partners in Time (USA)",
        )
        val requests = mutableListOf<String>()
        val client = LibretroThumbnailClient { url ->
            requests += url
            url == expected || url == expected.replace("Named_Boxarts", "Named_Snaps")
        }

        val images = client.lookup("DS", "Mario & Luigi: Partners in Time", "USA")

        assertEquals(expected, images.boxartUrl)
        assertEquals(expected.replace("Named_Boxarts", "Named_Snaps"), images.screenshotUrl)
        // Name variants in the preferred batch are checked together; the archive
        // is no longer probed sequentially one network timeout at a time.
        assertEquals(8, requests.size)
    }

    @Test
    fun resultCardLookupProbesOnlyBoxartAndCachesPositiveMatch() = runBlocking {
        val expected = thumbnailUrl(
            "Sony - PlayStation Portable", "Named_Boxarts", "God of War - Chains of Olympus (USA)",
        )
        val requests = mutableListOf<String>()
        val client = LibretroThumbnailClient { url ->
            requests += url
            url == expected
        }

        assertEquals(expected, client.lookupBoxart("PSP", "God of War: Chains of Olympus", "USA"))
        val requestCount = requests.size
        assertEquals(expected, client.lookupBoxart("PSP", "God of War: Chains of Olympus", "USA"))
        assertEquals(requestCount, requests.size)
        assertTrue(requests.all { "/Named_Boxarts/" in it })
    }

    @Test
    fun lookupMatchesCombinedUsAndEuropeBoxartRelease() = runBlocking {
        val expected = thumbnailUrl(
            "Nintendo - Game Boy Advance", "Named_Boxarts", "Wario Land 4 (USA, Europe)",
        )
        val client = LibretroThumbnailClient { it == expected }

        assertEquals(expected, client.lookupBoxart("GBA", "Wario Land 4", "USA"))
    }

    @Test
    fun checksNameVariantsConcurrentlyInBoundedBatchesAndKeepsPreferredMatch() = runBlocking {
        val preferred = thumbnailUrl(
            "Sony - PlayStation Portable", "Named_Boxarts", "Crisis Core - Final Fantasy VII (Europe)",
        )
        val active = AtomicInteger()
        val maximumActive = AtomicInteger()
        val client = LibretroThumbnailClient { url ->
            val nowActive = active.incrementAndGet()
            maximumActive.updateAndGet { maxOf(it, nowActive) }
            try {
                delay(20)
                url == preferred
            } finally {
                active.decrementAndGet()
            }
        }

        val result = client.lookup("PSP", "Crisis Core: Final Fantasy VII", "Europe")

        assertEquals(preferred, result.boxartUrl)
        assertTrue("Candidates should overlap, rather than wait serially", maximumActive.get() > 1)
        assertTrue("Each request batch must stay bounded", maximumActive.get() <= 4)
    }

    @Test
    fun stripsStackedRegionAndLanguageSuffixesFromImportedFilename() {
        val candidates = thumbnailFileNames("Crisis Core: Final Fantasy VII (Europe) (En,Fr,De)", "Europe")

        assertTrue(candidates.indexOf("Crisis Core - Final Fantasy VII (Europe)") in 0 until candidates.size)
        assertTrue(
            candidates.indexOf("Crisis Core - Final Fantasy VII (Europe)") <
                candidates.indexOf("Crisis Core - Final Fantasy VII (USA)"),
        )
        assertEquals(candidates.size, candidates.distinct().size)
    }

    @Test
    fun unsupportedPlatformDoesNotProbeNetwork() = runBlocking {
        val client = LibretroThumbnailClient { error("unexpected request") }
        assertNull(client.lookup("Android", "Demo").boxartUrl)
    }

    @Test
    fun retriesNegativeLookupInsteadOfKeepingTransientNetworkMiss() = runBlocking {
        val cover = thumbnailUrl(
            "Sony - PlayStation Portable", "Named_Boxarts", "Crisis Core - Final Fantasy VII (Europe)",
        )
        var connected = false
        var probes = 0
        val client = LibretroThumbnailClient { url ->
            probes++
            connected && url == cover
        }

        assertNull(client.lookup("PSP", "Crisis Core: Final Fantasy VII", "Europe").boxartUrl)
        connected = true
        assertEquals(cover, client.lookup("PSP", "Crisis Core: Final Fantasy VII", "Europe").boxartUrl)
        assertTrue("A retry should still probe candidate filenames", probes >= 2)
        assertTrue("Candidate requests should remain finite", probes <= 20)
    }

    @Test
    fun archiveArtworkReplacesOlderProviderArtworkWhenExactCoverExists() = runBlocking {
        val archiveCover = thumbnailUrl(
            "Nintendo - Nintendo DS", "Named_Boxarts", "Mario _ Luigi - Partners in Time (USA)",
        )
        val game = Game(
            id = GameId("mario-partners"), title = "Mario & Luigi: Partners in Time",
            platform = "DS", year = 2005, genre = "RPG", sizeMb = 1,
            state = InstallState.INSTALLED, artworkUrl = "https://old.example/cover.jpg",
        )
        val enriched = LibretroThumbnailClient { it == archiveCover }.enrich(game)
        assertEquals(archiveCover, enriched.artworkUrl)
        assertEquals(archiveCover, enriched.providerArtworkUrl)
    }

    @Test
    fun enrichmentPrefersTheKnownReleaseRegion() = runBlocking {
        val europeanCover = thumbnailUrl(
            "Sony - PlayStation Portable", "Named_Boxarts", "Crisis Core - Final Fantasy VII (Europe)",
        )
        val game = Game(
            id = GameId("crisis-core"), title = "Crisis Core: Final Fantasy VII",
            platform = "PSP", year = 2007, genre = "RPG", sizeMb = 1,
            state = InstallState.INSTALLED, region = "Europe",
        )
        val enriched = LibretroThumbnailClient { it == europeanCover }.enrich(game)
        assertEquals(europeanCover, enriched.artworkUrl)
    }

    @Test
    fun enrichmentUsesCanonicalProviderTitleAfterDisplayTitleOverride() = runBlocking {
        val cover = thumbnailUrl(
            "Sony - PlayStation Portable", "Named_Boxarts", "Crisis Core - Final Fantasy VII (Europe)",
        )
        val game = Game(
            id = GameId("crisis-core"), title = "My Favorite PSP Game",
            providerTitle = "Crisis Core: Final Fantasy VII",
            platform = "PSP", year = 2007, genre = "RPG", sizeMb = 1,
            state = InstallState.INSTALLED, region = "Europe",
        )

        val enriched = LibretroThumbnailClient { it == cover }.enrich(game)

        assertEquals(cover, enriched.artworkUrl)
        assertEquals(cover, enriched.providerArtworkUrl)
    }
}
