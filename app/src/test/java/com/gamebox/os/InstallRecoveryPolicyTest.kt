package com.gamebox.os

import com.gamebox.os.download.InstallRecoveryAction
import com.gamebox.os.download.InstallRecoveryPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallRecoveryPolicyTest {
    @Test
    fun defaultsProtectVerifiedContent() {
        val policy = InstallRecoveryPolicy()
        assertEquals(InstallRecoveryAction.CLEAN_PARTIAL, policy.onVerificationFailure)
        assertEquals(InstallRecoveryAction.RETRY, policy.onProcessDeath)
    }
}