package com.gamebox.os.importer

import com.gamebox.os.domain.LocalContentFile

/** Exact restore only: a new edition or different disc set needs a separate import. */
internal fun verifyReimportIdentity(expected: List<LocalContentFile>, actual: List<LocalContentFile>) {
    require(expected.isNotEmpty()) { "Original content identity is missing; use a new import" }
    fun identities(files: List<LocalContentFile>) = files.associate { it.relativePath to it.sha256 }
    require(expected.map { it.relativePath }.distinct().size == expected.size &&
        actual.map { it.relativePath }.distinct().size == actual.size &&
        identities(expected) == identities(actual)) {
        "Selected files do not match the original import. Select the original filenames and content, or import a different edition separately."
    }
}

