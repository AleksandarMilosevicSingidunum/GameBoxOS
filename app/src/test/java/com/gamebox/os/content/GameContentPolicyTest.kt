package com.gamebox.os.content

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GameContentPolicyTest {
    @Test
    fun preservesAllowlistedPlatformExtensionsAndMimeTypes() {
        val psp = GameContentPolicy.describe(
            gameId = "ridge-racer",
            platform = "PSP",
            sourceUrl = "https://cdn.example/games/ridge-racer.CSO?token=ignored",
        )
        val wii = GameContentPolicy.describe(
            gameId = "wii-sports",
            platform = "Wii",
            sourceUrl = "https://cdn.example/games/wii-sports.wbfs",
        )

        assertEquals("remote/ridge-racer/content.cso", psp.relativePath)
        assertEquals("application/x-compressed-iso", psp.mimeType)
        assertEquals("remote/wii-sports/content.wbfs", wii.relativePath)
        assertEquals("application/x-wbfs", wii.mimeType)
    }

    @Test
    fun storesWiiWareDownloadsAsWadByDefaultAndWhenTheUrlHasNoExtension() {
        val fromUrl = GameContentPolicy.describe("wiiware-demo", "WiiWare", "https://cdn.example/download?id=123")
        val default = GameContentPolicy.describe("another-wiiware-demo", "WiiWare", null)

        assertEquals("remote/wiiware-demo/content.wad", fromUrl.relativePath)
        assertEquals("application/x-wii-wad", fromUrl.mimeType)
        assertEquals("remote/another-wiiware-demo/content.wad", default.relativePath)
    }

    @Test
    fun substitutesSafePlatformDefaultForUnknownOrHostileSuffixes() {
        val psp = GameContentPolicy.describe(
            gameId = "portable-game",
            platform = "PSP",
            sourceUrl = "https://cdn.example/game.exe",
        )
        val unknown = GameContentPolicy.describe(
            gameId = "unknown-game",
            platform = "Future Console",
            sourceUrl = "https://cdn.example/game.rom",
        )

        assertEquals("remote/portable-game/content.iso", psp.relativePath)
        assertEquals("application/x-iso9660-image", psp.mimeType)
        assertEquals("remote/unknown-game/content.bin", unknown.relativePath)
        assertEquals("application/octet-stream", unknown.mimeType)
    }

    @Test
    fun supportsModernDiscAndCartridgeFormatsForAdditionalPlatforms() {
        val ps1 = GameContentPolicy.describe("wipeout", "PS1", "https://cdn.example/wipeout.pbp")
        val n64 = GameContentPolicy.describe("fzero-x", "N64", "https://cdn.example/fzero-x.v64")
        val dreamcast = GameContentPolicy.describe("revolt", "Dreamcast", "https://cdn.example/revolt.gdi")

        assertEquals("application/x-playstation-pbp", ps1.mimeType)
        assertEquals("application/x-n64-rom", n64.mimeType)
        assertEquals("application/x-dreamcast-gdi", dreamcast.mimeType)
        assertEquals("remote/revolt/content.gdi", dreamcast.relativePath)
    }

    @Test
    fun supportsSwitchAndNintendo3dsContentDescriptors() {
        val switch = GameContentPolicy.describe(
            "super-mario-odyssey",
            "Nintendo Switch",
            "https://cdn.example/legal-copy.xci",
        )
        val threeDs = GameContentPolicy.describe(
            "portable-game",
            "Nintendo 3DS",
            "https://cdn.example/legal-copy.cia",
        )
        val playStation2 = GameContentPolicy.describe(
            "racing-game",
            "Sony PlayStation 2",
            "https://cdn.example/legal-copy.chd",
        )

        assertEquals("remote/super-mario-odyssey/content.xci", switch.relativePath)
        assertEquals("application/x-nintendo-switch-xci", switch.mimeType)
        assertEquals("remote/portable-game/content.cia", threeDs.relativePath)
        assertEquals("application/x-nintendo-3ds-cia", threeDs.mimeType)
        assertEquals("remote/racing-game/content.chd", playStation2.relativePath)
        assertEquals("application/x-chd", playStation2.mimeType)
    }

    @Test
    fun legalHomebrewConsoleProfilesKeepTheirNativeContentExtensions() {
        assertEquals("remote/demo/content.nds", GameContentPolicy.describe("demo", "Nintendo DS", "https://cdn.example/demo.nds").relativePath)
        assertEquals("remote/demo/content.gba", GameContentPolicy.describe("demo", "Game Boy Advance", "https://cdn.example/demo.gba").relativePath)
        assertEquals("remote/demo/content.sfc", GameContentPolicy.describe("demo", "SNES", "https://cdn.example/demo.sfc").relativePath)
        assertEquals("remote/demo/content.md", GameContentPolicy.describe("demo", "Mega Drive", "https://cdn.example/demo.md").relativePath)
        assertEquals("remote/demo/content.pce", GameContentPolicy.describe("demo", "PC Engine", "https://cdn.example/demo.pce").relativePath)
        assertEquals("remote/demo/content.ws", GameContentPolicy.describe("demo", "WonderSwan", "https://cdn.example/demo.ws").relativePath)
        assertEquals("remote/demo/content.a26", GameContentPolicy.describe("demo", "Atari 2600", "https://cdn.example/demo.a26").relativePath)
    }

    @Test
    fun everyStandaloneEmulatorPlatformGetsANativeRemoteContentExtension() {
        val extensions = mapOf(
            "NES" to "nes",
            "Famicom" to "nes",
            "Nintendo Entertainment System" to "nes",
            "GB" to "gb",
            "Nintendo Game Boy" to "gb",
            "Game Boy Color" to "gbc",
            "Nintendo Game Boy Color" to "gbc",
            "PS1" to "chd",
            "Sony PlayStation" to "chd",
            "N64" to "z64",
            "Dreamcast" to "chd",
            "Sega Dreamcast" to "chd",
            "PSP" to "iso",
            "Sony PlayStation Portable" to "iso",
            "PS2" to "iso",
            "Sony PlayStation 2" to "iso",
            "GameCube" to "rvz",
            "Nintendo GameCube" to "rvz",
            "Wii" to "rvz",
            "WiiWare" to "wad",
            "3DS" to "3ds",
            "SNES" to "sfc",
            "Super Nintendo Entertainment System" to "sfc",
            "Game Boy Advance" to "gba",
            "Nintendo Game Boy Advance" to "gba",
            "DS" to "nds",
            "DSi" to "nds",
            "Genesis" to "md",
            "Sega Genesis" to "md",
            "Master System" to "sms",
            "Sega Master System" to "sms",
            "Game Gear" to "gg",
            "Sega Game Gear" to "gg",
            "Atari 2600" to "a26",
        )
        val registry = EmulatorCapabilityRegistry()

        extensions.forEach { (platform, expectedExtension) ->
            val game = Game(
                id = GameId("content-${platform.lowercase().filter(Char::isLetterOrDigit)}"),
                title = "Content policy check",
                platform = platform,
                year = 2000,
                genre = "Test",
                sizeMb = 1,
                state = InstallState.INSTALLED,
                sourceUrl = "https://cdn.example/content.$expectedExtension",
                expectedSha256 = "a".repeat(64),
            )

            assertEquals("$platform should have an approved emulator", true, registry.hasApprovedAdapter(game))
            val content = GameContentPolicy.describe(game.id.value, platform, game.sourceUrl)
            val capability = requireNotNull(registry.forGame(game))
            assertEquals("$platform should not be stored as generic content.bin", expectedExtension, content.fileExtension)
            assertEquals("Downloader and launcher must agree on $platform path", content.relativePath, capability.contentRelativePath)
            assertEquals("Downloader and launcher must agree on $platform MIME type", content.mimeType, capability.mimeType)
        }
    }

    @Test
    fun rejectsUnsafeGameIdsBeforeBuildingStoragePath() {
        assertThrows(IllegalArgumentException::class.java) {
            GameContentPolicy.describe("../outside", "PSP", "https://cdn.example/game.iso")
        }
    }

    @Test
    fun registryUsesTheSameDescriptorAsTheDownloader() {
        val game = Game(
            id = GameId("metroid-prime"),
            title = "Metroid Prime",
            platform = "GameCube",
            year = 2002,
            genre = "Action",
            sizeMb = 1400,
            state = InstallState.INSTALLED,
            sourceUrl = "https://cdn.example/metroid-prime.rvz",
            expectedSha256 = "a".repeat(64),
            emulatorPackage = "org.dolphinemu.dolphinemu",
        )

        val capability = requireNotNull(EmulatorCapabilityRegistry().forGame(game))

        assertEquals("remote/metroid-prime/content.rvz", capability.contentRelativePath)
        assertEquals("application/x-dolphin-rvz", capability.mimeType)
        assertEquals("org.dolphinemu.dolphinemu", capability.packageName)
    }
}

