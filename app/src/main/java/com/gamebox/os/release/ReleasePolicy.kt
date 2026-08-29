package com.gamebox.os.release

enum class ReleaseChannel(val allowsUnsignedBuilds: Boolean) {
    ALPHA(true),
    BETA(false),
    PRODUCTION(false)
}

data class ReleasePolicy(
    val channel: ReleaseChannel,
    val rollbackSupported: Boolean,
    val minimumSupportedVersion: String
)