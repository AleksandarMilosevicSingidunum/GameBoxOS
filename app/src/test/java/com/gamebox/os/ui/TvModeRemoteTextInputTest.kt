package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvModeRemoteTextInputTest {
    @Test
    fun textAndBackspaceEditOnlyTheActiveFieldInArrivalOrder() {
        var query = ""
        listOf("M", "a", "r", "i", "o").forEach { text ->
            query = requireNotNull(
                applyTvRemoteTextInput(
                    query,
                    TvRemoteTextTarget.CHANNEL_SEARCH,
                    TvRemoteTextInput(TvRemoteTextTarget.CHANNEL_SEARCH, text),
                ),
            )
        }
        assertEquals("Mario", query)
        assertEquals(
            "Mari",
            applyTvRemoteTextInput(
                query,
                TvRemoteTextTarget.CHANNEL_SEARCH,
                TvRemoteTextInput(TvRemoteTextTarget.CHANNEL_SEARCH, null),
            ),
        )
        assertNull(
            applyTvRemoteTextInput(
                query,
                TvRemoteTextTarget.COUNTRY_CODE,
                TvRemoteTextInput(TvRemoteTextTarget.CHANNEL_SEARCH, "x"),
            ),
        )
    }

    @Test
    fun countryEntryRemainsTwoLettersUppercaseAndBackspaceRemovesOneCodePoint() {
        assertEquals(
            "RS",
            applyTvRemoteTextInput(
                "R",
                TvRemoteTextTarget.COUNTRY_CODE,
                TvRemoteTextInput(TvRemoteTextTarget.COUNTRY_CODE, "s 1b"),
            ),
        )
        assertEquals(
            "A",
            applyTvRemoteTextInput(
                "A😀",
                TvRemoteTextTarget.APP_SEARCH,
                TvRemoteTextInput(TvRemoteTextTarget.APP_SEARCH, null),
            ),
        )
    }
}
