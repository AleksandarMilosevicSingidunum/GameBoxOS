package com.gamebox.os.release

data class ReleaseReadiness(val allowed: Boolean, val blockers: List<String>)

object ReleaseReadinessEvaluator {
    fun evaluate(channel: ReleaseChannel, signed: Boolean, rollbackPlan: Boolean, updateMetadata: Boolean): ReleaseReadiness {
        val blockers = buildList {
            if (!channel.allowsUnsignedBuilds && !signed) add("release signing is not configured")
            if (!rollbackPlan) add("rollback procedure is required")
            if (channel == ReleaseChannel.PRODUCTION && !updateMetadata) add("update metadata is required")
        }
        return ReleaseReadiness(blockers.isEmpty(), blockers)
    }
}
