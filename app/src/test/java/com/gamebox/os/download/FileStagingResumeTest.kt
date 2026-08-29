package com.gamebox.os.download

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class FileStagingResumeTest {
    @Test
    fun appendsToContainedPartialWithoutTouchingFinalFile() {
        val root = Files.createTempDirectory("gamebox-resume").toFile()
        try {
            val target = FileStagingTarget(root, "remote/demo/content.bin")
            target.openOutput(append = false).use { it.write(byteArrayOf(1, 2, 3)) }
            assertEquals(3L, target.stagedBytes)
            target.openOutput(append = true).use { it.write(byteArrayOf(4, 5)) }
            assertEquals(5L, target.stagedBytes)
            assertEquals(listOf<Byte>(1, 2, 3, 4, 5), target.openInput().use { it.readBytes() }.toList())
            assertEquals(false, target.finalFile.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
