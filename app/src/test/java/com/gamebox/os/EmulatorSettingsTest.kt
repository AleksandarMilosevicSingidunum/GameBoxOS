package com.gamebox.os

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import com.gamebox.os.launch.EmulatorReadinessPolicy
import com.gamebox.os.launch.emulatorPackageCandidates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmulatorSettingsTest {
    private val registry = EmulatorCapabilityRegistry()

    @Test
    fun platformOptionsProvideApprovedDefaults() {
        val psp = Game(GameId("psp-demo"), "PSP Demo", "PSP", 2020, "Action", 1, InstallState.INSTALLED,
            expectedSha256 = "abc")
        assertEquals("org.ppsspp.ppsspp", registry.forGame(psp)?.packageName)
        assertEquals(
            listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold", "org.ppsspp.ppsspplegacy"),
            registry.optionsFor(psp),
        )
    }

    @Test
    fun configuredPlatformDefaultOverridesBuiltInOrder() {
        val ps1 = Game(
            GameId("ps1-demo"), "PS1 Demo", "PS1", 2020, "Action", 1,
            InstallState.INSTALLED, expectedSha256 = "abc"
        )
        assertEquals(
            "com.github.stenzek.duckstation",
            registry.forGame(ps1, "com.retroarch.aarch64")?.packageName
        )
    }

    @Test
    fun perGameOverrideWinsOverConfiguredPlatformDefault() {
        val ps1 = Game(
            GameId("ps1-override"), "PS1 Override", "PS1", 2020, "Action", 1,
            InstallState.INSTALLED,
            emulatorPackage = "com.github.stenzek.duckstation",
            expectedSha256 = "abc"
        )
        assertEquals(
            "com.github.stenzek.duckstation",
            registry.forGame(ps1, "com.retroarch.aarch64")?.packageName
        )
    }

    @Test
    fun unsupportedConfiguredDefaultFallsBackToApprovedOption() {
        val psp = Game(
            GameId("psp-fallback"), "PSP Fallback", "PSP", 2020, "Action", 1,
            InstallState.INSTALLED, expectedSha256 = "abc"
        )
        assertEquals(
            "org.ppsspp.ppsspp",
            registry.forGame(psp, "com.example.untrusted")?.packageName
        )
    }

    @Test
    fun unknownOverrideFallsBackToStandalonePlatformDefault() {
        val retro = Game(GameId("retro-demo"), "Retro Demo", "Retro", 2020, "Action", 1, InstallState.INSTALLED,
            emulatorPackage = "com.example.untrusted", expectedSha256 = "abc")
        assertNull(registry.forGame(retro))
    }

    @Test
    fun galaxyPatrolUsesStandaloneNesEmuByDefault() {
        val fixture = Game(
            GameId("galaxy-patrol"), "Galaxy Patrol", "Retro", 2018, "Arcade", 1,
            InstallState.INSTALLED,
        )

        val options = registry.optionsFor(fixture)
        assertEquals(listOf("com.explusalpha.NesEmu"), options)
        val installed = options.filter { option ->
            emulatorPackageCandidates(option).any { it == "com.explusalpha.NesEmu" }
        }.toSet()
        val readiness = EmulatorReadinessPolicy.evaluate(
            approvedOptions = options,
            selectedPackage = null,
            installedPackages = installed,
            displayName = registry::displayName,
        )

        assertEquals("com.explusalpha.NesEmu", readiness.selectedPackage)
        assertEquals(listOf("com.explusalpha.NesEmu"), readiness.installedOptions)
        assertEquals(listOf("com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32"),
            emulatorPackageCandidates("com.retroarch.aarch64"))
    }

    @Test
    fun nesGamesUseStandaloneEmulatorWithoutRetroArchCore() {
        val nes = Game(
            GameId("nes-import"), "NES Import", "NES", 2020, "Action", 1,
            InstallState.INSTALLED,
            localContentRelativePath = "nes-import/game.nes",
            localContentSha256 = "a".repeat(64),
            localContentMimeType = "application/x-nes-rom",
        )

        val capability = requireNotNull(registry.forGame(nes))
        assertEquals("com.explusalpha.NesEmu", capability.packageName)
        assertNull(capability.requiredCore)
        assertNull(capability.retroArchCoreFileName)
    }

    @Test
    fun gbcImportsUseStandaloneEmulatorWithoutRetroArchCore() {
        val gbc = Game(
            GameId("gbc-import"), "Brickster", "Game Boy Color", 2020, "Adventure", 1,
            InstallState.INSTALLED,
            localContentRelativePath = "gbc-import/brickster.gbc",
            localContentSha256 = "b".repeat(64),
            localContentMimeType = "application/x-gameboy-color-rom",
        )

        val capability = requireNotNull(registry.forGame(gbc))
        assertEquals("com.explusalpha.GbcEmu", capability.packageName)
        assertNull(capability.requiredCore)
        assertNull(capability.retroArchCoreFileName)
    }

    @Test
    fun supportedVimmConsolesPreferStandaloneEmulators() {
        val cases = mapOf(
            "PS1" to "com.github.stenzek.duckstation",
            "N64" to "org.mupen64plusae.v3.fzurita",
            "Dreamcast" to "com.flycast.emulator",
            "PSP" to "org.ppsspp.ppsspp",
            "PS2" to "xyz.aethersx2.android",
            "GameCube" to "org.dolphinemu.dolphinemu",
            "Wii" to "org.dolphinemu.dolphinemu",
            "WiiWare" to "org.dolphinemu.dolphinemu",
            "3DS" to "org.azahar_emu.azahar",
            "NES" to "com.explusalpha.NesEmu",
            "SNES" to "com.explusalpha.Snes9xPlus",
            "GBA" to "com.explusalpha.GbaEmu",
            "Game Boy Color" to "com.explusalpha.GbcEmu",
            "DS" to "me.magnum.melonds",
            "Genesis" to "com.explusalpha.MdEmu",
            "Master System" to "com.explusalpha.MdEmu",
            "Game Gear" to "com.explusalpha.MdEmu",
            "Atari 2600" to "com.explusalpha.A2600Emu",
        )

        cases.forEach { (platform, packageName) ->
            val game = Game(
                GameId("standalone-${platform.filter(Char::isLetterOrDigit).lowercase()}"),
                "Standalone $platform", platform, 2020, "Action", 1,
                InstallState.INSTALLED,
                localContentRelativePath = "standalone/game.rom",
                localContentSha256 = "c".repeat(64),
                localContentMimeType = "application/octet-stream",
            )
            val capability = requireNotNull(registry.forGame(game))
            assertEquals(packageName, capability.packageName)
            assertNull(capability.requiredCore)
            assertNull(capability.retroArchCoreFileName)
        }
    }

    @Test
    fun standaloneEmulatorsDoNotReceiveRetroArchCorePaths() {
        listOf("PS1", "N64", "Dreamcast").forEachIndexed { index, platform ->
            val game = Game(
                GameId("standalone-$index"), "Standalone $platform", platform, 2020,
                "Action", 1, InstallState.INSTALLED,
                emulatorPackage = when (platform) {
                    "PS1" -> "com.github.stenzek.duckstation"
                    "N64" -> "org.mupen64plusae.v3.fzurita"
                    else -> "com.flycast.emulator"
                },
                localContentRelativePath = "standalone-$index/game.rom",
                localContentSha256 = "d".repeat(64),
                localContentMimeType = "application/octet-stream",
            )
            val capability = requireNotNull(registry.forGame(game))
            assertNull(capability.requiredCore)
            assertNull(capability.retroArchCoreFileName)
        }
    }

    @Test
    fun wiiWareUsesDolphinInsteadOfAnUnmappedRetroArchCore() {
        val game = Game(GameId("wiiware-demo"), "WiiWare Demo", "WiiWare", 2020,
            "Arcade", 1, InstallState.INSTALLED, expectedSha256 = "abc")
        assertEquals(listOf("org.dolphinemu.dolphinemu"), registry.optionsFor(game))
    }

    @Test
    fun threeDsUsesAzaharWithoutInventingARetroArchCore() {
        val game = Game(GameId("three-ds-demo"), "3DS Demo", "3DS", 2020,
            "Arcade", 1, InstallState.INSTALLED,
            localContentRelativePath = "three-ds-demo/game.3ds",
            localContentSha256 = "a".repeat(64),
            localContentMimeType = "application/octet-stream")
        assertEquals(listOf("org.azahar_emu.azahar"), registry.optionsFor(game))
        val capability = requireNotNull(registry.forGame(game))
        assertEquals("org.azahar_emu.azahar", capability.packageName)
        assertNull(capability.requiredCore)
        assertNull(capability.retroArchCoreFileName)
    }

    @Test
    fun unsupportedGameWithoutDiagnosticCapabilityIsRejected() {
        val android = Game(GameId("android-demo"), "Android Demo", "Android", 2020, "Action", 1, InstallState.INSTALLED,
            expectedSha256 = "abc")
        assertNull(registry.forGame(android))
    }
    @Test
    fun selectedGraphicsProfileReachesLaunchCapability() {
        val game = Game(GameId("ps1-profile"), "PS1 Profile", "PS1", 2020, "Action", 1, InstallState.INSTALLED,
            graphicsProfile = "Performance", expectedSha256 = "abc")
        val capability = requireNotNull(registry.forGame(game))
        assertEquals("Performance", capability.graphicsProfile)
        assertEquals("com.github.stenzek.duckstation", capability.packageName)
    }
    @Test
    fun unknownGraphicsProfileFallsBackToBalanced() {
        val repository = com.gamebox.os.data.FakeGameRepository()
        repository.setEmulatorSettings(GameId("retro-test"), null, "Ultra")
        assertEquals("Balanced", repository.game(GameId("retro-test"))?.graphicsProfile)
    }
    @Test
    fun supportedPackagesHaveFriendlyNames() {
        assertEquals("RetroArch", registry.displayName("com.retroarch.aarch64"))
        assertEquals("PPSSPP", registry.displayName("org.ppsspp.ppsspp"))
        assertEquals("PPSSPP", registry.displayName("org.ppsspp.ppssppgold"))
        assertEquals("PPSSPP", registry.displayName("org.ppsspp.ppsspplegacy"))
    }
    @Test
    fun androidPlatformHasNoExternalEmulatorOptions() {
        val game = Game(GameId("native"), "Native", "Android", 2024, "Arcade", 1, InstallState.INSTALLED)
        assertEquals(emptyList<String>(), registry.optionsFor(game))
    }
}
