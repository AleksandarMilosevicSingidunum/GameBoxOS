package com.gamebox.os.data

import android.content.Context
import com.gamebox.os.storage.DirectorySaveAdapter
import com.gamebox.os.storage.GameSaveBackupResult
import com.gamebox.os.storage.GameSaveRestoreResult
import com.gamebox.os.storage.SafSaveAdapter
import com.gamebox.os.storage.SaveAdapterRegistry
import com.gamebox.os.storage.SaveBackupCoordinator
import com.gamebox.os.storage.SaveBackupService
import com.gamebox.os.storage.SaveRestoreCoordinator
import com.gamebox.os.storage.SaveSnapshotManifestStore

/** Coordinates managed and explicitly authorized emulator-save snapshots for game sessions. */
internal class GameSaveSessionBackups(
    context: Context,
    emulatorSaveTreeUri: () -> String,
) {
    private val managedSaveBackup = SaveBackupCoordinator(
        registry = SaveAdapterRegistry(
            mapOf("*" to DirectorySaveAdapter(context.filesDir.resolve("saves"))),
        ),
        backupService = SaveBackupService(
            context.filesDir.resolve("saves"),
            context.filesDir.resolve("save-backups"),
        ),
        manifestStore = SaveSnapshotManifestStore(
            context.filesDir.resolve("save-backup-manifests"),
        ),
    )

    private val emulatorSaveAdapter = SafSaveAdapter(
        context = context,
        treeUri = emulatorSaveTreeUri,
    )
    private val emulatorSaveBackupService = SaveBackupService(
        context.filesDir.resolve("emulator-saves"),
        context.filesDir.resolve("emulator-save-backups"),
    )
    private val emulatorSaveManifestStore = SaveSnapshotManifestStore(
        context.filesDir.resolve("emulator-save-backup-manifests"),
    )
    private val emulatorSaveBackup = SaveBackupCoordinator(
        registry = SaveAdapterRegistry(mapOf("*" to emulatorSaveAdapter)),
        backupService = emulatorSaveBackupService,
        manifestStore = emulatorSaveManifestStore,
    )
    private val emulatorSaveRestore = SaveRestoreCoordinator(
        backupService = emulatorSaveBackupService,
        manifestStore = emulatorSaveManifestStore,
        adapter = emulatorSaveAdapter,
    )

    fun backup(platform: String, gameId: String): GameSaveBackupResult {
        val managed = managedSaveBackup.backup(platform, gameId)
        val emulator = emulatorSaveBackup.backup(platform, gameId)
        return GameSaveBackupResult(
            gameId = gameId,
            artifacts = managed.artifacts + emulator.artifacts,
            message = listOfNotNull(managed.message, emulator.message)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" "),
        )
    }

    fun restoreLatest(gameId: String): GameSaveRestoreResult =
        emulatorSaveRestore.restoreLatest(gameId)
}
