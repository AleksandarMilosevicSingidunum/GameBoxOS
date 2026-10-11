package com.gamebox.os.settings

import androidx.test.core.app.ApplicationProvider
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryConcurrencyTest {
    @Test
    fun concurrentGameSourceUpsertsBothSurvive() = runBlocking {
        val repository = SettingsRepository(ApplicationProvider.getApplicationContext())
        val original = repository.settings.first().gameSources
        try {
            repository.setGameSources(emptyList())
            coroutineScope {
                val first = async(Dispatchers.Default) { repository.upsertGameSource(source("concurrency-a")) }
                val second = async(Dispatchers.Default) { repository.upsertGameSource(source("concurrency-b")) }
                first.await()
                second.await()
            }
            val ids = repository.settings.first().gameSources.map { it.id }.toSet()
            assertTrue("first update should survive", "concurrency-a" in ids)
            assertTrue("second update should survive", "concurrency-b" in ids)
        } finally {
            repository.setGameSources(original)
        }
    }

    private fun source(id: String) = GameSourceConfig(
        id = id,
        name = id,
        type = GameSourceProviderType.EXTERNAL_WEB,
        baseUrl = "https://example.org",
    )
}
