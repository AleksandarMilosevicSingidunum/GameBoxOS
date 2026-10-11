package com.gamebox.os.data

import android.content.Context
import com.gamebox.os.catalog.AssetCatalogProvider
import com.gamebox.os.catalog.CatalogCredentialStore
import com.gamebox.os.catalog.CatalogProvider
import com.gamebox.os.catalog.CatalogProviderFactory
import com.gamebox.os.catalog.ConfiguredCatalogProvider
import com.gamebox.os.catalog.HttpsCatalogProvider
import com.gamebox.os.catalog.LibretroThumbnailClient
import com.gamebox.os.catalog.SelectingCatalogProvider
import com.gamebox.os.catalog.WikidataMetadataClient
import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.settings.SettingsRepository

/** Owns the provider graph and discovery sync that back the Store catalog. */
internal class CatalogRuntime(
    context: Context,
    settings: SettingsRepository,
    discoveryDao: CatalogDiscoveryDao,
) {
    private val applicationContext = context.applicationContext
    private val credentialStore = object : CatalogCredentialStore {
        override fun credentials(key: String) = settings.catalogCredentials(key)
    }
    private val configuredTransportProvider = CatalogProviderFactory(
        credentials = credentialStore,
    ).create(settings::catalogProviderConfig)
    private val selectedRemoteProvider = SelectingCatalogProvider(
        selected = settings::catalogTransport,
        providers = mapOf(
            "HTTPS" to HttpsCatalogProvider(
                applicationContext,
                settings::catalogUrl,
                credentialStore = credentialStore,
                credentialKey = {
                    "catalog-https".takeIf {
                        credentialStore.credentials("catalog-https") != null
                    }
                },
            ),
            "WEBDAV" to configuredTransportProvider,
            "S3" to configuredTransportProvider,
        ),
    )
    private val baseProvider = ConfiguredCatalogProvider(
        fallback = AssetCatalogProvider(applicationContext),
        remote = selectedRemoteProvider,
        configuredUrl = settings::catalogUrl,
        networkAvailable = { isNetworkAvailable(applicationContext) },
    )
    private val metadataClient = WikidataMetadataClient()
    private val thumbnailClient = LibretroThumbnailClient()

    suspend fun enrichGameMetadata(game: com.gamebox.os.domain.Game): com.gamebox.os.domain.Game =
        metadataClient.enrich(thumbnailClient.enrich(game))

    suspend fun screenshotsFor(game: com.gamebox.os.domain.Game): List<String> =
        thumbnailClient.lookup(game.platform, game.providerTitle, game.region).screenshotUrl
            ?.let(::listOf)
            .orEmpty()

    val provider: CatalogProvider = baseProvider
    val discoveryRepository = RoomCatalogDiscoveryRepository(discoveryDao)
}
