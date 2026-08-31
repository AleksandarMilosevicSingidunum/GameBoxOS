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

