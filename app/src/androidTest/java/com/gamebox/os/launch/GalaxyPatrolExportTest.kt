package com.gamebox.os.launch

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Tests real MediaStore duplicate-name handling, not RetroArch execution. */
@RunWith(AndroidJUnit4::class)
class GalaxyPatrolExportTest {
    @Test fun descriptorExportPublishesEveryVerifiedDiscTrackBesideTheCue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = "Download/GameBox/retroarch-disc-test-" + UUID.randomUUID()
        val preferencesName = "retroarch-disc-export-test-" + UUID.randomUUID()
        val cue = File.createTempFile("gamebox-disc-", ".cue", context.cacheDir)
        val track = File.createTempFile("gamebox-track-", ".bin", context.cacheDir)
        val publisher = RetroArchContentPublisher(context, preferencesName)
        try {
            cue.writeText("FILE \"track 01.bin\" BINARY\\n  TRACK 01 MODE2/2352\\n")
            track.writeBytes(byteArrayOf(3, 1, 4, 1, 5, 9))
            val exportedCue = requireNotNull(
                publisher.publishSet(
                    files = listOf(
                        RetroArchContentSource("disc/game.cue", "application/x-cue", cue::inputStream),
                        RetroArchContentSource("disc/track 01.bin", "application/octet-stream", track::inputStream),
                    ),
                    primaryRelativePath = "disc/game.cue",
                    relativeDirectory = directory,
                ),
            )
            val exportedTrack = File(requireNotNull(File(exportedCue).parentFile), "track 01.bin")
            assertTrue("The CUE must remain the selected launch file", File(exportedCue).isFile)
            assertTrue("The referenced sibling track must also exist", exportedTrack.isFile)
            assertTrue(File(exportedCue).readText().contains("FILE \"track 01.bin\" BINARY"))
            assertArrayEquals(track.readBytes(), exportedTrack.readBytes())
        } finally {
            publisher.cleanup()
            removeMediaStoreDirectory(context, directory)
            cue.delete()
            track.delete()
        }
    }

    @Test fun duplicateExportsReturnTheirActualPaths() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = "Download/GameBox-export-test-" + UUID.randomUUID()
        val source = File.createTempFile("galaxy-export-", ".nes", context.cacheDir)
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        try {
            val bytes = context.assets.open("downloads/galaxy-patrol.nes").use { it.readBytes() }
            source.writeBytes(bytes)
            val gateway = AndroidPackageGateway(context)
            val first = requireNotNull(gateway.publishGalaxyPatrolForRetroArch(source, directory))
            val second = requireNotNull(gateway.publishGalaxyPatrolForRetroArch(source, directory))
            assertNotEquals("Duplicate names must not hand off the older export", first, second)
            assertArrayEquals(bytes, File(first).readBytes())
            assertArrayEquals(bytes, File(second).readBytes())
        } finally {
            // Only remove rows in this test's unique directory, never normal exports.
            val ownedRows = context.contentResolver.query(
                collection, arrayOf(MediaStore.Downloads._ID),
                MediaStore.Downloads.RELATIVE_PATH + " = ?", arrayOf(directory + "/"), null
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(ContentUris.withAppendedId(collection, cursor.getLong(0)))
                }
            }.orEmpty()
            ownedRows.forEach { context.contentResolver.delete(it, null, null) }
            source.delete()
        }
    }

    @Test fun cleanupRemovesTemporaryExportsAfterInterruptedSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = "Download/GameBox-export-cleanup-test-" + UUID.randomUUID()
        val source = File.createTempFile("galaxy-cleanup-", ".nes", context.cacheDir)
        try {
            val bytes = context.assets.open("downloads/galaxy-patrol.nes").use { it.readBytes() }
            source.writeBytes(bytes)
            val gateway = AndroidPackageGateway(context)
            val exported = requireNotNull(gateway.publishGalaxyPatrolForRetroArch(source, directory))
            assertTrue(File(exported).isFile)

            // A fresh gateway models the process that follows an interrupted
            // handoff; cleanup must use persisted ownership, not only memory.
            AndroidPackageGateway(context).cleanupLaunchedContent()

            assertFalse("Interrupted-session export must be removed", File(exported).exists())
        } finally {
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val ownedRows = context.contentResolver.query(
                collection, arrayOf(MediaStore.Downloads._ID),
                MediaStore.Downloads.RELATIVE_PATH + " = ?", arrayOf(directory + "/"), null
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(ContentUris.withAppendedId(collection, cursor.getLong(0)))
                }
            }.orEmpty()
            ownedRows.forEach { context.contentResolver.delete(it, null, null) }
            source.delete()
        }
    }

    private fun removeMediaStoreDirectory(context: Context, directory: String) {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val ownedRows = context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            MediaStore.Downloads.RELATIVE_PATH + " = ?",
            arrayOf(directory + "/"),
            null,
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(ContentUris.withAppendedId(collection, cursor.getLong(0)))
            }
        }.orEmpty()
        ownedRows.forEach { context.contentResolver.delete(it, null, null) }
    }
}
