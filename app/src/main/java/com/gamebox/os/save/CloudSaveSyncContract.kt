package com.gamebox.os.save

import java.net.URI

enum class CloudSaveSyncState { READY, OFFLINE, AUTH_REQUIRED, INVALID_ENDPOINT, PAYLOAD_TOO_LARGE }

data class CloudSaveSyncRequest(val gameId: String, val endpoint: URI, val payloadBytes: Long, val credentialKey: String)

data class CloudSaveSyncValidation(val state: CloudSaveSyncState, val message: String)

object CloudSaveSyncContract {
    const val MAX_PAYLOAD_BYTES: Long = 16L * 1024L * 1024L

    fun validate(request: CloudSaveSyncRequest, networkAvailable: Boolean, credentialsAvailable: Boolean): CloudSaveSyncValidation {
        if (request.gameId.isBlank() || request.credentialKey.isBlank() || request.payloadBytes < 0) return CloudSaveSyncValidation(CloudSaveSyncState.INVALID_ENDPOINT, "Save sync metadata is invalid")
        if (request.endpoint.scheme.lowercase() != "https" || request.endpoint.userInfo != null || request.endpoint.query != null || request.endpoint.fragment != null) return CloudSaveSyncValidation(CloudSaveSyncState.INVALID_ENDPOINT, "Cloud save endpoint must be HTTPS without embedded credentials")
        if (request.payloadBytes > MAX_PAYLOAD_BYTES) return CloudSaveSyncValidation(CloudSaveSyncState.PAYLOAD_TOO_LARGE, "Save payload exceeds the 16 MiB limit")
        if (!networkAvailable) return CloudSaveSyncValidation(CloudSaveSyncState.OFFLINE, "Cloud sync will retry when network is available")
        if (!credentialsAvailable) return CloudSaveSyncValidation(CloudSaveSyncState.AUTH_REQUIRED, "Cloud credentials are required")
        return CloudSaveSyncValidation(CloudSaveSyncState.READY, "Cloud save sync is ready")
    }
}
