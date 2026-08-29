package com.gamebox.os

import com.gamebox.os.release.ReleaseChannel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleasePolicyTest {
    @Test
    fun onlyAlphaAllowsUnsignedBuilds() {
        assertTrue(ReleaseChannel.ALPHA.allowsUnsignedBuilds)
        assertFalse(ReleaseChannel.BETA.allowsUnsignedBuilds)
        assertFalse(ReleaseChannel.PRODUCTION.allowsUnsignedBuilds)
    }
}