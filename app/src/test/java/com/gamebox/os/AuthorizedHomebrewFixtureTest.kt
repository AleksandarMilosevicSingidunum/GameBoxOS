package com.gamebox.os

import com.gamebox.os.download.AuthorizedHomebrewDownload
import com.gamebox.os.download.InstalledContentStatus
import com.gamebox.os.download.reconcileAuthorizedInstallState
import com.gamebox.os.domain.InstallState
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
        assertEquals(
            "97c1757ffd6a5bc1a591809b2b0f8988741f61f6abd82889c148ecae8a2f471f",
            AuthorizedHomebrewDownload.SHA256
        )
    }

    @Test
    fun persistedAuthorizedContentRepairsRoomStateAfterColdStart() {
        assertEquals(
            InstallState.INSTALLED,
            reconcileAuthorizedInstallState(
                InstallState.NOT_INSTALLED,
                InstalledContentStatus.VERIFIED,
            ),
        )
        assertEquals(
            InstallState.MISSING_FILES,
            reconcileAuthorizedInstallState(
                InstallState.INSTALLED,
                InstalledContentStatus.MISSING,
            ),
        )
        assertEquals(
            InstallState.FAILED,
            reconcileAuthorizedInstallState(
                InstallState.INSTALLED,
                InstalledContentStatus.ALTERED,
            ),
        )
    }
}
