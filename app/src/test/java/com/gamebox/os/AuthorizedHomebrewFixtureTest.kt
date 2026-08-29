package com.gamebox.os

import com.gamebox.os.download.AuthorizedHomebrewDownload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizedHomebrewFixtureTest {
    @Test
    fun fixtureMetadataMatchesPinnedRunnableRom() {
        assertEquals("downloads/galaxy-patrol.nes", AuthorizedHomebrewDownload.ASSET_PATH)
        assertEquals("retro/galaxy-patrol/content/galaxy-patrol.nes", AuthorizedHomebrewDownload.RELATIVE_PATH)
        assertEquals(49_168L, AuthorizedHomebrewDownload.SIZE_BYTES)
        assertTrue(AuthorizedHomebrewDownload.MAX_BYTES >= AuthorizedHomebrewDownload.SIZE_BYTES)
        assertEquals(64, AuthorizedHomebrewDownload.SHA256.length)
    }
}
