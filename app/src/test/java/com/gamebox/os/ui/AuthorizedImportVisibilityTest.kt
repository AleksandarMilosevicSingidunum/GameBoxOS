package com.gamebox.os.ui

import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizedImportVisibilityTest {
    @Test
    fun unavailableCatalogStatesExposeAuthorizedImport() {
        assertTrue(shouldShowAuthorizedImportCard(InstallState.NOT_INSTALLED, authorizedFixture = false))
        assertTrue(shouldShowAuthorizedImportCard(InstallState.MISSING_FILES, authorizedFixture = false))
        assertTrue(shouldShowAuthorizedImportCard(InstallState.FAILED, authorizedFixture = false))
    }

    @Test
    fun installedOrActiveStatesDoNotExposeDuplicateImportSurface() {
        assertFalse(shouldShowAuthorizedImportCard(InstallState.INSTALLED, authorizedFixture = false))
        assertFalse(shouldShowAuthorizedImportCard(InstallState.DOWNLOADING, authorizedFixture = false))
        assertFalse(shouldShowAuthorizedImportCard(InstallState.NOT_INSTALLED, authorizedFixture = true))
    }
}
