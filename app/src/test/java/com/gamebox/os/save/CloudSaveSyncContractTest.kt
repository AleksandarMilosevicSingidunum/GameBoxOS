package com.gamebox.os.save

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class CloudSaveSyncContractTest {
    private val request = CloudSaveSyncRequest("game-1", URI("https://cloud.example/saves/game-1"), 128, "cloud-save")
    @Test fun rejectsEmbeddedCredentials() {
        val result = CloudSaveSyncContract.validate(request.copy(endpoint = URI("https://user:pass@cloud.example/save")), true, true)
        assertEquals(CloudSaveSyncState.INVALID_ENDPOINT, result.state)
    }
    @Test fun rejectsHttpsEndpointWithoutHost() {
        val result = CloudSaveSyncContract.validate(
            request.copy(endpoint = URI("https:/saves/game-1")),
            true,
            true,
        )
        assertEquals(CloudSaveSyncState.INVALID_ENDPOINT, result.state)
    }
    @Test fun defersWhenOfflineOrCredentialsMissing() {
        assertEquals(CloudSaveSyncState.OFFLINE, CloudSaveSyncContract.validate(request, false, true).state)
        assertEquals(CloudSaveSyncState.AUTH_REQUIRED, CloudSaveSyncContract.validate(request, true, false).state)
    }
    @Test fun enforcesPayloadLimit() {
        assertEquals(CloudSaveSyncState.PAYLOAD_TOO_LARGE, CloudSaveSyncContract.validate(request.copy(payloadBytes = CloudSaveSyncContract.MAX_PAYLOAD_BYTES + 1), true, true).state)
    }
}
