package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TvSafeAreaPolicyTest {
    @Test fun wideLivingRoomViewportUsesRequestedInset() {
        assertEquals(0.04f, effectiveTvSafeAreaPercent(1920f, 1080f, 0.04f), 0.0001f)
        assertEquals(0.08f, effectiveTvSafeAreaPercent(1280f, 720f, 0.08f), 0.0001f)
    }

    @Test fun phoneAndNarrowWindowNeverReceiveTvOverscanInset() {
        assertEquals(0f, effectiveTvSafeAreaPercent(412f, 915f, 0.1f), 0.0001f)
        assertEquals(0f, effectiveTvSafeAreaPercent(1000f, 800f, 0.1f), 0.0001f)
        assertEquals(0f, effectiveTvSafeAreaPercent(899f, 500f, 0.1f), 0.0001f)
    }

    @Test fun insetIsBoundedAndInvalidDimensionsFailClosed() {
        assertEquals(0f, effectiveTvSafeAreaPercent(1920f, 1080f, -1f), 0.0001f)
        assertEquals(0.1f, effectiveTvSafeAreaPercent(1920f, 1080f, 1f), 0.0001f)
        assertEquals(0f, effectiveTvSafeAreaPercent(Float.NaN, 1080f, 0.04f), 0.0001f)
        assertEquals(0f, effectiveTvSafeAreaPercent(1920f, 0f, 0.04f), 0.0001f)
    }
}
