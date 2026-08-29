package com.gamebox.os.storage

import android.content.Context
import android.net.Uri
import java.io.File

class InstalledContentMigration(private val installedRoot: File) {
    fun plan(): ContentMigrationPlan {
        if (!installedRoot.exists()) return ContentMigrationPlan(emptyList(), 0)
        val canonicalRoot = installedRoot.canonicalFile
        val items = canonicalRoot.walkTopDown().filter { it.isFile }.map { file ->
            val canonical = file.canonicalFile
            require(canonical.toPath().startsWith(canonicalRoot.toPath())) { "installed content escapes storage root" }
            val relative = canonical.relativeTo(canonicalRoot).invariantSeparatorsPath
            val segments = relative.split('/')
            val gameId = segments.getOrNull(1)?.takeIf { it.isNotBlank() } ?: segments.first()
            ContentMigrationItem(gameId, relative, canonical.length())
        }.toList()
        return ContentMigrationPlanner.plan(items)
    }
    fun execute(context: Context, treeUri: Uri, plan: ContentMigrationPlan): ContentMigrationResult =
        ContentMigrationExecutor(SafDocumentTreeCopyOperation(context, treeUri, installedRoot)).execute(plan)
}
