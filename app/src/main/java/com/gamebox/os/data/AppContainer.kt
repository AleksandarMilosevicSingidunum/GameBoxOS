package com.gamebox.os.data

import android.content.Context
import androidx.room.Room
import com.gamebox.os.catalog.AssetCatalogProvider
import com.gamebox.os.data.local.GameBoxDatabase
import com.gamebox.os.data.local.MIGRATION_1_2
import com.gamebox.os.data.local.MIGRATION_2_3
import com.gamebox.os.download.AuthorizedDownloadController
import com.gamebox.os.download.WorkManagerAuthorizedDownloadController
import com.gamebox.os.settings.SettingsRepository
import com.gamebox.os.launch.AndroidPackageGateway
import com.gamebox.os.launch.DefaultGameLaunchController
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import com.gamebox.os.launch.GameLaunchController
import com.gamebox.os.storage.DefaultSaveSafetyController
import com.gamebox.os.storage.SaveSafetyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val gameRepository: GameRepository
    val settingsRepository: SettingsRepository
    val downloadRepository: DownloadRepository
    val authorizedDownloadController: AuthorizedDownloadController
    val gameLaunchController: GameLaunchController
    val saveSafetyController: SaveSafetyController
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = Room.databaseBuilder(
        applicationContext,
        GameBoxDatabase::class.java,
        "gamebox.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    private val catalogProvider = AssetCatalogProvider(applicationContext)

    override val settingsRepository = SettingsRepository(applicationContext)

    override val downloadRepository: DownloadRepository =
        RoomDownloadRepository(database.downloadJobDao(), applicationScope)

    override val gameRepository: GameRepository = RoomGameRepository(
        dao = database.gameDao(),
        catalogProvider = catalogProvider,
        scope = applicationScope,
        onCatalogSeeded = settingsRepository::markCatalogSeeded,
        onCatalogRefreshed = settingsRepository::markCatalogRefreshed
    )

    override val authorizedDownloadController: AuthorizedDownloadController =
        WorkManagerAuthorizedDownloadController(
            applicationContext,
            gameRepository,
            applicationScope
        )

    override val gameLaunchController: GameLaunchController = DefaultGameLaunchController(
        EmulatorCapabilityRegistry(),
        AndroidPackageGateway(applicationContext),
        gameRepository
    )

    override val saveSafetyController: SaveSafetyController = DefaultSaveSafetyController(
        applicationContext,
        database.saveRecordDao(),
        gameRepository,
        applicationScope
    )
}
