package com.gamebox.os.importer

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
class AuthorizedRomImporterCancellationTest {
    @Test
    fun cancellationDuringCopyStopsImportAndRemovesStagedFiles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val gameId = GameId("cancel-import-${UUID.randomUUID()}")
        val source = File(context.cacheDir, "${gameId.value}.nes")
        source.writeBytes(ByteArray(4 * 1024 * 1024))
        val parentJob = Job()
        val sawCopyProgress = AtomicBoolean(false)
        val maxReportedBytes = AtomicLong(0L)

        try {
            val importJob = CoroutineScope(parentJob + Dispatchers.Default).launch {
                AuthorizedRomImporter(context).import(
                    gameId = gameId,
                    source = Uri.fromFile(source),
                    displayName = source.name,
                    platform = "NES",
                    onProgress = { progress ->
                        maxReportedBytes.updateAndGet { previous ->
                            maxOf(previous, progress.bytesProcessed)
                        }
                        if (
                            progress.stage == RomImportStage.COPYING_AND_VERIFYING &&
                            progress.bytesProcessed >= 1024L * 1024L
                        ) {
                            sawCopyProgress.set(true)
                            parentJob.cancel()
                        }
                    },
                )
            }
            importJob.join()

            val importsRoot = context.filesDir.resolve("imports")
            val consoleDirectory = importsRoot.resolve(RomImportPolicy.platformDirectory("NES"))
            val stagingPrefix = ".staging-${gameId.value}-"
            assertTrue("the copy should reach a progress boundary", sawCopyProgress.get())
            assertTrue("cancellation should remain cancellation", importJob.isCancelled)
            assertTrue(
                "the importer should stop reading before the complete source is copied",
                maxReportedBytes.get() < source.length(),
            )
            assertFalse(consoleDirectory.resolve(gameId.value).exists())
            assertFalse(
                "cancelled import should remove its staging directory",
                importsRoot.listFiles().orEmpty().any { it.name.startsWith(stagingPrefix) },
            )
        } finally {
            parentJob.cancel()
            source.delete()
        }
    }
}
