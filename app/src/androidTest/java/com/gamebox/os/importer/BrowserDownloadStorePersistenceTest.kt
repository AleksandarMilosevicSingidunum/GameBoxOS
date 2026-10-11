package com.gamebox.os.importer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserDownloadStorePersistenceTest {
    @Test
    fun trackedRecordSurvivesStoreRecreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.gamebox.os.debug", context.packageName)
        val preferences = context.getSharedPreferences("tracked_browser_downloads", Context.MODE_PRIVATE)
        val original = preferences.getString("records", "[]")
        val downloadId = System.currentTimeMillis().coerceAtLeast(1L)
        val record = BrowserDownloadRecord(
            downloadId = downloadId,
            stagedName = "fixture-$downloadId.zip",
            displayName = "Fixture game.zip",
            gameId = "fixture-game-$downloadId",
            title = "Fixture game",
            platformName = "PSP",
            platformId = "psp",
        )
        try {
            val firstStore = BrowserDownloadStore(context)
            firstStore.add(record)
            firstStore.update(downloadId) {
                it.copy(status = BrowserDownloadStatus.DOWNLOADING, downloadedBytes = 128L, totalBytes = 512L)
            }

            val restored = BrowserDownloadStore(context).get(downloadId)

            assertNotNull(restored)
            assertEquals(record.title, restored?.title)
            assertEquals(record.gameId, restored?.gameId)
            assertEquals("psp", restored?.platformId)
            assertEquals(BrowserDownloadStatus.DOWNLOADING, restored?.status)
            assertEquals(128L, restored?.downloadedBytes)
            assertEquals(512L, restored?.totalBytes)
        } finally {
            assertTrue(
                "Could not restore prior isolated-test preference value",
                preferences.edit().putString("records", original).commit(),
            )
        }
    }
}
