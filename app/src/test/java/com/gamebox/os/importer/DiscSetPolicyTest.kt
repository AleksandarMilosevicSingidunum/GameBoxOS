package com.gamebox.os.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.file.Files

class DiscSetPolicyTest {
    @Test fun selectsCueAndRequiresEveryReferencedTrack() {
        val directory = Files.createTempDirectory("gamebox-cue-test").toFile()
        try {
            directory.resolve("Game.cue").writeText(
                """
                FILE "Game (Track 1).bin" BINARY
                  TRACK 01 MODE2/2352
                FILE "Game (Track 2).bin" BINARY
                  TRACK 02 AUDIO
                """.trimIndent()
            )

            assertEquals(
                "Game.cue",
                DiscSetPolicy.selectLaunchFile(
                    directory,
                    listOf("Game.cue", "Game (Track 1).bin", "Game (Track 2).bin"),
                ),
            )
            assertThrows(IllegalArgumentException::class.java) {
                DiscSetPolicy.selectLaunchFile(directory, listOf("Game.cue", "Game (Track 1).bin"))
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun validatesGdiTrackListAndRejectsAmbiguousSets() {
        val directory = Files.createTempDirectory("gamebox-gdi-test").toFile()
        try {
            directory.resolve("Game.gdi").writeText(
                """
                2
                1 0 4 2352 track01.bin 0
                2 45000 0 2352 track02.raw 0
                """.trimIndent()
            )
            assertEquals(
                "Game.gdi",
                DiscSetPolicy.selectLaunchFile(
                    directory,
                    listOf("Game.gdi", "track01.bin", "track02.raw"),
                ),
            )
            assertThrows(IllegalArgumentException::class.java) {
                DiscSetPolicy.selectLaunchFile(directory, listOf("disc1.iso", "disc2.iso"))
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun mdsAndCcdRequireTheirImplicitDataFiles() {
        val directory = Files.createTempDirectory("gamebox-descriptor-test").toFile()
        try {
            directory.resolve("Disc.mds").writeBytes(byteArrayOf(1))
            directory.resolve("Disc.ccd").writeText("[CloneCD]")

            assertEquals(
                "Disc.mds",
                DiscSetPolicy.selectLaunchFile(directory, listOf("Disc.mds", "Disc.mdf")),
            )
            assertThrows(IllegalArgumentException::class.java) {
                DiscSetPolicy.selectLaunchFile(directory, listOf("Disc.ccd", "Other.img"))
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
