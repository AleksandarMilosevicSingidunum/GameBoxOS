package com.gamebox.os.data

import android.content.Context
import androidx.room.Room
import com.gamebox.os.catalog.AssetCatalogProvider
import com.gamebox.os.data.local.GameBoxDatabase
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val gameRepository: GameRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = Room.databaseBuilder(
        applicationContext,
        GameBoxDatabase::class.java,
        "gamebox.db"
    ).build()
    private val catalogProvider = AssetCatalogProvider(applicationContext)

    override val settingsRepository = SettingsRepository(applicationContext)

    override val gameRepository: GameRepository = RoomGameRepository(
        dao = database.gameDao(),
        catalogProvider = catalogProvider,
        scope = applicationScope,
        onCatalogSeeded = settingsRepository::markCatalogSeeded
    )
}
