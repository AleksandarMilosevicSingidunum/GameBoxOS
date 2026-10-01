package com.gamebox.os

import android.content.Intent
import android.os.Bundle
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.gamebox.os.input.AnalogNavigationDebouncer
import com.gamebox.os.navigation.GameBoxDeepLink
import com.gamebox.os.navigation.GameBoxNavigationRequest
import com.gamebox.os.ui.GameBoxApp
import com.gamebox.os.ui.OfflineStatusBanner
import com.gamebox.os.ui.theme.GameBoxTheme

/** Shared host for the normal Home task and the separate external-display task. */
abstract class GameBoxHostActivity : ComponentActivity() {
    private val container by lazy { (application as GameBoxApplication).container }
    private val analogNavigation = AnalogNavigationDebouncer()
    private val navigationRequest = mutableStateOf<GameBoxNavigationRequest?>(null)
    private var navigationSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptNavigation(intent)
        setContent {
            val saveFactory = androidx.compose.runtime.remember(container) {
                container::createSaveSafetyController
            }
            GameBoxTheme {
                Column(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .safeDrawingPadding(),
                ) {
                    OfflineStatusBanner(this@GameBoxHostActivity)
                    GameBoxApp(
                        container.gameRepository,
                        container.downloadRepository,
                        container.authorizedDownloadController,
                        container.remoteDownloadController,
                        container.gameLaunchController,
                        container.saveSafetyController,
                        container.settingsRepository,
                        container.catalogProvider,
                        container.catalogDiscoveryRepository,
                        container.authorizedRomImporter,
                        container.managedSaveDiscovery,
                        saveControllerFactory = saveFactory,
                        navigationRequest = navigationRequest.value,
                        externalDisplayOverride = display?.displayId != Display.DEFAULT_DISPLAY,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptNavigation(intent)
    }

    private fun acceptNavigation(intent: Intent?) {
        GameBoxDeepLink.parse(intent)?.let { parsed ->
            navigationRequest.value = parsed.copy(requestId = ++navigationSequence)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Compose's clickable/focusable controls understand D-pad center,
        // while Android gamepads report their confirm button as BUTTON_A. Keep
        // controller confirmation consistent across the normal and TV hosts by
        // translating only that confirm key before it reaches the view tree.
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_A) {
            val translated = KeyEvent(
                event.downTime,
                event.eventTime,
                event.action,
                KeyEvent.KEYCODE_DPAD_CENTER,
                event.repeatCount,
                event.metaState,
                event.deviceId,
                event.scanCode,
                event.flags,
                event.source,
            )
            return super.dispatchKeyEvent(translated)
        }
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                onBackPressedDispatcher.onBackPressed()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val joystick = event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (joystick && event.action == MotionEvent.ACTION_MOVE) {
            val keyCode = analogNavigation.consume(
                stickX = event.getAxisValue(MotionEvent.AXIS_X),
                stickY = event.getAxisValue(MotionEvent.AXIS_Y),
                hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
                hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                eventTimeMs = event.eventTime,
            )
            if (keyCode != null) {
                val down = KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
                val up = KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_UP, keyCode, 0)
                super.dispatchKeyEvent(down)
                super.dispatchKeyEvent(up)
                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onPause() {
        analogNavigation.reset()
        container.gameLaunchController.onHostPaused()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        container.gameLaunchController.onHostResumed()
        container.managedSaveDiscovery.refresh()
    }
}
