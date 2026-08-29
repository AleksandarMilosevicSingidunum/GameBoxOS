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

    fun channelForTag(tag: String): ReleaseChannel = when {
        tag.endsWith("-alpha") || tag.contains("-alpha.") -> ReleaseChannel.ALPHA
        tag.endsWith("-beta") || tag.contains("-beta.") -> ReleaseChannel.BETA
        tag.matches(Regex("^v[0-9]+\\.[0-9]+\\.[0-9]+$")) -> ReleaseChannel.PRODUCTION
        else -> throw IllegalArgumentException("unsupported release tag: $tag")
    }

    fun validateManifest(manifest: ReleaseArtifactManifest, expectedTag: String): ReleaseReadiness {
        val blockers = buildList {
            if (manifest.releaseTag != expectedTag) add("manifest tag does not match release tag")
            runCatching { channelForTag(expectedTag) }
                .onSuccess { expectedChannel -> if (manifest.channel != expectedChannel) add("manifest channel does not match release tag") }
                .onFailure { add("release tag is not supported") }
            if (manifest.rollbackReleaseTag == manifest.releaseTag) add("rollback tag must differ from release tag")
        }
        return ReleaseReadiness(blockers.isEmpty(), blockers)
    }
}
