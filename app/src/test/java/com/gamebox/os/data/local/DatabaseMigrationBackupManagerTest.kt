package com.gamebox.os.data.local

import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseMigrationBackupManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun copiesDatabaseAndWalBeforeUpgrade() {
        val database = sqliteHeader("gamebox.db", 11, byteArrayOf(7, 8, 9))
        val wal = File(database.path + "-wal").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val root = temporaryFolder.newFolder("backups")
        val backup = DatabaseMigrationBackupManager(root, nowMillis = { 1234L })
            .createIfNeeded(database, 12)

        requireNotNull(backup)
        assertEquals(11, backup.sourceVersion)
        assertEquals(12, backup.targetVersion)
        assertArrayEquals(database.readBytes(), backup.directory.resolve(database.name).readBytes())
        assertArrayEquals(wal.readBytes(), backup.directory.resolve(wal.name).readBytes())
        assertTrue(backup.directory.resolve("manifest.txt").readText().contains("sourceVersion=11"))
    }

    @Test
    fun skipsFreshCurrentAndUnknownDatabases() {
        val root = temporaryFolder.newFolder("backups")
        val manager = DatabaseMigrationBackupManager(root)
        assertNull(manager.createIfNeeded(temporaryFolder.root.resolve("missing.db"), 12))
        assertNull(manager.createIfNeeded(sqliteHeader("current.db", 12), 12))
        assertNull(manager.createIfNeeded(temporaryFolder.newFile("empty.db"), 12))
    }

    @Test
    fun retainsOnlyConfiguredNumberOfCommittedBackups() {
        val root = temporaryFolder.newFolder("backups")
        var time = 100L
        val manager = DatabaseMigrationBackupManager(root, retainedBackups = 2) { time++ }
        val database = sqliteHeader("gamebox.db", 9)

        manager.createIfNeeded(database, 12)
        writeVersion(database, 10)
        manager.createIfNeeded(database, 12)
        writeVersion(database, 11)
        manager.createIfNeeded(database, 12)

        val committed = root.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("pre-migration-") }
        assertEquals(2, committed.size)
        assertTrue(committed.none { it.name.contains("v9-to-v12") })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroRetention() {
        DatabaseMigrationBackupManager(temporaryFolder.root, retainedBackups = 0)
    }

    private fun sqliteHeader(name: String, version: Int, payload: ByteArray = byteArrayOf()): File =
        temporaryFolder.newFile(name).also { file ->
            RandomAccessFile(file, "rw").use {
                it.setLength(64)
                it.seek(60)
                it.writeInt(version)
                it.seek(64)
                it.write(payload)
            }
        }

    private fun writeVersion(file: File, version: Int) {
        RandomAccessFile(file, "rw").use {
            it.seek(60)
            it.writeInt(version)
        }
    }
}
