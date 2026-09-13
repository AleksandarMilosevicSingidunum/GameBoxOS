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
import com.gamebox.os.data.local.DatabaseMigrationBackupManager
import com.gamebox.os.data.local.GAMEBOX_DATABASE_VERSION
import com.gamebox.os.data.local.MIGRATION_1_2
import com.gamebox.os.data.local.MIGRATION_2_3
import com.gamebox.os.data.local.MIGRATION_3_4
import com.gamebox.os.data.local.MIGRATION_4_5
import com.gamebox.os.data.local.MIGRATION_5_6
import com.gamebox.os.data.local.MIGRATION_6_7
import com.gamebox.os.data.local.MIGRATION_7_8
import com.gamebox.os.data.local.MIGRATION_8_9
import com.gamebox.os.data.local.MIGRATION_9_10
import com.gamebox.os.data.local.MIGRATION_10_11
import com.gamebox.os.data.local.MIGRATION_11_12
import com.gamebox.os.data.local.RoomLaunchSessionJournal
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
import com.gamebox.os.storage.DirectorySaveAdapter
import com.gamebox.os.storage.SaveAdapterRegistry
import com.gamebox.os.storage.SaveBackupCoordinator
import com.gamebox.os.storage.SaveBackupService
import com.gamebox.os.storage.SaveSnapshotManifestStore
import com.gamebox.os.storage.ExternalLibraryContentStore
import com.gamebox.os.importer.AuthorizedRomImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

interface AppContainer {
    val managedSaveDiscovery: com.gamebox.os.storage.ManagedSaveDiscovery
    val gameRepository: GameRepository
    val settingsRepository: SettingsRepository
    val downloadRepository: DownloadRepository
    val authorizedDownloadController: AuthorizedDownloadController
    val remoteDownloadController: RemoteDownloadController
    val gameLaunchController: GameLaunchController
    val saveSafetyController: SaveSafetyController
    fun createSaveSafetyController(gameId: com.gamebox.os.domain.GameId,
        scope: CoroutineScope): SaveSafetyController {
        require(gameId.value == "galaxy-patrol") { "Per-game save controls are not configured" }
        return saveSafetyController
    }
    val catalogDiscoverySync: TheGamesDbCatalogSync
    val catalogDiscoveryRepository: CatalogDiscoveryRepository
    val authorizedRomImporter: AuthorizedRomImporter
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val databaseName = "gamebox.db"
    private val preMigrationBackup = DatabaseMigrationBackupManager(
        applicationContext.filesDir.resolve("database-backups")
    ).createIfNeeded(
        applicationContext.getDatabasePath(databaseName),
        GAMEBOX_DATABASE_VERSION,
    )
    private val database = Room.databaseBuilder(
        applicationContext,
        GameBoxDatabase::class.java,
        databaseName
    ).addMigrations(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
        MIGRATION_10_11, MIGRATION_11_12,
    ).build()
    override val settingsRepository = SettingsRepository(applicationContext)
    private val settingsState = settingsRepository.settings.stateIn(
        applicationScope,
        SharingStarted.Eagerly,
        com.gamebox.os.settings.GameBoxSettings(),
    )
    private val externalLibraryContent = ExternalLibraryContentStore(applicationContext) {
        settingsState.value.externalLibraryUri
    }
    override val authorizedRomImporter = AuthorizedRomImporter(applicationContext)
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

    override val managedSaveDiscovery = com.gamebox.os.storage.ManagedSaveDiscovery(
        gameRepository,
        com.gamebox.os.storage.DirectorySaveAdapter(applicationContext.filesDir.resolve("saves")),
        applicationScope,
    )

    override val remoteDownloadController: RemoteDownloadController =
        WorkManagerRemoteDownloadController(
            applicationContext,
            gameRepository,
            downloadRepository,
            applicationScope,
            downloadsUnmeteredOnly = { settingsState.value.downloadsUnmeteredOnly },
        )

    override val authorizedDownloadController: AuthorizedDownloadController =
        WorkManagerAuthorizedDownloadController(applicationContext, gameRepository, applicationScope)

    private val automaticSaveBackup = SaveBackupCoordinator(
        registry = SaveAdapterRegistry(
            mapOf("*" to DirectorySaveAdapter(applicationContext.filesDir.resolve("saves")))
        ),
        backupService = SaveBackupService(
            applicationContext.filesDir.resolve("saves"),
            applicationContext.filesDir.resolve("save-backups"),
        ),
        manifestStore = SaveSnapshotManifestStore(
            applicationContext.filesDir.resolve("save-backup-manifests")
        ),
    )

    override val gameLaunchController: GameLaunchController = DefaultGameLaunchController(
        EmulatorCapabilityRegistry(),
        AndroidPackageGateway(applicationContext, externalContent = externalLibraryContent),
        gameRepository,
        sessionJournal = RoomLaunchSessionJournal(database.launchSessionDao()),
        platformEmulatorDefault = settingsRepository::platformEmulatorDefault,
        backupAfterSession = { gameId ->
            gameRepository.game(gameId)?.let { game ->
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    automaticSaveBackup.backup(game.platform, game.id.value)
                }
            }
        },
    )

    override val saveSafetyController: SaveSafetyController = DefaultSaveSafetyController(
        applicationContext, database.saveRecordDao(), gameRepository, applicationScope, settingsRepository,
        externalContent = externalLibraryContent,
    )

    override fun createSaveSafetyController(gameId: com.gamebox.os.domain.GameId,
        scope: CoroutineScope): SaveSafetyController = DefaultSaveSafetyController(
        applicationContext, database.saveRecordDao(), gameRepository,
        CoroutineScope(scope.coroutineContext + Dispatchers.IO), settingsRepository, gameId,
        externalContent = externalLibraryContent,
    )
}

internal fun isNetworkAvailable(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

