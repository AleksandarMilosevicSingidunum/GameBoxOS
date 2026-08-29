package com.gamebox.os.ui

import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.Game

object GameBoxSemantics {
    const val HOME_TAB = "gamebox.home.tab"
    const val LIBRARY_TAB = "gamebox.library.tab"
    const val STORE_TAB = "gamebox.store.tab"
    const val SETTINGS_TAB = "gamebox.settings.tab"
    const val GAME_CARD = "gamebox.game.card"
    const val PRIMARY_ACTION = "gamebox.primary.action"
    const val BACK_ACTION = "gamebox.back.action"

    fun gameCardDescription(game: Game, hero: Boolean): String = buildList {
        add(game.title)
        add(game.platform)
        add(game.state.name.lowercase().replace('_', ' '))
        if (game.favorite) add("favorite")
        if (hero) add("continue playing")
    }.joinToString(", ")

    fun downloadDescription(job: DownloadJob): String = buildList {
        add(job.title)
        add(job.status.name.lowercase().replace('_', ' '))
        job.errorReason?.takeIf { it.isNotBlank() }?.let(::add)
    }.joinToString(", ")

    fun downloadProgressDescription(job: DownloadJob): String {
        val percent = (job.progress * 100f).toInt().coerceIn(0, 100)
        return "$percent percent downloaded"
    }
}
