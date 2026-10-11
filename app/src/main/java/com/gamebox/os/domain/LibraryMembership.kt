package com.gamebox.os.domain

/** States whose content can be launched from local storage without another install step. */
fun InstallState.isPlayableLocally(): Boolean =
    this == InstallState.INSTALLED || this == InstallState.UPDATE_AVAILABLE

/** Keep retained imports reachable after content-only removal. */
fun Game.belongsToLibrary(): Boolean = state.isPlayableLocally() ||
    localContentRelativePath != null || savePresent || lastPlayed != null

fun Game.canReimportContent(): Boolean = localContentRelativePath != null &&
    state in setOf(InstallState.NOT_INSTALLED, InstallState.MISSING_FILES, InstallState.FAILED)

