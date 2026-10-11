package com.gamebox.os.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gamebox.os.data.local.GameBoxDatabaseRuntime
import com.gamebox.os.data.local.RoomLaunchSessionJournal
import com.gamebox.os.data.local.toDomain
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
import com.gamebox.os.storage.ExternalLibraryContentStore
import com.gamebox.os.importer.AuthorizedRomImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface AppContainer {
    val managedSaveDiscovery: com.gamebox.os.storage.ManagedSaveDiscovery
    val catalogProvider: com.gamebox.os.catalog.CatalogProvider
    val gameRepository: GameRepository
    val settingsRepository: SettingsRepository
    val downloadRepository: DownloadRepository
    val authorizedDownloadController: AuthorizedDownloadController
    val remoteDownloadController: RemoteDownloadController
    val gameLaunchController: GameLaunchController
    val saveSafetyController: SaveSafetyController
    fun createSaveSafetyController(
        gameId: com.gamebox.os.domain.GameId,
        scope: CoroutineScope,
    ): SaveSafetyController
    val catalogDiscoveryRepository: CatalogDiscoveryRepository
    val authorizedRomImporter: AuthorizedRomImporter
    suspend fun gameScreenshots(game: com.gamebox.os.domain.Game): List<String>
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationContext = context.applicationContext
    private val processStartedAtMillis = System.currentTimeMillis()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = GameBoxDatabaseRuntime(applicationContext).database
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
    private val catalogRuntime = CatalogRuntime(
        context = applicationContext,
        settings = settingsRepository,
        discoveryDao = database.catalogDiscoveryDao(),
    )
    override val catalogDiscoveryRepository = catalogRuntime.discoveryRepository
    override val catalogProvider = catalogRuntime.provider
    override suspend fun gameScreenshots(game: com.gamebox.os.domain.Game): List<String> =
        catalogRuntime.screenshotsFor(game)

    override val downloadRepository: DownloadRepository =
        RoomDownloadRepository(database.downloadJobDao(), applicationScope)

    override val gameRepository: GameRepository = RoomGameRepository(
        dao = database.gameDao(),
        saveRecordDao = database.saveRecordDao(),
        catalogProvider = catalogProvider,
        scope = applicationScope,
        onCatalogSeeded = settingsRepository::markCatalogSeeded,
        onCatalogRefreshed = settingsRepository::markCatalogRefreshed,
        enrichGameMetadata = catalogRuntime::enrichGameMetadata,
    )

    private val importRegistrationRecovery = applicationScope.launch {
        authorizedRomImporter.reconcilePendingRegistrations(processStartedAtMillis) { gameId ->
            database.gameDao().getById(gameId.value)?.toDomain()
        }
    }

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
        WorkManagerAuthorizedDownloadController(
            applicationContext, gameRepository, downloadRepository, applicationScope
        )

    private val gameSaveSessionBackups = GameSaveSessionBackups(
        context = applicationContext,
        emulatorSaveTreeUri = { settingsState.value.emulatorSaveRootUri },
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
                    gameSaveSessionBackups.backup(game.platform, game.id.value)
                }
            }
        },
        restoreBeforeSession = { gameId ->
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                gameSaveSessionBackups.restoreLatest(gameId.value)
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

