package com.gamebox.os.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gamebox.os.data.CatalogDiscoveryRepository
import com.gamebox.os.source.ConfiguredDiscoverySyncResult
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType
import com.gamebox.os.source.VimmLairDiscoverySource
import com.gamebox.os.source.VimmLairPageNotFoundException
import com.gamebox.os.source.resolveBrowseUrl
import com.gamebox.os.source.searchVimmLairAcrossPlatforms
import com.gamebox.os.source.vimmLairSearchPlatforms
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Owns asynchronous Store discovery/source actions and their observable UI state. */
internal class CatalogDiscoveryActions(
    private val context: Context,
    private val repository: CatalogDiscoveryRepository,
    private val scope: CoroutineScope,
) {
    var configuredResults by mutableStateOf<List<DiscoverySourceGame>>(emptyList())
        private set
    var selectedConfiguredResult by mutableStateOf<DiscoverySourceGame?>(null)
        private set
    var configuredResultNotice by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var isSyncing by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    private var vimmSearchJob: Job? = null
    private var vimmSearchGeneration = 0L
    private var vimmSearchActive = false

    fun clearSearchResults() {
        cancelVimmSearch()
        configuredResults = emptyList()
        message = null
    }

    fun clearSelectedConfiguredResult() {
        selectedConfiguredResult = null
        configuredResultNotice = null
    }

    fun openSource(source: GameSourceConfig, query: String, console: StoreConsole?) {
        when (source.type) {
            GameSourceProviderType.VIMM_LAIR -> searchVimm(source, query, console)
            GameSourceProviderType.GAMEBOX_JSON -> syncJson(source)
            else -> openWebSource(source, query, console)
        }
    }

    private fun searchVimm(source: GameSourceConfig, query: String, console: StoreConsole?) {
        cancelVimmSearch()
        if (query.isBlank()) {
            message = "Enter a game title before searching " + source.name
            configuredResults = emptyList()
            return
        }
        val platforms = vimmLairSearchPlatforms(source, console?.label)
        if (platforms.isEmpty()) {
            message = if (console == null) {
                source.name + " has no supported consoles configured"
            } else {
                source.name + " is not available for " + console.label
            }
            configuredResults = emptyList()
            return
        }
        isSyncing = true
        progress = 0f
        vimmSearchActive = true
        val requestGeneration = ++vimmSearchGeneration
        val job = scope.launch(start = CoroutineStart.LAZY) {
            message = if (platforms.size == 1) {
                "Searching " + source.name + "…"
            } else {
                "Searching " + source.name + " across " + platforms.size + " consoles…"
            }
            try {
                val summary = searchVimmLairAcrossPlatforms(
                    config = source,
                    query = query,
                    selectedPlatform = console?.label,
                )
                if (requestGeneration != vimmSearchGeneration) return@launch
                configuredResults = summary.games
                progress = 1f
                val failedPlatforms = summary.failedPlatforms.joinToString { platform ->
                    summary.failureReasons[platform]?.let { "$platform ($it)" } ?: platform
                }
                message = when {
                    summary.games.isNotEmpty() && summary.failedPlatforms.isEmpty() ->
                        "Found " + summary.games.size + " result(s) from " + source.name
                    summary.games.isNotEmpty() ->
                        "Found " + summary.games.size + " result(s); failed: " +
                            failedPlatforms
                    summary.failedPlatforms.isNotEmpty() ->
                        source.name + " search failed for " + failedPlatforms
                    else -> "No " + source.name + " results matched “" + query + "”"
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (requestGeneration != vimmSearchGeneration) return@launch
                configuredResults = emptyList()
                progress = 1f
                message = source.name + " search failed: " +
                    (error.message?.take(180) ?: "unknown error")
            } finally {
                if (requestGeneration == vimmSearchGeneration) {
                    vimmSearchActive = false
                    vimmSearchJob = null
                    isSyncing = false
                }
            }
        }
        vimmSearchJob = job
        job.start()
    }

    private fun cancelVimmSearch() {
        if (vimmSearchJob == null && !vimmSearchActive) return
        vimmSearchGeneration += 1
        vimmSearchJob?.cancel()
        vimmSearchJob = null
        if (vimmSearchActive) {
            vimmSearchActive = false
            isSyncing = false
            progress = 0f
        }
    }

    private fun syncJson(source: GameSourceConfig) {
        isSyncing = true
        progress = 0f
        scope.launch {
            try {
                message = when (val result = repository.syncConfiguredSource(source)) {
                    is ConfiguredDiscoverySyncResult.Success ->
                        "Synced " + result.games + " games from " + source.name
                    is ConfiguredDiscoverySyncResult.Failed -> source.name + " sync failed: " + result.reason
                    ConfiguredDiscoverySyncResult.Disabled -> source.name + " is disabled in Settings"
                    ConfiguredDiscoverySyncResult.Unsupported -> source.name + " does not support in-app sync"
                }
                progress = 1f
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message = source.name + " sync failed: " + (error.message?.take(180) ?: "unknown error")
                progress = 1f
            } finally {
                isSyncing = false
            }
        }
    }

    private fun openWebSource(source: GameSourceConfig, query: String, console: StoreConsole?) {
        val consoleLabel = console?.label.orEmpty()
        val target = runCatching {
            if (query.isBlank() && consoleLabel.isBlank()) source.baseUrl
            else source.resolveBrowseUrl(query, consoleLabel)
        }.getOrElse { error ->
            message = error.message ?: "Configured source URL is invalid"
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, target.toUri()))
        } catch (_: ActivityNotFoundException) {
            message = "No browser is available to open " + source.name
        }
    }

    fun openResult(result: DiscoverySourceGame, sources: List<GameSourceConfig>) {
        val source = sources.firstOrNull { it.id.equals(result.sourceId, ignoreCase = true) }
        if (source?.type != GameSourceProviderType.VIMM_LAIR) {
            selectedConfiguredResult = result
            return
        }
        isSyncing = true
        configuredResultNotice = null
        message = "Loading " + source.name + " details…"
        scope.launch {
            try {
                val hydrated = VimmLairDiscoverySource(source).hydrate(result)
                val thumbnails = com.gamebox.os.catalog.LibretroThumbnailClient()
                    .lookup(hydrated.platform, hydrated.title, hydrated.region)
                selectedConfiguredResult = hydrated.copy(
                    coverUrl = hydrated.coverUrl ?: thumbnails.boxartUrl,
                    screenshotUrl = thumbnails.screenshotUrl,
                )
                configuredResultNotice = null
                message = "Loaded details from " + source.name
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: VimmLairPageNotFoundException) {
                configuredResultNotice = configuredSourceNotFoundMessage(source.name)
                message = configuredResultNotice.orEmpty()
                // Keep the game's /vault/{id} link; the letter-list page is not a download target.
                selectedConfiguredResult = result
            } catch (_: Exception) {
                configuredResultNotice = configuredSourceDetailFailureMessage(source.name)
                message = configuredResultNotice.orEmpty()
                val thumbnails = runCatching {
                    com.gamebox.os.catalog.LibretroThumbnailClient()
                        .lookup(result.platform, result.title, result.region)
                }.getOrNull()
                selectedConfiguredResult = result.copy(
                    coverUrl = result.coverUrl ?: thumbnails?.boxartUrl,
                    screenshotUrl = result.screenshotUrl ?: thumbnails?.screenshotUrl,
                )
            } finally {
                isSyncing = false
            }
        }
    }

}
