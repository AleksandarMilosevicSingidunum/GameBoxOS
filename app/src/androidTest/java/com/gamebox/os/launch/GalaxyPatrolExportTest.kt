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
}
