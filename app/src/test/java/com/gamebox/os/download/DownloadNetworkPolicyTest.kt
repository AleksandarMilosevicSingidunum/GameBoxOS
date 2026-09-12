package com.gamebox.os.download

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadNetworkPolicyTest {
    @Test fun unmeteredPolicyMapsToWorkManagerConstraint() {
        assertEquals(NetworkType.UNMETERED, requiredDownloadNetworkType(true))
    }

    @Test fun optInMeteredPolicyAllowsAnyConnectedNetwork() {
        assertEquals(NetworkType.CONNECTED, requiredDownloadNetworkType(false))
    }
}
