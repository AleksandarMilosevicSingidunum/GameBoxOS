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
        val uiSources = File("src/main/java/com/gamebox/os/ui")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map(File::readText)
            .toList()
        val source = uiSources.joinToString("\n")
        assertFalse(source.contains("Next test stage"))
        assertFalse(source.contains("repository.pauseOrResume"))
        assertFalse(source.contains("downloadRepository.resume"))
        assertTrue(source.contains("remoteDownloadController.pause("))
        assertTrue(source.contains("remoteDownloadController.resume("))
    }
}
