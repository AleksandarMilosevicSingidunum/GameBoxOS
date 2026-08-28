package com.gamebox.os

import com.gamebox.os.data.local.toDomain
import com.gamebox.os.data.local.toEntity
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class GameEntityMappingTest {
    @Test fun entityRoundTrip_preservesStableIdentityAndState() {
        val game = Game(
            GameId("fixture"), "Fixture", "Homebrew", 2026,
            "Test", 8, InstallState.PAUSED, "Today", 12
        )
        assertEquals(game, game.toEntity().toDomain())
    }

    @Test fun unknownPersistedState_failsClosed() {
        val entity = Game(
            GameId("fixture"), "Fixture", "Homebrew", 2026,
            "Test", 8, InstallState.INSTALLED
        ).toEntity().copy(installState = "UNKNOWN_FUTURE_STATE")
        assertEquals(InstallState.FAILED, entity.toDomain().state)
    }
}
