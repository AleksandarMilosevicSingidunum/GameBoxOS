package com.gamebox.os

import android.app.Application
import com.gamebox.os.data.AppContainer
import com.gamebox.os.data.DefaultAppContainer
import com.gamebox.os.storage.GameStorageTree
import com.gamebox.os.importer.BrowserDownloadCoordinator
import com.gamebox.os.importer.BrowserDownloadStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GameBoxApplication : Application() {
    lateinit var container: AppContainer
        private set
    lateinit var browserDownloadStore: BrowserDownloadStore
        private set
    lateinit var browserDownloadCoordinator: BrowserDownloadCoordinator
        private set
    private lateinit var browserDownloadScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        GameStorageTree.ensure(filesDir)
        container = DefaultAppContainer(this)
        browserDownloadStore = BrowserDownloadStore(this)
        browserDownloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        browserDownloadCoordinator = BrowserDownloadCoordinator(
            context = this,
            store = browserDownloadStore,
            importer = container.authorizedRomImporter,
            repository = container.gameRepository,
            scope = browserDownloadScope,
        ).also(BrowserDownloadCoordinator::start)
    }
}
