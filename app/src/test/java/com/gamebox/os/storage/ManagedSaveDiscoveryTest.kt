package com.gamebox.os.storage

import com.gamebox.os.data.FakeGameRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ManagedSaveDiscoveryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun discoversChangesAndRefreshesRemovedFilesWithoutWritingThem() = runBlocking {
        val repository = FakeGameRepository()
        val id = repository.observeGames().value.first().id.value
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val discovery = ManagedSaveDiscovery(repository, DirectorySaveAdapter(temporary.root), scope)
            withTimeout(5_000) { discovery.state.first { it[id]?.presence == SavePresence.NONE } }
            val file = temporary.root.resolve("$id/save.dat")
            file.parentFile.mkdirs()
            file.writeText("real file bytes")
            discovery.refresh()
            val present = withTimeout(5_000) { discovery.state.first { it[id]?.presence == SavePresence.PRESENT } }
            assertEquals(file.length(), present[id]?.totalBytes)
            assertEquals(1, present[id]?.artifactCount)
            assertEquals("real file bytes", file.readText())
            file.delete()
            discovery.refresh()
            withTimeout(5_000) { discovery.state.first { it[id]?.presence == SavePresence.NONE } }
        } finally { scope.coroutineContext[Job]?.cancelAndJoin() }
    }

    @Test fun failedInspectionDoesNotClaimNoSaves() = runBlocking {
        val repository = FakeGameRepository()
        val id = repository.observeGames().value.first().id.value
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val discovery = ManagedSaveDiscovery(repository, SaveAdapter { throw java.io.IOException("unreadable") }, scope)
            withTimeout(5_000) { discovery.state.first { it[id]?.presence == SavePresence.ERROR } }
        } finally { scope.coroutineContext[Job]?.cancelAndJoin() }
    }
}
