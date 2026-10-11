package com.gamebox.os.ui

import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryDetailsSourcesTest {
    @Test
    fun trackedBrowserGuidanceExplainsBackgroundDownloadAndAutomaticImport() {
        val guidance = configuredSourceGuidance(tracksBrowserDownload = true)

        assertTrue(guidance.contains("continues in the background"))
        assertTrue(guidance.contains("appears in Downloads"))
        assertTrue(guidance.contains("imported automatically"))
        assertFalse(guidance.contains("Keep this title open"))
    }

    @Test
    fun untrackedSourceGuidanceDoesNotPromiseAutomaticDownloadOrImport() {
        val guidance = configuredSourceGuidance(tracksBrowserDownload = false)

        assertTrue(guidance.contains("discovery links only"))
        assertFalse(guidance.contains("continues in the background"))
        assertFalse(guidance.contains("imported automatically"))
    }

    @Test
    fun removedVimmDetailCanStillBrowseTheCurrentConsoleResults() {
        val links = configuredDetailsSourceLinks(
            title = "God of War: Chains of Olympus",
            platformName = "PSP",
            sources = listOf(
                GameSourceConfig(
                    id = "vimm",
                    name = "Vimm's Lair",
                    type = GameSourceProviderType.VIMM_LAIR,
                    baseUrl = "https://vimm.net/vault",
                    platforms = setOf("PSP"),
                ),
            ),
            directSource = null,
        )

        assertEquals("https://vimm.net/vault/PSP/G", links.single().url)
        assertTrue(links.single().tracksBrowserDownload)
    }
}
