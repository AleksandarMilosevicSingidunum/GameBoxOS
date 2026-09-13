package com.gamebox.os.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class CloudBackupUninstallGateTest {
    @Test
    fun successfulCloudBackupAllowsRemoval() {
        assertTrue(
            requireCloudBackupOrAcknowledgement(
                uploadSucceeded = true,
                allowWithoutCloudBackup = false,
                failureReason = "unused",
            )
        )
    }

    @Test
    fun failedCloudBackupBlocksRemovalWithoutAcknowledgement() {
        val error = assertThrows(CloudBackupAcknowledgementRequired::class.java) {
            requireCloudBackupOrAcknowledgement(
                uploadSucceeded = false,
                allowWithoutCloudBackup = false,
                failureReason = "network is offline",
            )
        }
        assertTrue(error.message.orEmpty().contains("Content was not removed"))
        assertTrue(error.message.orEmpty().contains("explicitly continue"))
    }

    @Test
    fun explicitAcknowledgementAllowsVerifiedLocalBackupFallback() {
        assertFalse(
            requireCloudBackupOrAcknowledgement(
                uploadSucceeded = false,
                allowWithoutCloudBackup = true,
                failureReason = "network is offline",
            )
        )
    }
}
