package com.gamebox.os.ui

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.Settings
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.gamebox.os.display.externalDisplayActivityResultOptions
import com.gamebox.os.tvmode.TvChannel
import com.gamebox.os.tvmode.TvLaunchableApp
import com.gamebox.os.tvmode.TvProgramme
import com.gamebox.os.tvmode.TvStream
import com.gamebox.os.tvmode.TvWebDestination
import com.gamebox.os.tvmode.activeTvPlaylistLabel
import com.gamebox.os.display.toGameBoxFullscreenLaunchBundle
import com.gamebox.os.settings.SettingsRepository
import com.gamebox.os.tvmode.discoverTvLaunchableApps
import com.gamebox.os.tvmode.displayTvChannelName
import com.gamebox.os.tvmode.fetchIptvOrgPlaylist
import com.gamebox.os.tvmode.fetchCurrentXmltvProgrammes
import com.gamebox.os.tvmode.freeEpgCountryGuideUrl
import com.gamebox.os.tvmode.iptvOrgPlaylistCountryCode
import com.gamebox.os.tvmode.iptvOrgCountryPlaylistUrl
import com.gamebox.os.tvmode.hasCurrentTvProgramme
import com.gamebox.os.tvmode.inAppTvWebDestination
import com.gamebox.os.tvmode.nextTvStreamIndex
import com.gamebox.os.tvmode.retryTvStreamIndex
import com.gamebox.os.tvmode.officialRtsPlanetaLiveUrl
import com.gamebox.os.tvmode.orderTvApps
import com.gamebox.os.tvmode.orderTvChannelsForPlaylist
import com.gamebox.os.tvmode.parseAuthorizedM3u
import com.gamebox.os.tvmode.playlistGuideUrl
import com.gamebox.os.tvmode.playlistGrantToRelease
import com.gamebox.os.tvmode.playlistUriAfterLoad
import com.gamebox.os.tvmode.readPlaylist
import com.gamebox.os.tvmode.tvChannelKey
import com.gamebox.os.tvmode.tvPlaybackErrorMessage
import com.gamebox.os.tvmode.currentAndNextProgrammes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal enum class TvRemoteTextTarget { APP_SEARCH, CHANNEL_SEARCH, COUNTRY_CODE }

internal data class TvRemoteTextInput(val target: TvRemoteTextTarget, val text: String?)

