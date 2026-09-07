package com.gamebox.os

import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.DownloadRepository
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ProductionInstallBoundaryTest {
    @Test fun productionRepositoriesCannotSimulateCompletion() {
        assertFalse(GameRepository::class.java.methods.any { it.name == "advanceInstall" })
        assertFalse(DownloadRepository::class.java.methods.any { it.name == "advance" })
        assertFalse(File("src/main/java/com/gamebox/os/data/FakeGameRepository.kt").exists())
    }

    @Test fun productionUiUsesWorkerControllerForTransferActions() {
        val source = File("src/main/java/com/gamebox/os/ui/GameBoxApp.kt").readText()
        assertFalse(source.contains("Next test stage"))
        assertFalse(source.contains("repository.pauseOrResume"))
        assertFalse(source.contains("downloadRepository.resume"))
        assertTrue(source.contains("remoteDownloadController.pause(game)"))
        assertTrue(source.contains("remoteDownloadController.resume(game)"))
    }
}
