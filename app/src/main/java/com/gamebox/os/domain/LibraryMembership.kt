package com.gamebox.os.domain

/** Keep retained imports reachable after content-only removal. */
fun Game.belongsToLibrary(): Boolean = state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE) ||
    localContentRelativePath != null || savePresent || lastPlayed != null

fun Game.canReimportContent(): Boolean = localContentRelativePath != null &&
    state in setOf(InstallState.NOT_INSTALLED, InstallState.MISSING_FILES, InstallState.FAILED)