internal fun applyTvRemoteTextInput(
    current: String,
    activeTarget: TvRemoteTextTarget?,
    input: TvRemoteTextInput,
): String? {
    if (input.target != activeTarget) return null
    val updated = input.text?.let { current + it } ?: run {
        val end = current.length - if (
            current.lastOrNull()?.isLowSurrogate() == true &&
            current.getOrNull(current.lastIndex - 1)?.isHighSurrogate() == true
        ) 2 else 1
        current.take(end.coerceAtLeast(0))
    }
    return if (activeTarget == TvRemoteTextTarget.COUNTRY_CODE) {
        updated.filter(Char::isLetter).take(2).uppercase()
    } else updated
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
internal fun TvModeScreen(
    externalDisplay: Boolean,
    settingsRepository: SettingsRepository,
    bottomOverlayInset: Dp,
    favoritePackages: List<String>,
    playlistUri: String?,
    favoriteChannelKeys: List<String>,
    lastChannelKey: String?,
    homeSections: List<String>,
    remoteSearchTarget: TvRemoteTextTarget?,
    remoteTextInputs: Flow<TvRemoteTextInput>,
    onRemoteSearchTargetChanged: (TvRemoteTextTarget?) -> Unit,
    onFavoritesChanged: (List<String>) -> Unit,
    onPlaylistChanged: (String?) -> Unit,
    onFavoriteChannelsChanged: (List<String>) -> Unit,
    onLastChannelChanged: (String?) -> Unit,
    onHomeSectionsChanged: (List<String>) -> Unit,
    onReturnToGameBox: () -> Unit,
    onSettings: () -> Unit,
    onRemoteSetup: () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val firstAppFocusRequester = remember { FocusRequester() }
    val homeFocusRequester = remember { FocusRequester() }
    val homeFocusClaimed = remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<TvLaunchableApp>>(emptyList()) }
    var remotePaired by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var playlistChannels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var playlistLoading by remember { mutableStateOf(false) }
    var playlistError by remember { mutableStateOf<String?>(null) }
    var playbackMessage by remember { mutableStateOf<String?>(null) }
    var activeChannel by remember { mutableStateOf<TvChannel?>(null) }
    var embeddedWebDestination by remember { mutableStateOf<TvWebDestination?>(null) }
    var iptvCountryCode by remember(playlistUri) {
        mutableStateOf(iptvOrgPlaylistCountryCode(playlistUri.orEmpty())?.uppercase() ?: "RS")
    }
    var showAppSearchKeyboard by remember { mutableStateOf(false) }
    var showCountryKeyboard by remember { mutableStateOf(false) }
    var showChannelSearchKeyboard by remember { mutableStateOf(false) }
    LaunchedEffect(remoteSearchTarget) {
        if (remoteSearchTarget == null) {
            showAppSearchKeyboard = false
            showCountryKeyboard = false
            showChannelSearchKeyboard = false
        }
    }
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var showAllChannels by remember { mutableStateOf(false) }
    var showHomeOptions by remember { mutableStateOf(false) }
    var channelQuery by remember { mutableStateOf("") }
    var managingApps by remember { mutableStateOf(false) }
    var appDrawerMode by remember { mutableStateOf(false) }
    var guideProgrammes by remember { mutableStateOf<List<TvProgramme>>(emptyList()) }
    var guideLoading by remember { mutableStateOf(false) }
    var guideError by remember { mutableStateOf<String?>(null) }
    var guideRetryAvailable by remember { mutableStateOf(false) }
    var guideRetryCount by remember { mutableIntStateOf(0) }
    var loadedPlaylistUri by remember { mutableStateOf<String?>(null) }
    var pendingPlaylistUri by remember { mutableStateOf<String?>(null) }
    var playlistReplacementError by remember { mutableStateOf<String?>(null) }
    fun releasePlaylistGrant(uri: String) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
    fun changePlaylist(uri: String?) {
        if (uri == pendingPlaylistUri) return
        if (uri == playlistUri) {
            playlistGrantToRelease(pendingPlaylistUri, playlistUri)?.let { releasePlaylistGrant(it) }
            pendingPlaylistUri = null
            return
        }
        playlistGrantToRelease(pendingPlaylistUri, uri)?.let { releasePlaylistGrant(it) }
        pendingPlaylistUri = uri
        playlistReplacementError = null
        playlistError = null
    }
    val playlistPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onSuccess { changePlaylist(uri.toString()) }
                .onFailure { playlistReplacementError = it.message ?: "Playlist access could not be saved" }
        }
    }
    val playlistPickerOptions = remember(context) { externalDisplayActivityResultOptions(context) }
    // Settings can arrive after TV Mode opens; don't persist this fallback over a saved playlist.
    val effectivePlaylistUri = pendingPlaylistUri ?: playlistUri ?: iptvOrgCountryPlaylistUrl("RS")
    LaunchedEffect(playlistUri, pendingPlaylistUri) {
        if (pendingPlaylistUri != null && playlistUri == pendingPlaylistUri) pendingPlaylistUri = null
    }

    LaunchedEffect(context, managingApps) {
        if (!managingApps && apps.isNotEmpty()) return@LaunchedEffect
        loadError = null
        apps = withContext(Dispatchers.IO) {
            runCatching { discoverTvLaunchableApps(context.packageManager, context.packageName) }
                .onFailure { loadError = it.message ?: "Installed apps could not be loaded" }
                .getOrDefault(emptyList())
        }
    }
    LaunchedEffect(settingsRepository) {
        while (true) {
            remotePaired = runCatching { settingsRepository.remoteControlToken() != null }.getOrDefault(false)
            kotlinx.coroutines.delay(3_000)
        }
    }
    LaunchedEffect(apps, managingApps) {
        if (apps.isNotEmpty() && managingApps) runCatching { firstAppFocusRequester.requestFocus() }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            kotlinx.coroutines.delay(15_000)
        }
    }
    LaunchedEffect(remoteTextInputs, remoteSearchTarget) {
        remoteTextInputs.collect { input ->
            when (input.target) {
                TvRemoteTextTarget.APP_SEARCH -> query = applyTvRemoteTextInput(query, remoteSearchTarget, input) ?: query
                TvRemoteTextTarget.CHANNEL_SEARCH -> channelQuery = applyTvRemoteTextInput(channelQuery, remoteSearchTarget, input) ?: channelQuery
                TvRemoteTextTarget.COUNTRY_CODE -> iptvCountryCode = applyTvRemoteTextInput(iptvCountryCode, remoteSearchTarget, input) ?: iptvCountryCode
            }
        }
    }
    LaunchedEffect(effectivePlaylistUri, guideRetryCount) {
        val isReplacement = pendingPlaylistUri == effectivePlaylistUri
        val refreshGuideOnly = guideRetryCount > 0 && loadedPlaylistUri == effectivePlaylistUri
        if (!isReplacement && !refreshGuideOnly) {
            playlistChannels = emptyList()
            guideProgrammes = emptyList()
        }
        playlistError = null
        playlistLoading = true
        guideLoading = true
        guideError = null
        if (!refreshGuideOnly) guideRetryAvailable = false
        while (true) {
            playlistLoading = true
            guideLoading = true
            guideError = null
            val loaded = try {
                val (channels, guide, guideChannels) = withContext(Dispatchers.IO) {
                    val text = if (effectivePlaylistUri.startsWith("https://")) {
                        fetchIptvOrgPlaylist(effectivePlaylistUri)
                    } else {
                        context.contentResolver.openInputStream(Uri.parse(effectivePlaylistUri))
                            ?.buffered()?.use(::readPlaylist)
                            ?: error("The selected playlist is unavailable")
                    }
                    val channels = orderTvChannelsForPlaylist(effectivePlaylistUri, parseAuthorizedM3u(text))
                    require(channels.isNotEmpty()) { "No supported channels found in this playlist" }
                    val guide = playlistGuideUrl(text)
                        ?: iptvOrgPlaylistCountryCode(effectivePlaylistUri)?.let(::freeEpgCountryGuideUrl)
                    val guideChannels = channels.mapNotNull { channel -> channel.guideId?.let { it to channel.name } }.toMap()
                    Triple(channels, guide, guideChannels)
                }
                playlistChannels = channels
                loadedPlaylistUri = effectivePlaylistUri
                playlistError = null
                val selectedUri = playlistUriAfterLoad(playlistUri, pendingPlaylistUri, effectivePlaylistUri, channels.isNotEmpty())
                if (selectedUri != null && selectedUri != playlistUri && selectedUri == pendingPlaylistUri) {
                    playlistGrantToRelease(playlistUri, selectedUri)?.let(::releasePlaylistGrant)
                    onPlaylistChanged(selectedUri)
                    playlistReplacementError = null
                }
                guideRetryAvailable = guide != null && guideChannels.isNotEmpty()
                val programmes = if (guide != null && guideChannels.isNotEmpty()) {
                    try {
                        withContext(Dispatchers.IO) {
                            fetchCurrentXmltvProgrammes(guide, guideChannels, java.time.Instant.now())
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        guideError = failure.message ?: "Programme guide could not be loaded"
                        emptyList()
                    }
                } else {
                    guideError = "This playlist does not provide a programme guide."
                    emptyList()
                }
                if (programmes.isEmpty() && guideRetryAvailable && guideError == null) {
                    guideError = "No programme data was returned by the guide source."
                }
                channels to programmes
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (isReplacement) {
                    playlistGrantToRelease(effectivePlaylistUri, playlistUri)?.let(::releasePlaylistGrant)
                    playlistReplacementError = "Playlist not changed: ${failure.message ?: "could not be read"}"
                    pendingPlaylistUri = null
                } else if (playlistChannels.isEmpty()) playlistError = failure.message ?: "Playlist could not be read"
                null
            }
            loaded?.let { (_, programmes) ->
                guideProgrammes = programmes
                playlistError = null
            }
            playlistLoading = false
            guideLoading = false
            kotlinx.coroutines.delay(6 * 60 * 60 * 1000L)
        }
    }

    val orderedApps = remember(apps, favoritePackages) { orderTvApps(apps, favoritePackages) }
    val favorites = remember(orderedApps, favoritePackages) {
        favoritePackages.mapNotNull { pkg -> orderedApps.firstOrNull { it.packageName == pkg } }
    }
    val filteredApps = remember(orderedApps, query) {
        if (query.isBlank()) orderedApps else orderedApps.filter { it.label.contains(query.trim(), true) }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }

    fun channelProgrammeLabel(channel: TvChannel): String? {
        val instant = java.time.Instant.now()
        val programme = currentAndNextProgrammes(channel, guideProgrammes, instant).let { (current, next) -> current ?: next }
            ?: return null
        return if (instant >= programme.start && instant < programme.end) "Now · ${programme.title}"
        else "Up next · ${programme.title}"
    }

    val lastWatchedChannel = playlistChannels.firstOrNull { tvChannelKey(it) == lastChannelKey }
    val nowPlayingChannel = lastWatchedChannel
        ?: playlistChannels.firstOrNull { channelProgrammeLabel(it)?.startsWith("Now ·") == true }
        ?: playlistChannels.firstOrNull()
    val featuredProgramme = nowPlayingChannel?.let { currentAndNextProgrammes(it, guideProgrammes, java.time.Instant.now()).first }
    val featuredProgrammeProgress = featuredProgramme?.let { programme ->
        ((java.time.Instant.now().epochSecond - programme.start.epochSecond).toFloat() /
            (programme.end.epochSecond - programme.start.epochSecond).coerceAtLeast(1)).coerceIn(0f, 1f)
    }
    val orderedChannels = remember(playlistChannels, favoriteChannelKeys) {
        playlistChannels.sortedByDescending { tvChannelKey(it) in favoriteChannelKeys }
    }
    val railChannels = orderedChannels.filterNot { it == nowPlayingChannel }
    val guideChannels = orderedChannels.filterNot { it == nowPlayingChannel }.take(4)
    val orderedHomeSections = remember(homeSections) {
        homeSections.filter { it in setOf("LIVE_TV", "QUICK_LAUNCH", "GUIDE") }.distinct()
            .ifEmpty { listOf("LIVE_TV", "QUICK_LAUNCH", "GUIDE") }
    }
    val showLiveSection = "LIVE_TV" in orderedHomeSections
    val showAppsSection = "QUICK_LAUNCH" in orderedHomeSections
    val showGuideSection = "GUIDE" in orderedHomeSections
    val reverseMainSections = orderedHomeSections.indexOf("QUICK_LAUNCH") in 0 until orderedHomeSections.indexOf("LIVE_TV")
    val guideBeforeLive = showGuideSection && orderedHomeSections.indexOf("GUIDE") < orderedHomeSections.indexOf("LIVE_TV")

    fun launchApp(app: TvLaunchableApp) {
        inAppTvWebDestination(app)?.let {
            embeddedWebDestination = it
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        runCatching {
            val options = ActivityOptions.makeBasic().apply {
                activity?.window?.decorView?.display?.displayId
                    ?.takeIf { it != android.view.Display.DEFAULT_DISPLAY }
                    ?.let { launchDisplayId = it }
            }.toGameBoxFullscreenLaunchBundle()
            activity?.startActivity(intent, options) ?: context.startActivity(intent)
        }.onSuccess { playbackMessage = null }
            .onFailure { playbackMessage = "${app.label} could not be opened" }
    }

    val tvBottomInset = bottomOverlayInset.takeIf { it > 0.dp } ?: 48.dp
    Box(Modifier.fillMaxSize()) {
        TvModeBackdrop()
        BoxWithConstraints(Modifier.fillMaxSize().externalDisplaySystemOverlayInsets(externalDisplay, tvBottomInset)) {
            val wide = maxWidth >= 800.dp
            Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 44.dp else 20.dp, vertical = if (wide) 10.dp else 16.dp)) {
                Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("GAMEBOX TV", color = Color(0xFFBFC0C3), fontSize = 11.sp, letterSpacing = 2.5.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(now.format(DateTimeFormatter.ofPattern("h:mm a")), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Light)
                            Text("  ·  ${now.format(dateFormatter)}", color = Color(0xFFB8B9BD), fontSize = 12.sp)
                        }
                    }
                    TvTopAction(Icons.Rounded.Gamepad, "Controllers") {
                        runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                    Spacer(Modifier.width(8.dp))
                    TvTopAction(Icons.Rounded.Wifi, "Wi-Fi") {
                        runCatching { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                    Spacer(Modifier.width(8.dp))
                    TvTopAction(Icons.Rounded.Settings, "Settings", onSettings)
                    Spacer(Modifier.width(8.dp))
                    TvTopAction(Icons.Rounded.PhoneAndroid, if (remotePaired) "Remote paired" else "Pair remote", onRemoteSetup)
                    Spacer(Modifier.width(8.dp))
                    TvTopAction(Icons.Rounded.Tune, "Home layout") { showHomeOptions = true }
                    Spacer(Modifier.width(8.dp))
                    TvTopAction(Icons.Rounded.Home, "Exit TV Mode", onReturnToGameBox)
                }

                Spacer(Modifier.height(if (wide) 14.dp else 10.dp))
                if (guideBeforeLive) {
                    TvNowNextGuide(
                        channels = orderedChannels.take(4), programmes = guideProgrammes,
                        now = java.time.Instant.now(),
                        onSelect = { channel -> onLastChannelChanged(tvChannelKey(channel)); activeChannel = channel },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (showLiveSection || showAppsSection) Row(Modifier.fillMaxWidth().height(if (wide) 238.dp else 205.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (showLiveSection && !reverseMainSections) TvNowPlayingHero(
                        channel = nowPlayingChannel,
                        programme = nowPlayingChannel?.let(::channelProgrammeLabel),
                        programmeProgress = featuredProgrammeProgress,
                        isLastWatched = lastWatchedChannel != null,
                        sourceLabel = activeTvPlaylistLabel(effectivePlaylistUri),
                        loading = playlistLoading && playlistChannels.isEmpty(),
                        error = playlistError,
                        wide = wide,
                        modifier = Modifier.weight(if (showAppsSection) 0.59f else 1f).fillMaxHeight(),
                        actionFocusRequester = homeFocusRequester,
                        actionFocusClaimed = homeFocusClaimed,
                        onWatch = {
                            nowPlayingChannel?.let { channel -> onLastChannelChanged(tvChannelKey(channel)); activeChannel = channel }
                                ?: run { showAllChannels = true }
                        },
                    )
                    if (showAppsSection) Column(Modifier.weight(if (showLiveSection) 0.41f else 1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                            TvSectionHeading("Quick launch", Icons.Rounded.Star, Modifier.weight(1f))
                            TvActionButton(if (favorites.isEmpty()) "Add apps" else "Edit apps") { appDrawerMode = false; managingApps = true }
                            TvActionButton("All apps", Icons.Rounded.Apps) { appDrawerMode = true; managingApps = true }
                        }
                        if (favorites.isEmpty()) {
                            TvQuickLaunchEmpty(
                                Modifier.fillMaxWidth().weight(1f),
                                focusRequester = if (!showLiveSection) homeFocusRequester else null,
                                focusClaimed = homeFocusClaimed,
                            ) { managingApps = true }
                        } else {
                            favorites.take(4).chunked(2).forEach { rowApps ->
                                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowApps.forEach { app ->
                                        TvQuickLaunchCard(
                                            app = app,
                                            onOpen = { launchApp(app) },
                                            onFocus = { selectedPackage = app.packageName },
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            focusRequester = if (!showLiveSection && rowApps.firstOrNull() == app) homeFocusRequester else null,
                                            focusClaimed = homeFocusClaimed,
                                        )
                                    }
                                    if (rowApps.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    if (showLiveSection && reverseMainSections) TvNowPlayingHero(
                        channel = nowPlayingChannel,
                        programme = nowPlayingChannel?.let(::channelProgrammeLabel),
                        programmeProgress = featuredProgrammeProgress,
                        isLastWatched = lastWatchedChannel != null,
                        sourceLabel = activeTvPlaylistLabel(effectivePlaylistUri),
                        loading = playlistLoading && playlistChannels.isEmpty(),
                        error = playlistError,
                        wide = wide,
                        modifier = Modifier.weight(if (showAppsSection) 0.59f else 1f).fillMaxHeight(),
                        actionFocusRequester = homeFocusRequester,
                        actionFocusClaimed = homeFocusClaimed,
                        onWatch = { nowPlayingChannel?.let { onLastChannelChanged(tvChannelKey(it)); activeChannel = it } ?: run { showAllChannels = true } },
                    )
                }

                if (showLiveSection) {
                Spacer(Modifier.height(if (wide) 20.dp else 14.dp))
                Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        TvSectionHeading("Live TV", Icons.Rounded.LiveTv)
                    }
                    Text(activeTvPlaylistLabel(effectivePlaylistUri), Modifier.padding(end = 10.dp), color = Color(0xFFB8B9BD), fontSize = 12.sp)
                    TvActionButton("All channels") { showAllChannels = true }
                    Spacer(Modifier.width(8.dp))
                    TvActionButton("Options", Icons.Rounded.Tune) { showPlaylistOptions = true }
                }
                if (guideLoading || guideError != null) {
                    TvGuideStatus(
                        loading = guideLoading,
                        error = guideError,
                        retryAvailable = guideRetryAvailable,
                        onRetry = { guideRetryCount++ },
                        onOptions = { showPlaylistOptions = true },
                    )
                }
                playlistReplacementError?.let {
                    Text(it, color = Color(0xFFFFC2C2), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(8.dp))
                when {
                    playlistLoading && playlistChannels.isEmpty() -> Text("Loading channels…", color = Color(0xFFCAD7E6))
                    playlistError != null -> Text("Playlist unavailable: $playlistError", color = Color(0xFFFFC2C2))
                    playlistChannels.isEmpty() -> Text("No channels in this playlist.", color = Color(0xFFCAD7E6))
                    railChannels.isEmpty() -> Text("Your featured channel is available above. Browse All channels for more.", color = Color(0xFFCAD7E6))
                    else -> LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(railChannels, key = TvChannel::streamUrl) { channel ->
                            TvChannelCard(channel, channelProgrammeLabel(channel), wide,
                                isFavorite = tvChannelKey(channel) in favoriteChannelKeys,
                                onToggleFavorite = {
                                    val key = tvChannelKey(channel)
                                    onFavoriteChannelsChanged(if (key in favoriteChannelKeys) favoriteChannelKeys - key else favoriteChannelKeys + key)
                                }) {
                                playbackMessage = null
                                onLastChannelChanged(tvChannelKey(channel))
                                activeChannel = channel
                            }
                        }
                    }
                }
                }
                if (showGuideSection && !guideBeforeLive) {
                    Spacer(Modifier.height(12.dp))
                    TvNowNextGuide(
                        channels = guideChannels, programmes = guideProgrammes,
                        now = java.time.Instant.now(), onSelect = { channel -> onLastChannelChanged(tvChannelKey(channel)); activeChannel = channel },
                        focusRequester = if (!showLiveSection && !showAppsSection) homeFocusRequester else null,
                        focusClaimed = homeFocusClaimed,
                    )
                }
                playbackMessage?.let { Text(it, color = Color(0xFFD5E9FF), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("A  Open   ·   B  Back   ·   D-pad  Navigate   ·   ${if (LocalRuntimeDeviceStatus.current.controllerNames.isEmpty()) "No controller" else "Controller connected"}   ·   Menu  Options",
                        color = Color(0xFFC5D4E5), fontSize = 12.sp)
                    Text("TV Mode", color = Color(0xFF8ED8FF), fontSize = 12.sp)
                }
            }
        }
        activeChannel?.let { channel ->
            BackHandler { activeChannel = null }
            TvChannelPlayer(
                channel = channel,
                modifier = Modifier.fillMaxSize().externalDisplaySystemOverlayInsets(externalDisplay, tvBottomInset),
                onClose = { activeChannel = null },
            )
        }
        embeddedWebDestination?.let { destination ->
            BackHandler { embeddedWebDestination = null }
            TvEmbeddedBrowser(
                title = destination.title,
                url = destination.url,
                modifier = Modifier.fillMaxSize().externalDisplaySystemOverlayInsets(externalDisplay, tvBottomInset),
                onClose = { embeddedWebDestination = null },
            )
        }
        if (managingApps) {
            BackHandler { managingApps = false }
            TvAppsManagerDialog(
                apps = filteredApps,
                launchMode = appDrawerMode,
                onLaunch = ::launchApp,
                pinnedPackages = favoritePackages,
                focusRequester = firstAppFocusRequester,
                onSearch = { showAppSearchKeyboard = true; onRemoteSearchTargetChanged(TvRemoteTextTarget.APP_SEARCH) },
                query = query,
                onPin = { app -> onFavoritesChanged(if (app.packageName in favoritePackages) favoritePackages - app.packageName else favoritePackages + app.packageName) },
                onMove = { app, delta -> onFavoritesChanged(com.gamebox.os.tvmode.moveTvFavorite(favoritePackages, app.packageName, delta)) },
                onFocus = { selectedPackage = it.packageName },
                onClose = { managingApps = false },
                error = loadError,
            )
        }
        if (showPlaylistOptions) {
            TvPlaylistOptionsDialog(
                countryCode = iptvCountryCode,
                queryError = playlistError,
                replacementError = playlistReplacementError,
                activePlaylist = activeTvPlaylistLabel(playlistUri),
                onCountryCodeChange = { iptvCountryCode = it.filter(Char::isLetter).take(2).uppercase() },
                onOpenKeyboard = { showCountryKeyboard = true; onRemoteSearchTargetChanged(TvRemoteTextTarget.COUNTRY_CODE) },
                onBrowse = {
                    runCatching { iptvOrgCountryPlaylistUrl(iptvCountryCode) }
                        .onSuccess { changePlaylist(it); playlistError = null; showPlaylistOptions = false }
                        .onFailure { playlistError = it.message ?: "Country code is invalid" }
                },
                onPickPlaylist = {
                    showPlaylistOptions = false
                    playlistPicker.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain", "*/*"), playlistPickerOptions)
                },
                onClose = { showPlaylistOptions = false },
            )
        }
        if (showAllChannels) {
            TvAllChannelsDialog(
                channels = playlistChannels,
                programmeLabel = ::channelProgrammeLabel,
                query = channelQuery,
                favoriteChannelKeys = favoriteChannelKeys,
                onToggleFavorite = { key -> onFavoriteChannelsChanged(if (key in favoriteChannelKeys) favoriteChannelKeys - key else favoriteChannelKeys + key) },
                onOpenKeyboard = { showChannelSearchKeyboard = true; onRemoteSearchTargetChanged(TvRemoteTextTarget.CHANNEL_SEARCH) },
                onSelect = { channel -> showAllChannels = false; onLastChannelChanged(tvChannelKey(channel)); activeChannel = channel },
                onClose = { showAllChannels = false },
            )
        }
        if (showHomeOptions) {
            TvHomeLayoutDialog(
                sections = orderedHomeSections,
                onChange = onHomeSectionsChanged,
                onClose = { showHomeOptions = false },
            )
        }
    }

    if (showAppSearchKeyboard) {
        ControllerSearchKeyboardDialog(
            query = query,
            onQueryChange = { query = it },
            onDismiss = { showAppSearchKeyboard = false; onRemoteSearchTargetChanged(null) },
            title = "Find an app",
        )
    }
    if (showCountryKeyboard) {
        ControllerSearchKeyboardDialog(
            query = iptvCountryCode,
            onQueryChange = { iptvCountryCode = it.filter(Char::isLetter).take(2).uppercase() },
            onDismiss = { showCountryKeyboard = false; onRemoteSearchTargetChanged(null) },
            title = "Country code",
        )
    }
    if (showChannelSearchKeyboard) {
        ControllerSearchKeyboardDialog(
            query = channelQuery,
            onQueryChange = { channelQuery = it },
            onDismiss = { showChannelSearchKeyboard = false; onRemoteSearchTargetChanged(null) },
            title = "Find a channel",
        )
    }
}

@Composable
private fun TvTopAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    TvActionButton(title, icon, onClick)
}

@Composable
private fun TvSectionHeading(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF252629),
            border = BorderStroke(1.dp, Color(0xFF3A3B3F)),
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFFC8DFFF), modifier = Modifier.size(18.dp))
            }
        }
        Text(title, Modifier.padding(start = 10.dp), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TvActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, if (reducedMotion) snap() else spring(), label = "tv-action-focus")
    Surface(
        modifier = Modifier.height(50.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = title; role = Role.Button }
            .blueprintClick(onClick),
        shape = RoundedCornerShape(50),
        color = if (focused) Color(0xFFA9CEFA) else Color(0xFF222326),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFC4DEFA) else Color(0xFF3B3C40)),
        shadowElevation = if (focused) 12.dp else 1.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (focused) Color(0xFF141517) else Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(title, color = if (focused) Color(0xFF141517) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TvNowPlayingHero(
    channel: TvChannel?, programme: String?, isLastWatched: Boolean, sourceLabel: String,
    programmeProgress: Float?, loading: Boolean, error: String?, wide: Boolean,
    modifier: Modifier = Modifier, actionFocusRequester: FocusRequester? = null,
    actionFocusClaimed: MutableState<Boolean>? = null, onWatch: () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "tv-now-playing-glow")
    val glowAlpha = if (reducedMotion) 0.72f else transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tv-featured-glow-alpha",
    ).value
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1D1E21),
        border = BorderStroke(1.dp, Color(0xFF34363A)),
        shadowElevation = 7.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF232427), Color(0xFF1C1D20), Color(0xFF202124)))))
            Box(
                Modifier.align(Alignment.CenterEnd).offset(x = 46.dp).size(if (wide) 230.dp else 180.dp)
                    .graphicsLayer { alpha = glowAlpha }
                    .background(
                        Brush.radialGradient(listOf(Color(0x182F3339), Color(0x0F292B2F), Color.Transparent)),
                        CircleShape,
                    ),
            )
            Box(Modifier.align(Alignment.CenterEnd).padding(end = if (wide) 204.dp else 150.dp)
                .width(2.dp).fillMaxHeight(0.56f).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x555C6067), Color.Transparent))))
            Row(
                Modifier.fillMaxSize().padding(horizontal = if (wide) 28.dp else 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF17181A),
                    border = BorderStroke(1.dp, Color(0xFF36383D)),
                    shadowElevation = 8.dp,
                ) {
                    Box(Modifier.padding(7.dp)) {
                        Icon(Icons.Rounded.LiveTv, null, tint = Color(0xFFD9DADD), modifier = Modifier.size(if (wide) 72.dp else 56.dp).padding(12.dp))
                    }
                }
                Column(Modifier.weight(1f).padding(start = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Color(0xFFB7D7FF), CircleShape))
                        Text(if (isLastWatched) "LAST WATCHED" else "FEATURED · $sourceLabel",
                            Modifier.padding(start = 8.dp), color = Color(0xFFCDD0D5), fontSize = 10.sp,
                            letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(channel?.let { displayTvChannelName(it.name) } ?: if (loading) "Loading channels…" else "Live TV",
                        color = Color.White,
                        fontSize = if (wide) 34.sp else 25.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        error ?: programme?.removePrefix("Now · ")?.removePrefix("Up next · ") ?: "Choose a channel to start watching.",
                        color = Color(0xFFC7C9CE), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (programmeProgress != null) {
                        LinearProgressIndicator(progress = { programmeProgress }, Modifier.fillMaxWidth(0.82f).padding(top = 5.dp).height(3.dp),
                            color = Color(0xFFA8CEFA), trackColor = Color(0xFF50535A))
                    }
                }
                Spacer(Modifier.width(16.dp))
                val activate = rememberUiClickAction(onWatch)
                Button(
                    onClick = activate,
                    modifier = Modifier.then(
                        if (actionFocusRequester != null && actionFocusClaimed != null) Modifier.claimInitialCatalogActionFocus(true, actionFocusRequester, actionFocusClaimed)
                        else Modifier,
                    ).blueprintButtonFocusRing(ButtonDefaults.shape).onBlueprintActivation(activate),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA9CEFA), contentColor = Color(0xFF111315)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Text(if (isLastWatched) "Watch again" else "Watch ${channel?.let { displayTvChannelName(it.name).take(18) }.orEmpty()}".trim(), Modifier.padding(start = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun TvNowNextGuide(
    channels: List<TvChannel>, programmes: List<TvProgramme>, now: java.time.Instant, onSelect: (TvChannel) -> Unit,
    focusRequester: FocusRequester? = null, focusClaimed: MutableState<Boolean>? = null,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(channels) { listState.scrollToItem(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvSectionHeading("Now and next", Icons.Rounded.LiveTv)
            Text("  ·  Programme guide", color = Color(0xFF9DB1C8), fontSize = 12.sp)
        }
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(channels, key = { _, channel -> "guide-${channel.streamUrl}" }) { index, channel ->
                val (current, next) = currentAndNextProgrammes(channel, programmes, now)
                Surface(
                    Modifier.width(350.dp).height(112.dp).then(
                        if (index == 0 && focusRequester != null && focusClaimed != null) Modifier.claimInitialCatalogActionFocus(true, focusRequester, focusClaimed)
                        else Modifier,
                    ).semantics {
                        contentDescription = buildString {
                            append(displayTvChannelName(channel.name))
                            current?.let { append(", now ${it.title}") }
                            next?.let { append(", next ${it.title}") }
                        }
                    }.blueprintClick { onSelect(channel) },
                    color = Color(0xFF1D1E21), shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF383A3E)),
                ) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(displayTvChannelName(channel.name), color = Color(0xFFC9DFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(current?.let { "${formatProgrammeTime(it.start)}  ${it.title}" } ?: "Programme info unavailable",
                            color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (current != null) {
                            val progress = ((now.epochSecond - current.start.epochSecond).toFloat() /
                                (current.end.epochSecond - current.start.epochSecond).coerceAtLeast(1)).coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(3.dp), color = Color(0xFFA8CEFA), trackColor = Color(0xFF50535A))
                        }
                        Text(next?.let { "Next · ${formatProgrammeTime(it.start)}  ${it.title}" } ?: "Next programme unavailable",
                            color = Color(0xFFBBD3E6), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (channels.isEmpty()) Text("No other channels are available in this playlist.", color = Color(0xFFB9BBC0), fontSize = 12.sp)
    }
}

private fun formatProgrammeTime(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault()).format(instant)

@Composable
private fun TvQuickLaunchEmpty(
    modifier: Modifier = Modifier, focusRequester: FocusRequester? = null,
    focusClaimed: MutableState<Boolean>? = null, onClick: () -> Unit,
) {
    Surface(modifier.fillMaxSize().then(
        if (focusRequester != null && focusClaimed != null) Modifier.claimInitialCatalogActionFocus(true, focusRequester, focusClaimed)
        else Modifier,
    ).blueprintClick(onClick), color = Color(0xFF202124),
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFF3B3C40))) {
        Box(contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, tint = Color(0xFFB9D7FC))
                Text(" Pin your favorite apps", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TvQuickLaunchCard(
    app: TvLaunchableApp, onOpen: () -> Unit, onFocus: () -> Unit,
    modifier: Modifier = Modifier, focusRequester: FocusRequester? = null,
    focusClaimed: MutableState<Boolean>? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, label = "quick-launch-focus")
    val opensInsideGameBox = inAppTvWebDestination(app) != null
    Surface(modifier.then(
        if (focusRequester != null && focusClaimed != null) Modifier.claimInitialCatalogActionFocus(true, focusRequester, focusClaimed)
        else Modifier,
    )
        .graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
        .semantics { contentDescription = "Open ${app.label}"; role = Role.Button }.blueprintClick(onOpen),
        color = if (focused) Color(0xFF30343A) else Color(0xFF1E1F22), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFA9CEFA) else Color(0xFF383A3E)),
        shadowElevation = if (focused) 14.dp else 2.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            TvAppIcon(app, Modifier.size(42.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(app.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (opensInsideGameBox) "Opens in GameBox" else "Opens installed app",
                    color = Color(0xFFB5B7BC), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!opensInsideGameBox) Icon(Icons.Rounded.OpenInNew, null, tint = Color(0xFFC4C6CB), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TvAppsManagerDialog(
    apps: List<TvLaunchableApp>, pinnedPackages: List<String>, focusRequester: FocusRequester,
    launchMode: Boolean, onLaunch: (TvLaunchableApp) -> Unit,
    onSearch: () -> Unit, query: String, onPin: (TvLaunchableApp) -> Unit,
    onMove: (TvLaunchableApp, Int) -> Unit, onFocus: (TvLaunchableApp) -> Unit,
    onClose: () -> Unit, error: String?,
) {
    TvModal(if (launchMode) "All apps" else "Edit quick launch", onClose) {
        ControllerSearchField(query, onSearch, Modifier.width(320.dp), label = "Find an app")
        error?.let { Text(it, color = Color(0xFFFFBABA)) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            items(apps, key = TvLaunchableApp::packageName) { app ->
                TvAppTile(app, app.packageName in pinnedPackages, { if (launchMode) onLaunch(app) else onPin(app) }, { onFocus(app) }, { onPin(app) }, true, true,
                    onMove = { onMove(app, it) }, focusRequester = if (app == apps.firstOrNull()) focusRequester else null)
            }
        }
        Text(if (launchMode) "Select an app to open it. Use the star to pin or unpin it on Home."
            else "Select an app to pin or unpin it. Use the arrows to reorder pinned apps.", color = Color(0xFFBBD3E6), fontSize = 13.sp)
    }
}

@Composable
private fun TvPlaylistOptionsDialog(
    countryCode: String, queryError: String?, replacementError: String?, activePlaylist: String,
    onCountryCodeChange: (String) -> Unit, onOpenKeyboard: () -> Unit, onBrowse: () -> Unit,
    onPickPlaylist: () -> Unit, onClose: () -> Unit,
) {
    TvModal("Live TV options", onClose) {
        Text("Playlist", color = Color(0xFFBBD3E6))
        Text(activePlaylist, color = Color.White, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ControllerSearchField(countryCode, onOpenKeyboard, Modifier.width(220.dp), label = "Country code")
            TvActionButton("Load country channels", Icons.Rounded.Public, onBrowse)
            TvActionButton("Choose playlist", Icons.Rounded.FolderOpen, onPickPlaylist)
        }
        (replacementError ?: queryError)?.let { Text(it, color = Color(0xFFFFBABA)) }
    }
}

@Composable
private fun TvAllChannelsDialog(
    channels: List<TvChannel>, programmeLabel: (TvChannel) -> String?, query: String,
    favoriteChannelKeys: List<String>, onToggleFavorite: (String) -> Unit,
    onOpenKeyboard: () -> Unit, onSelect: (TvChannel) -> Unit, onClose: () -> Unit,
) {
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = remember(channels) { channels.mapNotNull { it.group?.trim()?.takeIf(String::isNotEmpty) }.distinct().sortedBy(String::lowercase) }
    val filtered = remember(channels, query, favoritesOnly, favoriteChannelKeys, selectedCategory) {
        filterTvChannelCategory(channels, query, favoritesOnly, favoriteChannelKeys, selectedCategory)
    }
    TvModal("All channels · ${channels.size}", onClose) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ControllerSearchField(query, onOpenKeyboard, Modifier.width(360.dp), label = "Find a channel")
            FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly },
                label = { Text("Favorites") }, leadingIcon = { Icon(Icons.Rounded.Star, null) })
        }
        if (categories.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "all-categories") { FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("All categories") }) }
            items(categories, key = { it }) { category ->
                FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category, maxLines = 1) })
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 520.dp)) {
            items(filtered, key = TvChannel::streamUrl) { channel ->
                TvChannelCard(channel, programmeLabel(channel), true,
                    isFavorite = tvChannelKey(channel) in favoriteChannelKeys,
                    onToggleFavorite = { onToggleFavorite(tvChannelKey(channel)) }) { onSelect(channel) }
            }
        }
        if (filtered.isEmpty()) Text(if (favoritesOnly) "No favorite channels yet. Select a star to add one." else "No channels match this search.", color = Color(0xFFBBD3E6))
    }
}

internal fun filterTvChannelCategory(
    channels: List<TvChannel>, query: String, favoritesOnly: Boolean,
    favoriteChannelKeys: List<String>, selectedCategory: String?,
): List<TvChannel> = channels.filter {
    it.name.contains(query.trim(), true) &&
        (!favoritesOnly || tvChannelKey(it) in favoriteChannelKeys) &&
        (selectedCategory == null || it.group?.trim() == selectedCategory)
}

@Composable
internal fun TvGuideStatus(
    loading: Boolean,
    error: String?,
    retryAvailable: Boolean,
    onRetry: () -> Unit,
    onOptions: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().semantics { contentDescription = if (loading) "Loading programme guide" else "Programme guide unavailable" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            when {
                loading -> "Loading programme guide…"
                error != null -> "Programme guide unavailable: $error"
                else -> "Programme guide unavailable"
            },
            Modifier.weight(1f), color = Color(0xFFBBD3E6), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        if (!loading) TextButton(onClick = if (retryAvailable) onRetry else onOptions) {
            Text(if (retryAvailable) "Retry guide" else "Playlist options")
        }
    }
}

@Composable
private fun TvHomeLayoutDialog(sections: List<String>, onChange: (List<String>) -> Unit, onClose: () -> Unit) {
    val labels = mapOf("LIVE_TV" to "Live TV", "QUICK_LAUNCH" to "Quick launch", "GUIDE" to "Now and next guide")
    val visibleThenHidden = sections + listOf("LIVE_TV", "QUICK_LAUNCH", "GUIDE").filterNot { it in sections }
    TvModal("Customize Home", onClose) {
        Text("Choose which sections appear and their order. At least one section must stay visible.", color = Color(0xFFBBD3E6))
        visibleThenHidden.forEach { section ->
            val index = sections.indexOf(section)
            val visible = index >= 0
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = visible, enabled = visible || sections.size > 1, onCheckedChange = { checked ->
                    if (checked && !visible) onChange(sections + section)
                    else if (!checked && visible && sections.size > 1) onChange(sections - section)
                })
                Text(labels[section] ?: section, Modifier.weight(1f), color = if (visible) Color.White else Color(0xFF8193A8), fontSize = 16.sp)
                IconButton(enabled = visible && index > 0, onClick = {
                    onChange(sections.toMutableList().apply { add(index - 1, removeAt(index)) })
                }, modifier = Modifier.semantics { contentDescription = "Move ${labels[section]} up" }) {
                    Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Color.White)
                }
                IconButton(enabled = visible && index < sections.lastIndex, onClick = {
                    onChange(sections.toMutableList().apply { add(index + 1, removeAt(index)) })
                }, modifier = Modifier.semantics { contentDescription = "Move ${labels[section]} down" }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TvModal(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.86f).heightIn(max = 760.dp), shape = RoundedCornerShape(26.dp),
            color = Color(0xF41D1E21), border = BorderStroke(1.dp, Color(0xFF414247)), shadowElevation = 20.dp) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = "Close $title" }) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White)
                    }
                }
                content()
            }
        }
    }
}

@Composable
private fun TvEmbeddedBrowser(
    title: String,
    url: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    autoplayMedia: Boolean = false,
    subtitle: String? = null,
) {
    val webView = remember(url) { arrayOfNulls<WebView>(1) }
    var canGoBack by remember(url) { mutableStateOf(false) }
    var pageError by remember(url) { mutableStateOf(false) }
    var pageProgress by remember(url) { mutableStateOf(0) }
    BackHandler {
        if (webView[0]?.canGoBack() == true) webView[0]?.goBack() else onClose()
    }
    DisposableEffect(url) {
        onDispose {
            webView[0]?.apply {
                stopLoading()
                destroy()
            }
        }
    }
    Column(modifier.background(Color(0xFF080C14))) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xF5121C2B)).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (canGoBack) webView[0]?.goBack() else onClose() },
                modifier = Modifier.semantics { contentDescription = "Back in ${title}" }) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(if (pageError) "This service could not be loaded" else subtitle ?: Uri.parse(url).host.orEmpty(),
                    color = if (pageError) Color(0xFFFFBABA) else Color(0xFFB7CBDE), fontSize = 11.sp, maxLines = 1)
            }
            IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = "Close ${title}" }) {
                Icon(Icons.Rounded.Close, null, tint = Color.White)
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mediaPlaybackRequiresUserGesture = !autoplayMedia
                    settings.safeBrowsingEnabled = true
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            pageProgress = newProgress
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                            request.url.scheme != "https"

                        override fun onPageFinished(view: WebView, page: String) {
                            canGoBack = view.canGoBack()
                            pageError = false
                            view.requestFocus()
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                            if (request.isForMainFrame) pageError = true
                        }
                    }
                    webView[0] = this
                    loadUrl(url)
                }
            },
            update = { canGoBack = it.canGoBack() },
        )
        if (pageProgress < 100) {
            LinearProgressIndicator(progress = { pageProgress / 100f }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF6CCEFF))
        }
    }
}

@Composable
internal fun TvPlaybackRetryAction(
    error: String?,
    officialFallbackLabel: String? = null,
    onOfficialFallback: () -> Unit = {},
    onRetry: () -> Unit,
) {
    if (error != null) {
        TextButton(onClick = onRetry) { Text("Retry stream") }
        if (officialFallbackLabel != null) {
            TextButton(onClick = onOfficialFallback) { Text(officialFallbackLabel) }
        }
    }
}

@UnstableApi
@Composable
private fun TvChannelPlayer(channel: TvChannel, modifier: Modifier = Modifier, onClose: () -> Unit) {
    val officialRtsUrl = remember(channel) { officialRtsPlanetaLiveUrl(channel) }
    var useOfficialRts by remember(channel) { mutableStateOf(false) }
    if (useOfficialRts && officialRtsUrl != null) {
        TvEmbeddedBrowser(
            title = displayTvChannelName(channel.name) + " · RTS Planeta",
            url = officialRtsUrl,
            modifier = modifier,
            onClose = onClose,
            autoplayMedia = true,
            subtitle = "Direct stream unavailable · official RTS Planeta page",
        )
        return
    }
    val context = LocalContext.current
    var playing by remember(channel.streams) { mutableStateOf(false) }
    var loading by remember(channel.streams) { mutableStateOf(true) }
    var error by remember(channel.streams) { mutableStateOf<String?>(null) }
    var videoResolution by remember(channel.streams) { mutableStateOf<String?>(null) }
    var streamIndex by remember(channel.streams) { mutableIntStateOf(0) }
    val exoPlayer = remember(channel.streams) { ExoPlayer.Builder(context).build() }
    DisposableEffect(exoPlayer, channel.streams) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                loading = error == null &&
                    (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_BUFFERING)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoResolution = if (videoSize.width > 0 && videoSize.height > 0) {
                    "${videoSize.width}×${videoSize.height}"
                } else null
            }

            override fun onPlayerError(playerError: PlaybackException) {
                playing = false
                val nextIndex = nextTvStreamIndex(streamIndex, channel.streams.size)
                if (nextIndex != null) {
                    streamIndex = nextIndex
                    loading = true
                    error = null
                    exoPlayer.setTvStream(context, channel.streams[nextIndex])
                    exoPlayer.prepare()
                } else if (officialRtsUrl != null) {
                    loading = false
                    error = "Stream failed (${playerError.errorCodeName}). Retry or open RTS Planeta."
                } else {
                    loading = false
                    error = tvPlaybackErrorMessage(playerError.errorCodeName, playerError)
                }
            }
        }
        exoPlayer.addListener(listener)
        exoPlayer.setTvStream(context, channel.streams.first())
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    Box(modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { it.player = exoPlayer },
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(18.dp),
            color = Color(0xCC101827),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x665E8CAF)),
        ) {
            Row(Modifier.padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(displayTvChannelName(channel.name), color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(if (loading) "Connecting to stream…" else error ?: videoResolution?.let { "Live TV · $it" } ?: "Live TV",
                        color = if (error != null) Color(0xFFFFC2C2) else Color(0xFFC2D4E5), fontSize = 12.sp)
                }
                IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = "Back to Live TV" }) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
                }
            }
        }
        if (loading && error == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFF8FD8FF))
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            color = Color(0xCC101827),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x665E8CAF)),
        ) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TvPlaybackRetryAction(
                    error = error,
                    officialFallbackLabel = officialRtsUrl?.let { "Open RTS Planeta" },
                    onOfficialFallback = { useOfficialRts = true },
                ) {
                    val retryIndex = retryTvStreamIndex(channel.streams.size)
                        ?: return@TvPlaybackRetryAction
                    streamIndex = retryIndex
                    error = null
                    loading = true
                    playing = false
                    exoPlayer.setTvStream(context, channel.streams[retryIndex])
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                }
                IconButton(
                    enabled = !loading && error == null,
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier.semantics { contentDescription = if (playing) "Pause stream" else "Play stream" },
                ) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                }
                Text("B  Back to Live TV", Modifier.padding(horizontal = 8.dp), color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@UnstableApi
private fun ExoPlayer.setTvStream(context: android.content.Context, stream: TvStream) {
    val httpFactory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(stream.requestHeaders)
    val sourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(DefaultDataSource.Factory(context, httpFactory))
    setMediaSource(sourceFactory.createMediaSource(MediaItem.fromUri(stream.url)))
}

@Composable
private fun TvAppTile(
    app: TvLaunchableApp,
    pinned: Boolean,
    onOpen: () -> Unit,
    onFocus: () -> Unit,
    onTogglePin: () -> Unit,
    showPinControls: Boolean,
    wide: Boolean,
    onMove: ((Int) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(if (focused) 1.045f else 1f, if (reducedMotion) snap() else spring(), label = "tv-app-focus")
    Column(Modifier.width(if (wide) 180.dp else 142.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(if (wide) 104.dp else 94.dp)
                .then(focusRequester?.let(Modifier::focusRequester) ?: Modifier)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
                .semantics { contentDescription = "${app.label}, ${if (pinned) "pinned" else "not pinned"}"; role = Role.Button }
                .blueprintClick(onOpen),
            color = if (focused) Color(0xFF30343A) else Color(0xFF1D1E21),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFA9CEFA) else Color(0xFF3A3B3F)),
            shadowElevation = if (focused) 22.dp else 5.dp,
        ) {
            Box(Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0x183D4249), Color.Transparent, Color(0x102A2C31))),
            ).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Box(Modifier.align(Alignment.TopEnd).size(if (wide) 90.dp else 70.dp)
                    .background(Brush.radialGradient(listOf(Color(0x223D4249), Color.Transparent)), CircleShape))
                Surface(
                    modifier = Modifier.align(Alignment.TopStart),
                    color = Color(0xFF303236),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFF4A4C51)),
                ) {
                    Text(if (inAppTvWebDestination(app) == null) "APP" else "WEB", Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = Color(0xFFE0E1E4), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                TvAppIcon(app, Modifier.align(Alignment.Center).offset(y = (-8).dp).size(if (wide) 58.dp else 46.dp))
                Text(app.label, Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = Color.White, fontSize = if (wide) 15.sp else 12.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        if (showPinControls) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePin, modifier = Modifier.size(34.dp).semantics { contentDescription = if (pinned) "Unpin ${app.label}" else "Pin ${app.label}" }) {
                    Icon(if (pinned) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = null,
                        tint = if (pinned) Color(0xFFFFD572) else Color.White)
                }
                if (onMove != null && pinned) {
                    IconButton(onClick = { onMove(-1) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Rounded.ChevronLeft, "Move ${app.label} earlier", tint = Color.White) }
                    IconButton(onClick = { onMove(1) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Rounded.ChevronRight, "Move ${app.label} later", tint = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun TvAppIcon(app: TvLaunchableApp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(app.packageName)
                val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, 96, 96)
                drawable.draw(Canvas(bitmap))
                bitmap.asImageBitmap()
            }.getOrNull()
        }
    }
    Surface(modifier, shape = RoundedCornerShape(10.dp), color = Color(0xFFEEF4FA)) {
        if (bitmap != null) androidx.compose.foundation.Image(bitmap!!, app.label, Modifier.fillMaxSize().padding(5.dp))
        else Box(contentAlignment = Alignment.Center) { Text(app.label.take(1).uppercase(), color = Color(0xFF19334B), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun TvChannelCard(
    channel: TvChannel, programmeLabel: String?, wide: Boolean, isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null, onClick: () -> Unit,
) {
    val displayName = displayTvChannelName(channel.name)
    var focused by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(if (focused) 1.045f else 1f, if (reducedMotion) snap() else spring(), label = "tv-channel-focus")
    Surface(
        modifier = Modifier.width(if (wide) 290.dp else 220.dp).height(if (wide) 80.dp else 78.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = "Watch $displayName${programmeLabel?.let { ", $it" } ?: ""}"; role = Role.Button }
            .blueprintClick(onClick),
        color = if (focused) Color(0xFF282B30) else Color(0xFF1D1E21), shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFA9CEFA) else Color(0xFF3A3B3F)),
        shadowElevation = if (focused) 14.dp else 2.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF303236)),
                contentAlignment = Alignment.Center,
            ) {
                if (channel.logoUrl != null) {
                    TvChannelLogo(channel.logoUrl, displayName)
                } else Text(displayName.filter(Char::isLetterOrDigit).take(3).uppercase().ifBlank { "TV" },
                    color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(displayName, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                    if (hasCurrentTvProgramme(programmeLabel)) {
                        Spacer(Modifier.width(5.dp))
                        Surface(color = Color(0x553C1422), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Color(0x887E3546))) {
                            Text("● LIVE", Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = Color(0xFFFFA4A4),
                                fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(programmeLabel?.removePrefix("Now · ")?.removePrefix("Up next · ") ?: "Programme guide unavailable",
                    color = Color(0xFFBBD3E6), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
            }
            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(38.dp).semantics {
                    contentDescription = if (isFavorite) "Remove $displayName from favorites" else "Add $displayName to favorites"
                }) {
                    Icon(if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, null,
                        tint = if (isFavorite) Color(0xFFFFD572) else Color(0xFFC4C6CB), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun TvChannelLogo(url: String, label: String) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, url) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.instanceFollowRedirects = false
                try {
                    require(connection.url.protocol == "https" && connection.responseCode == 200)
                    require(connection.contentLengthLong in -1L..512_000L)
                    val bytes = connection.inputStream.use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 512_000) { "Channel logo is too large" }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048) { "Invalid channel logo dimensions" }
                    val sample = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 256)
                    }
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sample)?.asImageBitmap()
                } finally { connection.disconnect() }
            }.getOrNull()
        }
    }
    if (bitmap != null) androidx.compose.foundation.Image(bitmap!!, label, Modifier.fillMaxSize().padding(4.dp))
    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(label.filter(Char::isLetterOrDigit).take(3).uppercase().ifBlank { "TV" }, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
