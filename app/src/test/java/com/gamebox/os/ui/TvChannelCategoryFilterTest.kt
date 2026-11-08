package com.gamebox.os.ui

import com.gamebox.os.tvmode.TvChannel
import com.gamebox.os.tvmode.tvChannelKey
import org.junit.Assert.assertEquals
import org.junit.Test

class TvChannelCategoryFilterTest {
    private val channels = listOf(
        TvChannel("RTS 1", "https://example.test/rts1", group = "National"),
        TvChannel("Pink", "https://example.test/pink", group = "Entertainment"),
        TvChannel("RTS 2", "https://example.test/rts2", group = " National "),
    )

    @Test
    fun categoryFilterCombinesWithSearchAndFavorites() {
        assertEquals(
            listOf("RTS 1"),
            filterTvChannelCategory(channels, "rts", true, listOf(tvChannelKey(channels[0])), "National").map(TvChannel::name),
        )
    }

    @Test
    fun allCategoriesKeepsChannelsWithoutGroupsAndSearchesNames() {
        val ungrouped = TvChannel("News", "https://example.test/news")
        assertEquals(
            listOf("News"),
            filterTvChannelCategory(channels + ungrouped, "news", false, emptyList(), null).map(TvChannel::name),
        )
    }
}
