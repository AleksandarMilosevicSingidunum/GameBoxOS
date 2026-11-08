package com.gamebox.os.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.gamebox.os.source.defaultVimmLairGameSource
import com.gamebox.os.source.encodeGameSourceConfigs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPreferenceMapperTest {
    @Test
    fun emptyPreferencesUseProductDefaults() {
        val settings = mutablePreferencesOf().toGameBoxSettings()

        assertEquals(0f, settings.safeAreaPercent)
        assertFalse(settings.reducedMotion)
        assertTrue(settings.uiSoundEffectsEnabled)
        assertTrue(settings.downloadsUnmeteredOnly)
        assertEquals("Local player", settings.activeProfileName)
        assertEquals("HTTPS", settings.catalogTransport)
        assertEquals("WEBDAV", settings.cloudSaveProvider)
        assertEquals(49_500, settings.companionPort)
        assertEquals(47_984, settings.moonlightPort)
        assertEquals(listOf(defaultVimmLairGameSource()), settings.gameSources)
        assertEquals(DEFAULT_TV_FAVORITE_PACKAGES, settings.tvFavoritePackages)
        assertTrue(settings.tvFavoriteChannels.isEmpty())
        assertEquals(DEFAULT_TV_HOME_SECTIONS, settings.tvHomeSections)
    }

    @Test
    fun explicitlyEmptyTvFavoritesStayEmpty() {
        val preferences = mutablePreferencesOf().apply { this[TV_FAVORITE_PACKAGES] = "" }

        assertTrue(preferences.toGameBoxSettings().tvFavoritePackages.isEmpty())
    }

    @Test
    fun tvHomeStateFiltersMalformedValuesAndKeepsLastChannel() {
        val preferences = mutablePreferencesOf().apply {
            this[TV_FAVORITE_CHANNELS] = "rts1.rs\n\ninvalid\u0001key\npink.rs"
            this[TV_LAST_CHANNEL] = "rts1.rs"
            this[TV_HOME_SECTIONS] = "GUIDE\nUNKNOWN\nLIVE_TV\nGUIDE"
        }

        val settings = preferences.toGameBoxSettings()

        assertEquals(listOf("rts1.rs", "pink.rs"), settings.tvFavoriteChannels)
        assertEquals("rts1.rs", settings.tvLastChannel)
        assertEquals(listOf("GUIDE", "LIVE_TV"), settings.tvHomeSections)
    }

    @Test
    fun explicitlyRemovedSourcesStayRemoved() {
        val preferences = mutablePreferencesOf().apply {
            this[GAME_SOURCES] = encodeGameSourceConfigs(emptyList())
        }

        assertTrue(preferences.toGameBoxSettings().gameSources.isEmpty())
    }

    @Test
    fun invalidPersistedPortsAreNormalized() {
        val preferences = mutablePreferencesOf().apply {
            this[COMPANION_PORT] = 1
            this[MOONLIGHT_PORT] = 70_000
        }

        val settings = preferences.toGameBoxSettings()

        assertEquals(10_240, settings.companionPort)
        assertEquals(65_535, settings.moonlightPort)
    }
}
