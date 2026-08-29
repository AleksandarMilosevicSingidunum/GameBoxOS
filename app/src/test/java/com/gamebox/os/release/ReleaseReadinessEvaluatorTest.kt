package com.gamebox.os.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseReadinessEvaluatorTest {
    @Test fun alphaRequiresRollbackButAllowsUnsignedBuilds() {
        val result = ReleaseReadinessEvaluator.evaluate(ReleaseChannel.ALPHA, signed = false, rollbackPlan = false, updateMetadata = false)
        assertTrue(!result.allowed); assertEquals(listOf("rollback procedure is required"), result.blockers)
    }
    @Test fun productionRequiresAllGates() {
        val result = ReleaseReadinessEvaluator.evaluate(ReleaseChannel.PRODUCTION, signed = true, rollbackPlan = true, updateMetadata = true)
        assertTrue(result.allowed); assertEquals(emptyList<String>(), result.blockers)
    }
}
