package com.gamebox.os

import com.gamebox.os.domain.GameId
import com.gamebox.os.storage.ArtifactKind
import com.gamebox.os.storage.InstallPathPolicy
import com.gamebox.os.storage.StoredArtifact
import com.gamebox.os.storage.UninstallPlanner
import com.gamebox.os.storage.toConfirmation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageSafetyTest {
    private val gameId = GameId("freedoom")

    @Test fun installPath_isDeterministicAndPlatformScoped() {
        assertEquals(
            "source-port/freedoom/content/game.zip",
            InstallPathPolicy().relativeContentPath("Source Port", gameId, "game.zip")
        )
    }

    @Test fun installPath_rejectsTraversalAndSeparators() {
        assertThrows(IllegalArgumentException::class.java) {
            InstallPathPolicy().relativeContentPath("Retro", GameId("../other"), "game.zip")
        }
        assertThrows(IllegalArgumentException::class.java) {
            InstallPathPolicy().relativeContentPath("Retro", gameId, "../save.dat")
        }
        assertThrows(IllegalArgumentException::class.java) {
            InstallPathPolicy().relativeContentPath("Retro", gameId, "folder/game.zip")
        }
    }

    @Test fun defaultUninstall_deletesOnlyGameContent() {
        val plan = UninstallPlanner().plan(gameId, artifacts())

        assertEquals(listOf(ArtifactKind.GAME_CONTENT), plan.deleteArtifacts.map { it.kind })
        assertEquals(100L, plan.bytesFreed)
        assertTrue(plan.retainArtifacts.any { it.kind == ArtifactKind.SAVE_DATA })
        assertTrue(plan.retainArtifacts.any { it.kind == ArtifactKind.PLAY_HISTORY })
    }

    @Test fun advancedProgressDeletion_stillRetainsMetadataAndHistory() {
        val plan = UninstallPlanner().plan(gameId, artifacts(), deleteProgress = true)

        assertTrue(plan.deleteArtifacts.any { it.kind == ArtifactKind.SAVE_DATA })
        assertTrue(plan.deleteArtifacts.any { it.kind == ArtifactKind.SAVE_STATE })
        assertTrue(plan.retainArtifacts.any { it.kind == ArtifactKind.METADATA })
        assertTrue(plan.retainArtifacts.any { it.kind == ArtifactKind.PLAY_HISTORY })
    }

    @Test fun confirmation_reportsExactFreedAndRetainedSaveBytes() {
        val confirmation = UninstallPlanner().plan(gameId, artifacts()).toConfirmation()

        assertEquals(100L, confirmation.bytesFreed)
        assertEquals(15L, confirmation.retainedSaveBytes)
        assertEquals(2, confirmation.retainedSaveArtifacts)
        assertTrue(confirmation.retainsProgress)
    }

    @Test fun progressDeletion_confirmationDoesNotClaimRetainedSave() {
        val confirmation = UninstallPlanner()
            .plan(gameId, artifacts(), deleteProgress = true)
            .toConfirmation()

        assertEquals(115L, confirmation.bytesFreed)
        assertEquals(0L, confirmation.retainedSaveBytes)
        assertEquals(0, confirmation.retainedSaveArtifacts)
    }

    private fun artifacts() = listOf(
        StoredArtifact(gameId, "content://game", ArtifactKind.GAME_CONTENT, 100),
        StoredArtifact(gameId, "content://save", ArtifactKind.SAVE_DATA, 5),
        StoredArtifact(gameId, "content://state", ArtifactKind.SAVE_STATE, 10),
        StoredArtifact(gameId, "db://metadata", ArtifactKind.METADATA, 1),
        StoredArtifact(gameId, "db://history", ArtifactKind.PLAY_HISTORY, 1)
    )
}
