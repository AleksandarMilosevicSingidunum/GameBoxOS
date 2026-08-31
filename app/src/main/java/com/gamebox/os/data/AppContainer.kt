package com.gamebox.os.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import com.gamebox.os.catalog.AssetCatalogProvider
import com.gamebox.os.catalog.ConfiguredCatalogProvider
import com.gamebox.os.catalog.HttpsCatalogProvider
import com.gamebox.os.catalog.MetadataEnrichingCatalogProvider
import com.gamebox.os.catalog.TheGamesDbMetadataClient
import com.gamebox.os.catalog.TheGamesDbCatalogSync
import com.gamebox.os.catalog.HttpsTheGamesDbCatalogTransport
import com.gamebox.os.data.local.GameBoxDatabase
import com.gamebox.os.data.local.MIGRATION_1_2
import com.gamebox.os.data.local.MIGRATION_2_3
import com.gamebox.os.data.local.MIGRATION_3_4
import com.gamebox.os.data.local.MIGRATION_4_5
import com.gamebox.os.data.local.MIGRATION_5_6
import com.gamebox.os.data.local.MIGRATION_6_7
import com.gamebox.os.data.local.MIGRATION_7_8
import com.gamebox.os.download.AuthorizedDownloadController
import com.gamebox.os.download.WorkManagerAuthorizedDownloadController
import com.gamebox.os.download.RemoteDownloadController
import com.gamebox.os.download.WorkManagerRemoteDownloadController
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
    val remoteDownloadController: RemoteDownloadController
    val gameLaunchController: GameLaunchController
    val saveSafetyController: SaveSafetyController
    val catalogDiscoverySync: TheGamesDbCatalogSync
    val catalogDiscoveryRepository: CatalogDiscoveryRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = Room.databaseBuilder(
        applicationContext,
        GameBoxDatabase::class.java,
        "gamebox.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build()
    override val settingsRepository = SettingsRepository(applicationContext)
    private val assetCatalogProvider = AssetCatalogProvider(applicationContext)
    private val configuredCatalogProvider = ConfiguredCatalogProvider(
        fallback = assetCatalogProvider,
        remote = HttpsCatalogProvider(applicationContext, settingsRepository::catalogUrl),
        configuredUrl = settingsRepository::catalogUrl,
        networkAvailable = { isNetworkAvailable(applicationContext) }
    )
    private val metadataClient = TheGamesDbMetadataClient(settingsRepository::theGamesDbApiKey)
    override val catalogDiscoverySync = TheGamesDbCatalogSync(
        apiKey = settingsRepository::theGamesDbApiKey,
        transport = HttpsTheGamesDbCatalogTransport(),
        dao = database.catalogDiscoveryDao(),
    )

    override val catalogDiscoveryRepository: CatalogDiscoveryRepository =
        RoomCatalogDiscoveryRepository(database.catalogDiscoveryDao(), catalogDiscoverySync)

    private val catalogProvider = MetadataEnrichingCatalogProvider(
        base = configuredCatalogProvider,
        enrich = metadataClient::enrich,
    )

    override val downloadRepository: DownloadRepository =
        RoomDownloadRepository(database.downloadJobDao(), applicationScope)

    override val gameRepository: GameRepository = RoomGameRepository(
        dao = database.gameDao(),
        saveRecordDao = database.saveRecordDao(),
        catalogProvider = catalogProvider,
        scope = applicationScope,
        onCatalogSeeded = settingsRepository::markCatalogSeeded,
        onCatalogRefreshed = settingsRepository::markCatalogRefreshed
    )

    override val remoteDownloadController: RemoteDownloadController =
        WorkManagerRemoteDownloadController(applicationContext, gameRepository, downloadRepository, applicationScope)

    override val authorizedDownloadController: AuthorizedDownloadController =
        WorkManagerAuthorizedDownloadController(applicationContext, gameRepository, applicationScope)

    override val gameLaunchController: GameLaunchController = DefaultGameLaunchController(
        EmulatorCapabilityRegistry(), AndroidPackageGateway(applicationContext), gameRepository
    )

    override val saveSafetyController: SaveSafetyController = DefaultSaveSafetyController(
        applicationContext, database.saveRecordDao(), gameRepository, applicationScope
    )
}

internal fun isNetworkAvailable(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
