package com.gamebox.os

import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.LocalContentFile
import com.gamebox.os.launch.EmulatorCapability
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import com.gamebox.os.launch.EmulatorContentRoot
import com.gamebox.os.launch.ReturnTracker
import com.gamebox.os.launch.retroArchCorePath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first

class LaunchAdapterTest {
    @Test fun launchFailuresInvalidateOnlyBrokenContent(): Unit = runBlocking {
        val cases = listOf(
            com.gamebox.os.launch.GatewayResult.CONTENT_MISSING to InstallState.MISSING_FILES,
            com.gamebox.os.launch.GatewayResult.VERIFICATION_FAILED to InstallState.FAILED,
            com.gamebox.os.launch.GatewayResult.EMULATOR_UNAVAILABLE to InstallState.INSTALLED,
            com.gamebox.os.launch.GatewayResult.HANDOFF_REJECTED to InstallState.INSTALLED,
        )
        cases.forEach { (result, expected) ->
            val repository = com.gamebox.os.data.FakeGameRepository()
            val original = repository.observeGames().value.first()
            repository.setInstallState(original.id, InstallState.INSTALLED)
            val installed = requireNotNull(repository.game(original.id))
            val registry = EmulatorCapabilityRegistry(listOf(capability.copy(gameId = installed.id)))
            val gateway = object : com.gamebox.os.launch.PackageGateway {
                override fun launch(capability: EmulatorCapability, preparation: com.gamebox.os.launch.LaunchPreparation) = result
            }
            val controller = com.gamebox.os.launch.DefaultGameLaunchController(registry, gateway, repository, scope = this)
            controller.launch(installed)
            withTimeout(5_000) { controller.observeState().first { it.status != com.gamebox.os.launch.LaunchUiState.Status.PREPARING } }
            controller.onHostResumed()
            val after = requireNotNull(repository.game(original.id))
            assertEquals(expected, after.state)
            assertEquals(original.minutesPlayed, after.minutesPlayed)
            assertEquals(original.favorite, after.favorite)
        }
    }

