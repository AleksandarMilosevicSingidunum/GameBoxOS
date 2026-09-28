package com.gamebox.os.importer

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the packaged Android APK contains the XZ runtime required by 7z/LZMA2. */
@RunWith(AndroidJUnit4::class)
class SevenZipRuntimeTest {
    @Test
    fun packagedSevenZipRuntimeExtractsSyntheticPspPayload() {
        val root = File(
            ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir,
            "sevenzip-runtime-${System.nanoTime()}",
        ).apply { mkdirs() }
        try {
            val payload = root.resolve("fixture.iso").apply { writeBytes(byteArrayOf(7, 8, 9, 10)) }
            val archive = root.resolve("fixture.7z")
            SevenZOutputFile(archive).use { sevenZip ->
                sevenZip.putArchiveEntry(sevenZip.createArchiveEntry(payload, "nested/${payload.name}"))
                sevenZip.write(payload.readBytes())
                sevenZip.closeArchiveEntry()
            }

            val result = RomArchiveExtractor.extractIfArchive(
                source = archive,
                staging = root.resolve("staging"),
                platform = "PSP",
                maxFiles = 8,
                maxBytes = 1024,
            )

            assertEquals(listOf("fixture.iso"), result!!.map { it.displayName })
            assertTrue(result.single().file.readBytes().contentEquals(byteArrayOf(7, 8, 9, 10)))
        } finally {
            root.deleteRecursively()
        }
    }
}
