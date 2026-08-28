package com.gamebox.os

import com.gamebox.os.data.FakeGameRepository
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeGameRepositoryTest {
    @Test fun installFlow_usesExplicitOrderedStates() {
        val repository = FakeGameRepository()
        val id = GameId("freedoom")
        val expected = listOf(
            InstallState.QUEUED,
            InstallState.DOWNLOADING,
            InstallState.VERIFYING,
            InstallState.INSTALLING,
            InstallState.INSTALLED
        )
        expected.forEach { state ->
            repository.advanceInstall(id)
            assertEquals(state, repository.game(id)?.state)
        }
    }

    @Test fun pauseOnlyChangesActiveDownload() {
        val repository = FakeGameRepository()
        val id = GameId("freedoom")
        repository.advanceInstall(id)
        repository.advanceInstall(id)
        repository.pauseOrResume(id)
        assertEquals(InstallState.PAUSED, repository.game(id)?.state)
        repository.pauseOrResume(id)
        assertEquals(InstallState.DOWNLOADING, repository.game(id)?.state)
    }
}