    @Test fun preparationDoesNotBlockCallerAndDuplicatePlayDoesNotDispatchTwice(): Unit = runBlocking {
        val repository = com.gamebox.os.data.FakeGameRepository()
        val game = repository.observeGames().value.first().copy(state = InstallState.INSTALLED)
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val caller = Thread.currentThread()
        val gateway = object : com.gamebox.os.launch.PackageGateway {
            override fun launch(capability: EmulatorCapability, preparation: com.gamebox.os.launch.LaunchPreparation): com.gamebox.os.launch.GatewayResult {
                calls.incrementAndGet()
                org.junit.Assert.assertNotEquals(caller, Thread.currentThread())
                entered.countDown()
                check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
                return com.gamebox.os.launch.GatewayResult.LAUNCHED
            }
        }
        val controller = com.gamebox.os.launch.DefaultGameLaunchController(
            EmulatorCapabilityRegistry(listOf(capability.copy(gameId = game.id))), gateway, repository, scope = this
        )
        try {
            controller.launch(game)
            assertEquals(com.gamebox.os.launch.LaunchUiState.Status.PREPARING, controller.observeState().value.status)
            controller.launch(game)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                check(entered.await(5, java.util.concurrent.TimeUnit.SECONDS))
            }
            controller.onHostResumed() // No session exists while file validation is running.
            assertEquals(com.gamebox.os.launch.LaunchUiState.Status.PREPARING, controller.observeState().value.status)
        } finally { release.countDown() }
        withTimeout(5_000) { controller.observeState().first { it.status == com.gamebox.os.launch.LaunchUiState.Status.LAUNCHED } }
        controller.launch(game)
        assertEquals(1, calls.get())
        controller.onHostResumed()
        assertEquals(com.gamebox.os.launch.LaunchUiState.Status.RETURNED, controller.observeState().value.status)
    }

    @Test fun storageFailureBecomesActionableStateAndAllowsRetry(): Unit = runBlocking {
        val repository = com.gamebox.os.data.FakeGameRepository()
        val game = repository.observeGames().value.first().copy(state = InstallState.INSTALLED)
        var calls = 0
        val gateway = object : com.gamebox.os.launch.PackageGateway {
            override fun launch(capability: EmulatorCapability, preparation: com.gamebox.os.launch.LaunchPreparation): com.gamebox.os.launch.GatewayResult {
                calls++
                throw java.io.IOException("unreadable content")
            }
        }
        val controller = com.gamebox.os.launch.DefaultGameLaunchController(
            EmulatorCapabilityRegistry(listOf(capability.copy(gameId = game.id))), gateway, repository, scope = this
        )
        repeat(2) {
            controller.launch(game)
            withTimeout(5_000) { controller.observeState().first { it.status == com.gamebox.os.launch.LaunchUiState.Status.HANDOFF_REJECTED } }
        }
        assertEquals(2, calls)
    }

    private val capability = EmulatorCapability(
        "approved",
        GameId("retro-test"),
        "example.emulator",
        "retro/retro-test/content/test.txt",
        "application/octet-stream",
        "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2"
    )

    @Test fun registry_matchesOnlyExplicitGameCapability() {
        val registry = EmulatorCapabilityRegistry(listOf(capability))

        assertEquals("example.emulator", registry.forGame(GameId("retro-test"))?.packageName)
        assertNull(registry.forGame(GameId("different-game")))
    }

    @Test fun capabilityContainsOnlyRelativeScopedContentPath() {
        assertEquals("retro/retro-test/content/test.txt", capability.contentRelativePath)
        assertEquals("application/octet-stream", capability.mimeType)
    }

    @Test fun galaxyPatrolDeclaresRequiredNesCore() {
        val game = Game(GameId("galaxy-patrol"), "Galaxy Patrol", "Retro", 2018, "Arcade", 1, InstallState.INSTALLED)
        val registry = EmulatorCapabilityRegistry()
        val message = registry.readinessMessage(game)
        assertEquals(true, message?.contains("FCEUmm"))
        assertEquals("fceumm_libretro_android.so", registry.forGame(game)?.retroArchCoreFileName)
    }

    @Test fun galaxyPatrolUsesRetroArchStablePrivateCorePath() {
        assertEquals(
            "/data/data/com.retroarch.aarch64/cores/fceumm_libretro_android.so",
            retroArchCorePath("com.retroarch.aarch64", "fceumm_libretro_android.so")
        )
    }

    @Test fun registryExposesApprovedAdaptersForAdditionalPlatformGroups() {
        val registry = EmulatorCapabilityRegistry()
        val installed = InstallState.INSTALLED
        val ps1 = Game(GameId("ps1-test"), "PS1 Test", "PS1", 1999, "Racing", 1, installed)
        val n64 = Game(GameId("n64-test"), "N64 Test", "N64", 1999, "Racing", 1, installed)
        val dreamcast = Game(GameId("dc-test"), "DC Test", "Dreamcast", 1999, "Racing", 1, installed)

        assertEquals(listOf("com.github.stenzek.duckstation", "com.retroarch.aarch64"), registry.optionsFor(ps1))
        assertEquals(listOf("org.mupen64plusae.v3.fzurita", "com.retroarch.aarch64"), registry.optionsFor(n64))
        assertEquals(listOf("com.flycast.emulator", "com.retroarch.aarch64"), registry.optionsFor(dreamcast))
        assertEquals("DuckStation", registry.displayName("com.github.stenzek.duckstation"))
        assertEquals("M64Plus FZ", registry.displayName("org.mupen64plusae.v3.fzurita"))
        assertEquals("Flycast", registry.displayName("com.flycast.emulator"))
        val nes = Game(GameId("nes-test"), "NES Test", "Nintendo Entertainment System", 1986, "Action", 1, installed)
        assertEquals("com.retroarch.aarch64", registry.optionsFor(nes).first())
    }

    @Test fun importedContentUsesImportRootAndFriendlyPlatformAlias() {
        val game = Game(
            GameId("portable"), "Portable", "PlayStation Portable", 2008, "Racing", 512,
            InstallState.INSTALLED,
            localContentRelativePath = "portable/Portable.cso",
            localContentSha256 = "b".repeat(64),
            localContentMimeType = "application/x-compressed-iso",
            localContentFiles = listOf(
                LocalContentFile("portable/Portable.cso", "b".repeat(64), "application/x-compressed-iso"),
                LocalContentFile("portable/Portable.sidecar", "c".repeat(64), "application/octet-stream"),
            ),
        )

        val imported = requireNotNull(EmulatorCapabilityRegistry().forGame(game))

        assertEquals("org.ppsspp.ppsspp", imported.packageName)
        assertEquals("portable/Portable.cso", imported.contentRelativePath)
        assertEquals(EmulatorContentRoot.IMPORTS, imported.contentRoot)
        assertEquals(listOf("portable/Portable.sidecar"), imported.companionFiles.map { it.relativePath })
    }

    @Test fun returnTracker_recordsOneSessionAndThenClearsIt() {
        var time = 1_000L
        val tracker = ReturnTracker { time }
        tracker.started(GameId("retro-test"))
        time += 125_000L

        val session = tracker.returned()

        assertEquals(GameId("retro-test"), session?.gameId)
        assertEquals(2, session?.minutesPlayed)
        assertNull(tracker.returned())
    }

    @Test fun clockRollback_neverCreatesNegativePlayTime() {
        var time = 2_000L
        val tracker = ReturnTracker { time }
        tracker.started(GameId("retro-test"))
        time = 1_000L

        assertEquals(0, tracker.returned()?.minutesPlayed)
    }
}
