package com.gamebox.os.storage

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.StandardCharsets

/**
 * Exercises the real DocumentsProvider selected in Settings, using only a disposable
 * emulator fixture. It is intentionally skipped when no save root has been selected.
 */
@RunWith(AndroidJUnit4::class)
class SafSaveAdapterDeviceTest {
    @Test
    fun configuredSaveRootRoundTripsARealDocument() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val settings = SettingsRepository(context).settings.first()
        assumeTrue("Select an emulator save root before running this provider test", settings.emulatorSaveRootUri.isNotBlank())

        val adapter = SafSaveAdapter(context, treeUri = { settings.emulatorSaveRootUri })
        val artifact = adapter.discover("game-a")
            .singleOrNull { it.relativePath == "game-a/PSP/SAVEDATA/slot.dat" }
        assumeTrue("Disposable game-a fixture is not present in the selected save root", artifact != null)
        requireNotNull(artifact)

        val original = adapter.openArtifact(artifact).use { it.readBytes() }
        assertEquals("emulator-save-v1\n", original.toString(StandardCharsets.UTF_8))

        try {
            adapter.restoreArtifact(artifact, "emulator-save-v2".byteInputStream())
            val restored = adapter.openArtifact(artifact).use { it.readBytes() }
            assertEquals("emulator-save-v2", restored.toString(StandardCharsets.UTF_8))
            assertTrue(restored.isNotEmpty())
            assertTrue(
                adapter.discover("game-a").none {
                    it.relativePath.contains(".gamebox-restore-backup-")
                },
            )
        } finally {
            adapter.restoreArtifact(artifact, original.inputStream())
        }

        val finalBytes = adapter.openArtifact(artifact).use { it.readBytes() }
        assertEquals(original.toList(), finalBytes.toList())
        assertTrue(
            adapter.discover("game-a").none {
                it.relativePath.contains(".gamebox-restore-backup-")
            },
        )
    }
}
