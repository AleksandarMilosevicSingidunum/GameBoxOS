package com.gamebox.os

import com.gamebox.os.settings.OfflineAction
import com.gamebox.os.settings.OfflineOperationPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineOperationPolicyTest {
    @Test
    fun uncachedNetworkDataRequiresNetworkOffline() {
        val policy = OfflineOperationPolicy { true }
        assertEquals(OfflineAction.REQUIRE_NETWORK, policy.actionFor(true, false))
    }

    @Test
    fun localOnlyOperationRemainsAvailableOffline() {
        val policy = OfflineOperationPolicy { true }
        assertEquals(OfflineAction.OPEN_LOCAL_LIBRARY, policy.actionFor(false, false))
    }

    @Test
    fun onlineNetworkOperationRequiresNetworkInsteadOfCache() {
        val policy = OfflineOperationPolicy { false }
        assertEquals(OfflineAction.REQUIRE_NETWORK, policy.actionFor(true, true))
    }

    @Test
    fun cachedNetworkDataIsUsedOffline() {
        val policy = OfflineOperationPolicy { true }
        assertEquals(OfflineAction.USE_CACHED_CATALOG, policy.actionFor(true, true))
    }
}