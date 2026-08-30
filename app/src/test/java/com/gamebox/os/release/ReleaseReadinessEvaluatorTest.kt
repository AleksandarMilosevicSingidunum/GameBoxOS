package com.gamebox.os.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseReadinessEvaluatorTest {
    @Test fun alphaRequiresRollbackButAllowsUnsignedBuilds() {
        val result = ReleaseReadinessEvaluator.evaluate(ReleaseChannel.ALPHA, signed = false, rollbackPlan = false, updateMetadata = false)
        assertTrue(!result.allowed); assertEquals(listOf("rollback procedure is required"), result.blockers)
    }
    @Test fun signedManifestRequiresRollbackTag() {
        val manifest = ReleaseArtifactManifest(
            artifactName = "gamebox.apk",
            releaseTag = "v1.0.0",
            channel = ReleaseChannel.PRODUCTION,
            sha256 = "a".repeat(64),
            sizeBytes = 1,
            rollbackReleaseTag = null,
        )

        val result = ReleaseReadinessEvaluator.validateManifest(manifest, "v1.0.0")

        assertTrue(!result.allowed)
        assertTrue(result.blockers.contains("rollback release tag is required for signed release channels"))
    }

    @Test fun productionRequiresAllGates() {
        val result = ReleaseReadinessEvaluator.evaluate(ReleaseChannel.PRODUCTION, signed = true, rollbackPlan = true, updateMetadata = true)
        assertTrue(result.allowed); assertEquals(emptyList<String>(), result.blockers)
    }
}
