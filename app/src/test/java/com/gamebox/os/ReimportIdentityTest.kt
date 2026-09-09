package com.gamebox.os

import com.gamebox.os.domain.LocalContentFile
import com.gamebox.os.importer.verifyReimportIdentity
import org.junit.Assert.assertThrows
import org.junit.Test

class ReimportIdentityTest {
    private val disc = LocalContentFile("game/disc.cue", "a".repeat(64), "text/plain")
    private val track = LocalContentFile("game/track.bin", "b".repeat(64), "application/octet-stream")
    @Test fun sameContentAcceptsPickerOrderChanges() {
        verifyReimportIdentity(listOf(disc, track), listOf(track, disc))
    }
    @Test fun rejectsDifferentTrackMissingTrackExtraFileAndRenaming() {
        val expected = listOf(disc, track)
        for (actual in listOf(listOf(disc, track.copy(sha256 = "c".repeat(64))),
            listOf(disc), expected + track.copy(relativePath = "game/extra.bin"),
            listOf(disc, track.copy(relativePath = "game/renamed.bin")), expected + track)) {
            assertThrows(IllegalArgumentException::class.java) { verifyReimportIdentity(expected, actual) }
        }
    }
    @Test fun missingIdentityFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) { verifyReimportIdentity(emptyList(), listOf(disc)) }
    }
}

