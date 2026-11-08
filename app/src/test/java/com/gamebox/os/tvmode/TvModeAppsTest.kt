package com.gamebox.os.tvmode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class TvModeAppsTest {
    @Test
    fun displayChannelNameRemovesUnverifiedTrailingQualityAndResolution() {
        assertEquals("Pink", displayTvChannelName("Pink (720p)"))
        assertEquals("RTS 1", displayTvChannelName("RTS 1 1080p"))
        assertEquals("Arena Sport", displayTvChannelName("Arena Sport HD"))
        assertEquals("RTS 2", displayTvChannelName("RTS 2 (SD)"))
        assertEquals("News", displayTvChannelName("News - FHD"))
        assertEquals("Movie", displayTvChannelName("Movie UHD"))
        assertEquals("Cinema", displayTvChannelName("Cinema | 3840×2160"))
        assertEquals("Channel", displayTvChannelName("Channel 4K"))
    }

    @Test
    fun displayChannelNameKeepsNamesWithoutQualitySuffix() {
        assertEquals("RTS 1", displayTvChannelName("RTS 1"))
        assertEquals("HD Radio", displayTvChannelName("HD Radio"))
    }

    @Test
    fun streamFailureAdvancesOnlyToAnAvailableAlternate() {
        assertEquals(1, nextTvStreamIndex(0, 2))
        assertEquals(null, nextTvStreamIndex(1, 2))
        assertEquals(null, nextTvStreamIndex(0, 0))
    }

    @Test
    fun retryStartsAtPrimaryStreamWhenStreamsExist() {
        assertEquals(0, retryTvStreamIndex(3))
        assertEquals(0, retryTvStreamIndex(1))
        assertEquals(null, retryTvStreamIndex(0))
    }

    @Test
    fun playbackFailuresExplainCleartextDnsAndConnectionErrorsWithoutLeakingUrls() {
        assertEquals(
            "This channel uses unencrypted HTTP, which Android blocks for security. Choose an HTTPS stream.",
            tvPlaybackErrorMessage("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", java.net.UnknownServiceException("CLEARTEXT communication blocked")),
        )
        assertEquals(
            "The stream server could not be found. Check the network or choose another channel.",
            tvPlaybackErrorMessage("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", java.net.UnknownHostException("private-token.example")),
        )
        assertEquals(
            "Could not connect to this stream. It may be offline or unavailable on this network.",
            tvPlaybackErrorMessage("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", java.net.SocketTimeoutException("private URL")),
        )
    }

    @Test
    fun supportedStreamingAppsOpenTheirOfficialWebVersionsInGameBox() {
        assertEquals("https://www.netflix.com/", inAppTvWebDestination(TvLaunchableApp("com.netflix.mediaclient", "Netflix"))?.url)
        assertEquals("https://www.primevideo.com/", inAppTvWebDestination(TvLaunchableApp("com.amazon.avod", "Prime Video"))?.url)
        assertEquals("https://www.youtube.com/tv", inAppTvWebDestination(TvLaunchableApp("com.google.android.youtube.tv", "YouTube"))?.url)
        assertEquals("https://sr.eon.tv/", inAppTvWebDestination(TvLaunchableApp("rs.sbb.eon", "EON TV"))?.url)
        assertEquals(null, inAppTvWebDestination(TvLaunchableApp("com.example.game", "Example Game")))
    }

    @Test
    fun rtsChannelsHaveOfficialPlanetaLiveFallbacks() {
        assertEquals("https://rtsplaneta.rs/live/tv/5/rts-1", officialRtsPlanetaLiveUrl(TvChannel("RTS 1", "https://example.invalid", guideId = "RTS1.rs@SD")))
        assertEquals("https://rtsplaneta.rs/live/tv/4/rts-2", officialRtsPlanetaLiveUrl(TvChannel("RTS 2", "https://example.invalid")))
        assertEquals(null, officialRtsPlanetaLiveUrl(TvChannel("Pink", "https://example.invalid")))
    }

    @Test
    fun liveBadgeRequiresAProgrammeThatIsOnAirNow() {
        assertEquals(true, hasCurrentTvProgramme("Now · News"))
        assertEquals(false, hasCurrentTvProgramme("Up next · News"))
        assertEquals(false, hasCurrentTvProgramme(null))
    }

    @Test
    fun m3uChannelLogoMustBeAnHttpsUrlWithoutCredentials() {
        val playlist = """#EXTM3U
#EXTINF:-1 tvg-id="news" tvg-logo="https://logos.example/news.png",News
https://media.example/news.m3u8
#EXTINF:-1 tvg-logo="http://logos.example/unsafe.png",Unsafe logo
https://media.example/unsafe.m3u8
"""

        assertEquals("https://logos.example/news.png", parseAuthorizedM3u(playlist).first().logoUrl)
        assertEquals(null, parseAuthorizedM3u(playlist).last().logoUrl)
    }

    @Test
    fun channelIdentityDoesNotPersistStreamCredentialsOrUrl() {
        assertEquals("rts1.rs", tvChannelKey(TvChannel("RTS 1 HD", "https://secret.example/live?token=private", guideId = "RTS1.rs@HD")))
        assertEquals("pink", tvChannelKey(TvChannel("Pink 720p", "https://example.invalid/live")))
        assertEquals(tvChannelKey(TvChannel("Pink", "https://one.example/live")), tvChannelKey(TvChannel("Pink (1080p)", "https://two.example/live")))
        assertEquals(tvChannelKey(TvChannel("РТС 1", "https://one.example/live")), tvChannelKey(TvChannel("РТС 1 HD", "https://two.example/live")))
    }

    @Test
    fun programmeLookupReturnsTheOnAirProgrammeAndTheNextOne() {
        val now = java.time.Instant.parse("2026-11-03T12:30:00Z")
        val channel = TvChannel("RTS 1", "https://example.invalid/live", guideId = "rts1.rs@sd")
        val programmes = listOf(
            TvProgramme("rts1.rs", now.minusSeconds(1800), now.plusSeconds(1800), "News"),
            TvProgramme("rts1.rs", now.plusSeconds(1800), now.plusSeconds(5400), "Film"),
        )

        val (current, next) = currentAndNextProgrammes(channel, programmes, now)

        assertEquals("News", current?.title)
        assertEquals("Film", next?.title)
    }

    @Test
    fun favoritesAreShownFirstAndMissingPackagesAreIgnored() {
        val apps = listOf(TvLaunchableApp("b.app", "Beta"), TvLaunchableApp("a.app", "Alpha"))

        assertEquals(listOf("b.app", "a.app"), orderTvApps(apps, listOf("missing.app", "b.app")).map { it.packageName })
    }

    @Test
    fun favoriteCanMoveWithinItsBounds() {
        val favorites = listOf("a.app", "b.app", "c.app")

        assertEquals(listOf("b.app", "a.app", "c.app"), moveTvFavorite(favorites, "a.app", 1))
        assertEquals(favorites, moveTvFavorite(favorites, "a.app", -1))
        assertEquals(favorites, moveTvFavorite(favorites, "missing.app", 1))
    }

    @Test
    fun serbianChannelsFollowPublishedAudienceRankAndKeepPlaylistOrderForUnrankedChannels() {
        val channels = listOf(
            "Belle Amie", "Prva Files", "RTS 2", "Happy", "B92", "Prva Kick", "Prva", "Pink (720p)", "RTS 1", "AXN Adria",
        )
            .map { TvChannel(it, "https://example.invalid/${it.hashCode()}") }

        assertEquals(
            listOf("RTS 1", "Pink (720p)", "Prva", "Happy", "B92", "RTS 2", "Belle Amie", "Prva Files", "Prva Kick", "AXN Adria"),
            sortSerbianChannelsByPopularity(channels).map(TvChannel::name),
        )
    }

    @Test
    fun customPlaylistKeepsItsOriginalChannelOrder() {
        val channels = listOf(
            TvChannel("RTS 1", "https://example.invalid/rts1"),
            TvChannel("Pink", "https://example.invalid/pink"),
        )

        assertEquals(
            channels,
            orderTvChannelsForPlaylist("content://playlists/custom.m3u", channels),
        )
    }

    @Test
    fun releasesOnlyTheReplacedContentPlaylistGrant() {
        val previous = "content://documents/playlist.m3u"

        assertEquals(previous, playlistGrantToRelease(previous, "https://iptv-org.github.io/iptv/countries/rs.m3u"))
        assertEquals(previous, playlistGrantToRelease(previous, "content://documents/other.m3u"))
        assertEquals(null, playlistGrantToRelease(previous, previous))
        assertEquals(null, playlistGrantToRelease("https://iptv-org.github.io/iptv/countries/rs.m3u", previous))
        assertEquals(null, playlistGrantToRelease(null, previous))
    }

    @Test
    fun failedOrEmptyPlaylistReplacementKeepsTheActivePlaylist() {
        val active = "content://documents/working.m3u"
        val pending = "content://documents/replacement.m3u"

        assertEquals(active, playlistUriAfterLoad(active, pending, pending, hasChannels = false))
        assertEquals(active, playlistUriAfterLoad(active, pending, "https://other.example/list.m3u", hasChannels = true))
        assertEquals(pending, playlistUriAfterLoad(active, pending, pending, hasChannels = true))
    }

    @Test
    fun m3uOnlyReturnsNamedNetworkChannelsWithoutEmbeddedCredentials() {
        val playlist = """#EXTM3U
#EXTINF:-1 tvg-id="news" group-title="News",News Channel
https://media.example/live/news.m3u8
#EXTINF:-1,Unsafe
https://user:password@media.example/live.m3u8
#EXTINF:-1,Unsupported
file:///private/recording.ts
#EXTINF:-1,Camera
rtsp://camera.example/live
"""

        assertEquals(
            listOf(TvChannel("News Channel", "https://media.example/live/news.m3u8", "News", "news"), TvChannel("Camera", "rtsp://camera.example/live")),
            parseAuthorizedM3u(playlist),
        )
    }

    @Test
    fun m3uPreservesSafeHeadersForPrimaryAndAlternateStreams() {
        val playlist = """#EXTM3U
#EXTINF:-1 tvg-id="news",News
#EXTVLCOPT:http-user-agent=GameBox%2F1
#EXTVLCOPT:http-referrer=https%3A%2F%2Fportal.example%2Fwatch
https://media.example/news.m3u8|Origin=https%3A%2F%2Fportal.example&Cookie=ignored&Referer=https%3A%2F%2Fportal.example%2Fwatch
#EXTINF:-1 tvg-id="news",News HD
https://cdn.example/news.m3u8|User-Agent=Alternate%2F2&Authorization=ignored
#EXTINF:-1 tvg-id="news",News Portal
https://media.example/news.m3u8|User-Agent=Portal%2F3
"""

        val channel = parseAuthorizedM3u(playlist).single()

        assertEquals(
            mapOf("User-Agent" to "GameBox/1", "Referer" to "https://portal.example/watch", "Origin" to "https://portal.example"),
            channel.streams.first().requestHeaders,
        )
        assertEquals(
            listOf(
                TvStream("https://media.example/news.m3u8", channel.streams.first().requestHeaders),
                TvStream("https://cdn.example/news.m3u8", mapOf("User-Agent" to "Alternate/2")),
                TvStream("https://media.example/news.m3u8", mapOf("User-Agent" to "Portal/3")),
            ),
            channel.streams,
        )
    }

    @Test
    fun m3uGroupsGuideQualityVariantsAsFallbackStreams() {
        val playlist = """#EXTM3U
#EXTINF:-1 tvg-id="rts1.rs@SD",RTS 1 SD
#EXTVLCOPT:http-user-agent=GameBox%2F1
https://sd.example/live.m3u8
#EXTINF:-1 tvg-id="rts1.rs@HD",RTS 1 HD
https://hd.example/live.m3u8|Referer=https%3A%2F%2Fportal.example%2F
"""

        val channel = parseAuthorizedM3u(playlist).single()

        assertEquals("RTS 1 SD", channel.name)
        assertEquals("rts1.rs@SD", channel.guideId)
        assertEquals(
            listOf(
                TvStream("https://sd.example/live.m3u8", mapOf("User-Agent" to "GameBox/1")),
                TvStream("https://hd.example/live.m3u8", mapOf("Referer" to "https://portal.example/")),
            ),
            channel.streams,
        )
    }

    @Test
    fun m3uKeepsCommasInQuotedAttributesAndChannelNames() {
        val playlist = """#EXTM3U
#EXTINF:-1 group-title="News, local",News, Belgrade
https://media.example/live/news.m3u8
"""

        assertEquals(
            listOf(TvChannel("News, Belgrade", "https://media.example/live/news.m3u8", "News, local")),
            parseAuthorizedM3u(playlist),
        )
    }

    @Test
    fun readsAndParsesUtf8LocalM3uContent() {
        val playlist = """#EXTM3U
#EXTINF:-1 tvg-id="fixture.demo",GameBox Test Channel
https://example.invalid/live.m3u8
"""

        val text = readPlaylist(ByteArrayInputStream(playlist.toByteArray(Charsets.UTF_8)))

        assertEquals(
            listOf(TvChannel("GameBox Test Channel", "https://example.invalid/live.m3u8", guideId = "fixture.demo")),
            parseAuthorizedM3u(text),
        )
    }

    @Test
    fun readsM3uWithUtf8AndUtf16ByteOrderMarks() {
        val playlist = """#EXTM3U
#EXTINF:-1 tvg-id="fixture.demo",GameBox Test Channel
https://example.invalid/live.m3u8
"""
        val expected = listOf(TvChannel("GameBox Test Channel", "https://example.invalid/live.m3u8", guideId = "fixture.demo"))
        val cases = listOf(
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + playlist.toByteArray(Charsets.UTF_8),
            byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + playlist.toByteArray(Charsets.UTF_16LE),
            byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + playlist.toByteArray(Charsets.UTF_16BE),
        )

        cases.forEach { bytes ->
            assertEquals(expected, parseAuthorizedM3u(readPlaylist(ByteArrayInputStream(bytes))))
        }
    }

    @Test
    fun playlistDeduplicatesByGuideIdThenNormalizedChannelName() {
        val playlist = """#EXTM3U
#EXTINF:-1 tvg-id="news" group-title="HD",News HD
https://one.example/news.m3u8
#EXTINF:-1 tvg-id="news" group-title="SD",News SD
https://two.example/news.m3u8
#EXTINF:-1,Local News (720p)
https://one.example/local.m3u8
#EXTINF:-1,  local   news (1080p)${"  "}
https://two.example/local.m3u8
#EXTINF:-1,News SD
https://two.example/news-sd.m3u8
"""

        val channels = parseAuthorizedM3u(playlist)
        assertEquals(listOf("News HD", "Local News (720p)"), channels.map { it.name })
        assertEquals(listOf("https://one.example/news.m3u8", "https://two.example/news.m3u8", "https://two.example/news-sd.m3u8"), channels.first().streamUrls)
        assertEquals(listOf("https://one.example/local.m3u8", "https://two.example/local.m3u8"), channels.last().streamUrls)
    }

    @Test
    fun xmltvGuideUrlAndProgrammeAreReadWithoutResolvingDoctype() {
        assertEquals("https://guide.example/guide.xml", playlistGuideUrl("#EXTM3U url-tvg=\"https://guide.example/guide.xml\"\n"))
        assertEquals(null, playlistGuideUrl("#EXTM3U url-tvg=\"http://guide.example/guide.xml\"\n"))
        val xml = """<tv><programme channel="news" start="20261030000000 +0000" stop="20261030010000 +0000"><title>Evening News</title></programme></tv>"""

        assertEquals("Evening News", parseXmltvProgrammes(xml).single().title)
        org.junit.Assert.assertThrows(Exception::class.java) {
            parseXmltvProgrammes("<!DOCTYPE tv [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><tv/>")
        }
    }

    @Test
    fun iptvOrgCountryPlaylistUsesFreeEpgCountryFeed() {
        assertEquals("rs", iptvOrgPlaylistCountryCode("https://iptv-org.github.io/iptv/countries/RS.m3u"))
        assertEquals("de", iptvOrgPlaylistCountryCode("https://iptv-org.github.io/iptv/countries/de.m3u"))
        assertEquals(null, iptvOrgPlaylistCountryCode("https://example.org/iptv/countries/rs.m3u"))
        assertEquals("https://free-epg.de/api/epg/rs.xml.gz", freeEpgCountryGuideUrl("RS"))
    }

    @Test
    fun currentXmltvProgramsMatchPlaylistFeedIdsAndKeepUpNext() {
        val now = java.time.Instant.parse("2026-10-30T12:30:00Z")
        val xml = """<tv>
            <programme channel="ArenaSport1Premium.rs" start="20261030120000 +0000" stop="20261030130000 +0000"><title>Live match</title></programme>
            <programme channel="ArenaSport1Premium.rs" start="20261030130000 +0000" stop="20261030140000 +0000"><title>Post match</title></programme>
            <programme channel="Other.rs" start="20261030120000 +0000" stop="20261030130000 +0000"><title>Other</title></programme>
        </tv>"""

        assertEquals(
            listOf("Live match", "Post match"),
            parseCurrentXmltvProgrammes(xml.byteInputStream(), mapOf("ArenaSport1Premium.rs@SD" to "Arena Sport 1 Premium"), now).map { it.title },
        )
    }

    @Test
    fun currentXmltvGuideRetainsAllProgramsInIts24HourWindow() {
        val now = java.time.Instant.parse("2026-10-30T12:30:00Z")
        val xml = """<tv>
            <programme channel="news" start="20261030120000 +0000" stop="20261030130000 +0000"><title>News</title></programme>
            <programme channel="news" start="20261030130000 +0000" stop="20261030140000 +0000"><title>Weather</title></programme>
            <programme channel="news" start="20261030140000 +0000" stop="20261030150000 +0000"><title>Film</title></programme>
            <programme channel="news" start="20261030150000 +0000" stop="20261030160000 +0000"><title>Late show</title></programme>
        </tv>"""

        val channel = TvChannel("News", "https://example.invalid/live", guideId = "news")
        val programmes = parseCurrentXmltvProgrammes(xml.byteInputStream(), mapOf("news" to "News"), now)

        assertEquals(listOf("News", "Weather", "Film", "Late show"), programmes.map { it.title })
        assertEquals("Late show", currentAndNextProgrammes(channel, programmes, now.plusSeconds(3 * 60 * 60 + 1)).first?.title)
    }

    @Test
    fun currentXmltvGuideBoundsTheNumberOfEntriesPerChannel() {
        val now = java.time.Instant.parse("2026-10-30T00:00:00Z")
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z").withZone(java.time.ZoneOffset.UTC)
        val xml = (0..100).joinToString(prefix = "<tv>", postfix = "</tv>") { index ->
            val start = formatter.format(now.plusSeconds(index * 600L))
            val end = formatter.format(now.plusSeconds((index + 1) * 600L))
            "<programme channel=\"news\" start=\"$start\" stop=\"$end\"><title>Programme $index</title></programme>"
        }

        assertEquals(96, parseCurrentXmltvProgrammes(xml.byteInputStream(), mapOf("news" to "News"), now).size)
    }

    @Test
    fun currentXmltvProgramsMatchEquivalentNamesWhenProviderIdsDiffer() {
        assertEquals(
            normalizedGuideName("ArenaSport1Premium.rs@SD"),
            normalizedGuideName("Arena Sport Premium 1.rs"),
        )
        val now = java.time.Instant.parse("2026-10-30T12:30:00Z")
        val xml = """<tv>
            <channel id="ArenaPremium1.rs"><display-name>Arena Premium 1</display-name></channel>
            <programme channel="ArenaPremium1.rs" start="20261030120000 +0000" stop="20261030130000 +0000"><title>Live match</title></programme>
        </tv>"""

        val programme = parseCurrentXmltvProgrammes(
            xml.byteInputStream(),
            mapOf("ArenaSport1Premium.rs@SD" to "Arena Sport 1 Premium (Serbia) (360p)"),
            now,
        ).single()

        assertEquals("Live match", programme.title)
        assertEquals("ArenaSport1Premium.rs@SD", programme.channelId)
    }

    @Test
    fun xmltvResponseDetectsGzipByMagicBytesInsteadOfFileName() {
        val xml = "<tv/>".toByteArray()
        val compressed = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(xml) }
        }.toByteArray()

        assertEquals("<tv/>", xmltvContentStream(ByteArrayInputStream(xml)).reader().readText())
        assertEquals("<tv/>", xmltvContentStream(ByteArrayInputStream(compressed)).reader().readText())
    }

    @Test
    fun iptvOrgPlaylistUrlOnlyAcceptsCountryPlaylists() {
        assertEquals("https://iptv-org.github.io/iptv/countries/rs.m3u", iptvOrgCountryPlaylistUrl("RS"))
        assertEquals("iptv-org · RS (default)", activeTvPlaylistLabel(null))
        assertEquals("iptv-org · DE", activeTvPlaylistLabel("https://iptv-org.github.io/iptv/countries/de.m3u"))
        assertEquals("Custom playlist", activeTvPlaylistLabel("content://playlists/favorites.m3u"))
        listOf("", "usa", "1a", "..", "https://example.org/x.m3u").forEach { code ->
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { iptvOrgCountryPlaylistUrl(code) }
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            fetchIptvOrgPlaylist("https://example.org/not-a-country.m3u")
        }
    }

    @Test
    fun playlistReaderRejectsInputLargerThanEightMiB() {
        assertEquals("#EXTM3U", readPlaylist(ByteArrayInputStream("#EXTM3U".toByteArray())))
        val oversized = ByteArray(8 * 1024 * 1024 + 1)

        assertThrows(IllegalArgumentException::class.java) {
            readPlaylist(ByteArrayInputStream(oversized))
        }
    }
}
