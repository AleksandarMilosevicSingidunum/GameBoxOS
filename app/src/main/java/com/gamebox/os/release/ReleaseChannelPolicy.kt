package com.gamebox.os.release

enum class ReleaseChannel { DEVELOPMENT, ALPHA, BETA, PRODUCTION }

data class ReleaseReadiness(val allowed: Boolean, val blockers: List<String>)

object ReleaseChannelPolicy {
    fun evaluate(channel: ReleaseChannel, signed: Boolean, rollbackPlan: Boolean, updateMetadata: Boolean): ReleaseReadiness {
        val blockers = buildList {
            if (channel == ReleaseChannel.PRODUCTION && !signed) add("production signing is not configured")
            if (channel != ReleaseChannel.DEVELOPMENT && !rollbackPlan) add("rollback procedure is required")
            if (channel == ReleaseChannel.PRODUCTION && !updateMetadata) add("update metadata is required")
        }
        return ReleaseReadiness(blockers.isEmpty(), blockers)
    }
}
