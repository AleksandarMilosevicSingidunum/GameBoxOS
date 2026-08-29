package com.gamebox.os

import com.gamebox.os.settings.OfflineAction
import com.gamebox.os.settings.OfflineOperationPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineOperationPolicyTest {
    @Test
    fun cachedNetworkDataIsUsedOffline() {
        val policy = OfflineOperationPolicy { true }
        assertEquals(OfflineAction.USE_CACHED_CATALOG, policy.actionFor(true, true))
    }
}