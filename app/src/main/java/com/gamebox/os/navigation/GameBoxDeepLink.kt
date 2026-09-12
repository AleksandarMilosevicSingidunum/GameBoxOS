package com.gamebox.os.navigation

import android.content.Context
import android.content.Intent
import com.gamebox.os.MainActivity

data class GameBoxNavigationRequest(
    val destination: String,
    val gameId: String? = null,
    val requestId: Long = 0L,
)

object GameBoxDeepLink {
    const val EXTRA_DESTINATION = "com.gamebox.os.extra.DESTINATION"
    const val EXTRA_GAME_ID = "com.gamebox.os.extra.GAME_ID"

    private val destinations = setOf(
        "HOME", "LIBRARY", "STORE", "DOWNLOADS", "MEDIA", "PC", "SETTINGS"
    )
    private val gameIdPattern = Regex("[A-Za-z0-9._-]{1,160}")

    fun intent(context: Context, destination: String, gameId: String? = null): Intent {
        require(destination in destinations) { "Unsupported GameBox destination" }
        require(gameId == null || gameId.matches(gameIdPattern)) { "Invalid game ID" }
        return Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_DESTINATION, destination)
            .apply { gameId?.let { putExtra(EXTRA_GAME_ID, it) } }
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    fun parse(intent: Intent?): GameBoxNavigationRequest? {
        val destination = intent?.getStringExtra(EXTRA_DESTINATION)
            ?.takeIf { it in destinations } ?: return null
        val gameId = intent.getStringExtra(EXTRA_GAME_ID)
            ?.takeIf { it.matches(gameIdPattern) }
        return GameBoxNavigationRequest(destination, gameId)
    }
}
