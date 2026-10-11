package com.gamebox.os

import android.app.DownloadManager
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.FrameLayout
import com.gamebox.os.display.applyBorderlessFullscreenWindow
import com.gamebox.os.display.hideSystemBars
import com.gamebox.os.display.requestExternalFullscreenAfterFocus
import com.gamebox.os.importer.RomImportPolicy
import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.input.gamepadBrowserNavigationKeyCode
import com.gamebox.os.input.gamepadBrowserNavigationMetaState
import com.gamebox.os.input.gamepadBrowserActivationKeyCode
import com.gamebox.os.input.gamepadBrowserKeyEvent
import com.gamebox.os.ui.TrackedSourceBrowserToolbar
import com.gamebox.os.ui.TrackedSourcePageProgress
import com.gamebox.os.ui.theme.GameBoxTheme
import java.util.UUID

internal const val VIMM_DOWNLOAD_FOCUS_SCRIPT = """
    (() => {
      const label = /^download\b/i;
      const specificLabel = /^download\s+(?:this\s+)?(?:game|rom|file)\b/i;
      const visible = e => {
        for (let n = e; n && n !== document.body; n = n.parentElement) {
          const s = getComputedStyle(n);
          if (s.display === 'none' || s.visibility === 'hidden' || s.visibility === 'collapse') return false;
        }
        return e.getClientRects().length > 0;
      };
      const labels = e => [e.value, e.innerText, e.textContent, e.getAttribute('aria-label'), e.title].filter(Boolean);
      const isSpecific = e => labels(e).some(t => specificLabel.test(t.trim()));
      const find = () => {
        const candidates = [...document.querySelectorAll('button,input[type=button],input[type=submit],a,[role=button],[onclick],[tabindex]')]
          .filter(x => labels(x).some(t => label.test(t.trim())) && visible(x) && !x.disabled && x.getAttribute('aria-disabled') !== 'true');
        return candidates.find(isSpecific) || candidates[0] ||
          [...document.querySelectorAll('a[href^="/vault/"]')].find(x => /^\/vault\/\d+(?:[/?#]|$)/.test(x.getAttribute('href')) && x.textContent.trim() && visible(x));
      };
      const addFocusCue = () => {
        if (document.getElementById('gamebox-controller-focus')) return;
        const s = document.createElement('style');
        s.id = 'gamebox-controller-focus';
        s.textContent = '*:focus{outline:3px solid #ffd400!important;outline-offset:3px}';
        (document.head || document.documentElement).appendChild(s);
      };
      let focused = null;
      const focus = () => {
        const e = find();
        if (!e) return false;
        const preferred = isSpecific(e) || /^\/vault\/\d+(?:[/?#]|$)/.test(e.getAttribute('href') || '');
        if (e !== focused) {
          addFocusCue();
          if (e.tabIndex < 0) e.tabIndex = 0;
          if (!/^(BUTTON|INPUT|A)$/.test(e.tagName) && !e.dataset.gameboxKeyActivation) {
            e.dataset.gameboxKeyActivation = 'true';
            e.addEventListener('keydown', k => {
              if (k.key === 'Enter' || k.key === ' ') { k.preventDefault(); e.click(); }
            });
          }
          e.focus();
          e.scrollIntoView({block: 'center'});
          focused = e;
        }
        return preferred;
      };
      if (focus()) return;
      const observer = new MutationObserver(() => { if (focus()) observer.disconnect(); });
      observer.observe(document.documentElement, {childList: true, subtree: true, attributes: true});
      setTimeout(() => observer.disconnect(), 5000);
    })()
"""
internal fun focusBrowserView(view: View) {
    view.isFocusable = true
    view.isFocusableInTouchMode = true
    if (view.isAttachedToWindow) {
        view.requestFocus(View.FOCUS_DOWN)
    } else {
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.removeOnAttachStateChangeListener(this)
                view.requestFocus(View.FOCUS_DOWN)
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
}

/** Keeps the source page and its user-initiated download on GameBox's TV display. */
class TrackedSourceBrowserActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var browserContainer: FrameLayout
    private var popupWebView: WebView? = null
    private var allowControllerPopup = false
    private val pageLoadProgress = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        applyBorderlessFullscreenWindow(window)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars(window)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnToPreviousPage()
        })
        val source = intent.getStringExtra(EXTRA_SOURCE_URL)?.let(Uri::parse)
        if (source?.scheme != "https" || source.host.isNullOrBlank()) {
            finish()
            return
        }
        val platform = intent.getStringExtra(EXTRA_PLATFORM)
        val label = intent.getStringExtra(EXTRA_SOURCE_LABEL).orEmpty()
        browserContainer = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        lateinit var browserChromeClient: WebChromeClient
        browserChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (view === webView || view === popupWebView) {
                    pageLoadProgress.intValue = newProgress.coerceIn(0, 100)
                }
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message,
            ): Boolean {
                // Source sites sometimes put the download action in target=_blank.
                // Keep that navigation in GameBox so its DownloadListener can track it.
                if (!isUserGesture && !allowControllerPopup) return false
                allowControllerPopup = false
                // A previous page/ad popup must not block the user's actual Download link.
                popupWebView?.let(::closePopup)
                val popup = createSourceWebView(platform, browserChromeClient)
                popupWebView = popup
                browserContainer.addView(
                    popup,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                focusBrowserView(popup)
                (resultMsg.obj as? WebView.WebViewTransport)?.webView = popup
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView) {
                closePopup(window)
            }
        }
        webView = createSourceWebView(platform, browserChromeClient)
        browserContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        focusBrowserView(webView)
        webView.loadUrl(source.toString())
        setContent {
            GameBoxTheme {
                Column(Modifier.fillMaxSize()) {
                    TrackedSourceBrowserToolbar(
                        sourceLabel = label.ifBlank { getString(R.string.tracked_source_web_page) },
                        backLabel = getString(R.string.tracked_source_back_to_gamebox),
                        onBack = { finish() },
                    )
                    TrackedSourcePageProgress(
                        progress = pageLoadProgress.intValue,
                        loadingDescription = getString(R.string.tracked_source_loading_page),
                    )
                    AndroidView(
                        factory = { browserContainer },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            returnToPreviousPage()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    @SuppressLint("RestrictedApi") // Match the shared host's controller-confirm translation.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val activationKeyCode = gamepadBrowserActivationKeyCode(event.keyCode)
        if (activationKeyCode != null) {
            val activeBrowser = popupWebView ?: if (::webView.isInitialized) webView else null
            val host = activeBrowser?.url?.let(Uri::parse)?.host.orEmpty()
            if (activeBrowser != null && (host == "vimm.net" || host.endsWith(".vimm.net"))) {
                if (!activeBrowser.hasFocus()) focusBrowserView(activeBrowser)
                allowControllerPopup = true
                val translated = gamepadBrowserKeyEvent(
                    event,
                    activationKeyCode,
                    event.metaState,
                )
                val handled = activeBrowser.dispatchKeyEvent(translated)
                if (event.action == KeyEvent.ACTION_UP) {
                    activeBrowser.postDelayed({ allowControllerPopup = false }, 1_000)
                }
                return handled
            }
            val translated = gamepadBrowserKeyEvent(
                event,
                activationKeyCode,
                event.metaState,
            )
            return super.dispatchKeyEvent(translated)
        }
        val browserNavigationKeyCode = gamepadBrowserNavigationKeyCode(event.keyCode)
        if (browserNavigationKeyCode != null) {
            val translated = gamepadBrowserKeyEvent(
                event,
                browserNavigationKeyCode,
                gamepadBrowserNavigationMetaState(event.keyCode, event.metaState),
            )
            return super.dispatchKeyEvent(translated)
        }
        return super.dispatchKeyEvent(event)
    }

    private fun returnToPreviousPage() {
        val active = popupWebView ?: if (::webView.isInitialized) webView else null
        when {
            active == null -> finish()
            active.canGoBack() -> active.goBack()
            active !== webView -> closePopup(active)
            else -> finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createSourceWebView(platform: String?, chromeClient: WebChromeClient): WebView = WebView(this).apply {
        setBackgroundColor(android.graphics.Color.BLACK)
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        webChromeClient = chromeClient
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean =
                request.url.scheme != "https"

            override fun onPageFinished(view: WebView, url: String) {
                focusBrowserView(view)
                val host = Uri.parse(url).host.orEmpty()
                if (host == "vimm.net" || host.endsWith(".vimm.net")) {
                    view.evaluateJavascript(
                        VIMM_DOWNLOAD_FOCUS_SCRIPT,
                        null,
                    )
                }
            }
        }
        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            startTrackedDownload(url, userAgent, contentDisposition, mimeType, platform)
        }
    }

    private fun startTrackedDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        platform: String?,
    ) {
        val downloadUrl = Uri.parse(url)
        if (downloadUrl.scheme != "https" || downloadUrl.host.isNullOrBlank()) {
            reportDownloadStartFailure(
                url = url,
                platform = platform,
                displayName = null,
                reason = getString(R.string.tracked_source_https_downloads_only),
            )
            return
        }
        val guessedName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val displayName = runCatching { RomImportPolicy.safeFileName(guessedName, platform) }
            .getOrElse { error ->
                reportDownloadStartFailure(
                    url = url,
                    platform = platform,
                    displayName = guessedName,
                    reason = error.message ?: getString(R.string.tracked_source_unsupported_filename),
                )
                return
            }
        val gameId = intent.getStringExtra(EXTRA_GAME_ID)?.takeIf(String::isNotBlank)
        val gameTitle = intent.getStringExtra(EXTRA_GAME_TITLE)?.takeIf(String::isNotBlank)
        val platformId = intent.getStringExtra(EXTRA_PLATFORM_ID)?.takeIf(String::isNotBlank)
        if (gameId == null || gameTitle == null || platformId == null || platform.isNullOrBlank()) {
            Toast.makeText(this, "Game details are missing; return to GameBox and try again", Toast.LENGTH_LONG).show()
            return
        }
        runCatching {
            val stagedName = "${UUID.randomUUID()}.${displayName.substringAfterLast('.').lowercase()}"
            val request = DownloadManager.Request(downloadUrl)
                .setTitle(displayName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, stagedName)
            if (!mimeType.isNullOrBlank()) request.setMimeType(mimeType)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            (popupWebView?.url ?: webView.url)?.takeIf { it.startsWith("https://") }?.let {
                request.addRequestHeader("Referer", it)
            }
            CookieManager.getInstance().getCookie(url)?.takeIf(String::isNotBlank)?.let {
                request.addRequestHeader("Cookie", it)
            }
            val downloadManager = getSystemService(DownloadManager::class.java)
            val id = downloadManager.enqueue(request)
            val app = application as GameBoxApplication
            try {
                app.browserDownloadStore.add(
                    BrowserDownloadRecord(
                        downloadId = id,
                        stagedName = stagedName,
                        displayName = displayName,
                        gameId = gameId,
                        title = gameTitle,
                        platformName = platform,
                        platformId = platformId,
                        region = intent.getStringExtra(EXTRA_GAME_REGION),
                        releaseDate = intent.getStringExtra(EXTRA_GAME_RELEASE_DATE),
                        description = intent.getStringExtra(EXTRA_GAME_DESCRIPTION),
                        players = intent.getStringExtra(EXTRA_GAME_PLAYERS),
                        rating = intent.getDoubleExtra(EXTRA_GAME_RATING, Double.NaN)
                            .takeUnless(Double::isNaN),
                        coverUrl = intent.getStringExtra(EXTRA_GAME_COVER),
                        backgroundUrl = intent.getStringExtra(EXTRA_GAME_BACKGROUND),
                        logoUrl = intent.getStringExtra(EXTRA_GAME_LOGO),
                        downloadUrl = url,
                        userAgent = userAgent,
                        mimeType = mimeType,
                        referer = (popupWebView?.url ?: webView.url)?.takeIf { it.startsWith("https://") },
                    ),
                )
            } catch (error: Exception) {
                // The accepted request cannot be safely tracked without its durable
                // game metadata, so remove it only when that metadata could not be saved.
                downloadManager.remove(id)
                throw error
            }
            // Android owns the transfer now. Retry credentials and GameBox's
            // background monitor are optional follow-up work; failures here must
            // never cancel a download that the user explicitly started.
            runCatching {
                app.browserDownloadStore.setRetryCookie(id, CookieManager.getInstance().getCookie(url))
            }
            runCatching { app.browserDownloadCoordinator.trackImmediately(id) }
            setResult(RESULT_OK, Intent().apply {
                putExtra(EXTRA_DOWNLOAD_ID, id)
                putExtra(EXTRA_STAGED_NAME, stagedName)
                putExtra(EXTRA_DISPLAY_NAME, displayName)
            })
            finish()
        }.onFailure {
            reportDownloadStartFailure(
                url = url,
                platform = platform,
                displayName = displayName,
                reason = getString(
                    R.string.tracked_source_download_start_failed,
                    it.message.orEmpty().take(100),
                ),
                retryable = true,
                userAgent = userAgent,
                mimeType = mimeType,
            )
        }
    }

    /** Keep a rejected/failed Vimm attempt visible instead of making it look like nothing happened. */
    private fun reportDownloadStartFailure(
        url: String,
        platform: String?,
        displayName: String?,
        reason: String,
        retryable: Boolean = false,
        userAgent: String? = null,
        mimeType: String? = null,
    ) {
        val gameId = intent.getStringExtra(EXTRA_GAME_ID)?.takeIf(String::isNotBlank)
        val gameTitle = intent.getStringExtra(EXTRA_GAME_TITLE)?.takeIf(String::isNotBlank)
        val platformId = intent.getStringExtra(EXTRA_PLATFORM_ID)?.takeIf(String::isNotBlank)
        val safePlatform = platform?.takeIf(String::isNotBlank)
        if (gameId == null || gameTitle == null || platformId == null || safePlatform == null) {
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
            return
        }
        val app = application as GameBoxApplication
        val previousNegativeId = app.browserDownloadStore.records.value
            .minOfOrNull { it.downloadId }?.takeIf { it < 0L } ?: 0L
        val attemptId = minOf(-System.currentTimeMillis(), previousNegativeId - 1L)
        val safeDisplayName = displayName?.takeIf(String::isNotBlank)
            ?: (gameTitle.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(100).ifBlank { "download" } + ".download")
        val stagedExtension = safeDisplayName.substringAfterLast('.', "download")
            .lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,10}")) } ?: "download"
        val record = BrowserDownloadRecord(
            downloadId = attemptId,
            stagedName = "${UUID.randomUUID()}.$stagedExtension",
            displayName = safeDisplayName,
            gameId = gameId,
            title = gameTitle,
            platformName = safePlatform,
            platformId = platformId,
            region = intent.getStringExtra(EXTRA_GAME_REGION),
            releaseDate = intent.getStringExtra(EXTRA_GAME_RELEASE_DATE),
            description = intent.getStringExtra(EXTRA_GAME_DESCRIPTION),
            players = intent.getStringExtra(EXTRA_GAME_PLAYERS),
            rating = intent.getDoubleExtra(EXTRA_GAME_RATING, Double.NaN).takeUnless(Double::isNaN),
            coverUrl = intent.getStringExtra(EXTRA_GAME_COVER),
            backgroundUrl = intent.getStringExtra(EXTRA_GAME_BACKGROUND),
            logoUrl = intent.getStringExtra(EXTRA_GAME_LOGO),
            downloadUrl = url.takeIf { retryable },
            userAgent = userAgent,
            mimeType = mimeType,
            referer = (popupWebView?.url ?: webView.url)?.takeIf { it.startsWith("https://") },
            status = com.gamebox.os.importer.BrowserDownloadStatus.FAILED,
            error = reason.take(180),
        )
        runCatching { app.browserDownloadStore.add(record) }
        setResult(RESULT_OK, Intent().putExtra(EXTRA_DOWNLOAD_ERROR, reason.take(180)))
        finish()
    }

    private fun closePopup(window: WebView) {
        if (window !== popupWebView) return
        popupWebView = null
        if (::browserContainer.isInitialized) browserContainer.removeView(window)
        window.stopLoading()
        window.destroy()
        pageLoadProgress.intValue = 100
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars(window)
            requestExternalFullscreenAfterFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars(window)
        requestExternalFullscreenAfterFocus()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        hideSystemBars(window)
        if (isInMultiWindowMode) requestExternalFullscreenAfterFocus()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideSystemBars(window)
    }

    override fun onDestroy() {
        popupWebView?.let(::closePopup)
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SOURCE_URL = "source_url"
        const val EXTRA_SOURCE_LABEL = "source_label"
        const val EXTRA_PLATFORM = "platform"
        const val EXTRA_DOWNLOAD_ID = "download_id"
        const val EXTRA_STAGED_NAME = "staged_name"
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_DOWNLOAD_ERROR = "download_error"
        const val EXTRA_GAME_ID = "game_id"
        const val EXTRA_GAME_TITLE = "game_title"
        const val EXTRA_PLATFORM_ID = "platform_id"
        const val EXTRA_GAME_REGION = "game_region"
        const val EXTRA_GAME_RELEASE_DATE = "game_release_date"
        const val EXTRA_GAME_DESCRIPTION = "game_description"
        const val EXTRA_GAME_PLAYERS = "game_players"
        const val EXTRA_GAME_RATING = "game_rating"
        const val EXTRA_GAME_COVER = "game_cover"
        const val EXTRA_GAME_BACKGROUND = "game_background"
        const val EXTRA_GAME_LOGO = "game_logo"
    }
}
