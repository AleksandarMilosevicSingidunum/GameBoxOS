package com.gamebox.os

import android.os.Bundle
import android.content.Intent
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import com.gamebox.os.input.AnalogNavigationDebouncer
import com.gamebox.os.navigation.GameBoxDeepLink
import com.gamebox.os.navigation.GameBoxNavigationRequest
import com.gamebox.os.ui.GameBoxApp
import com.gamebox.os.ui.OfflineStatusBanner
import com.gamebox.os.ui.theme.GameBoxTheme

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as GameBoxApplication).container }
    private val analogNavigation = AnalogNavigationDebouncer()
    private val navigationRequest = mutableStateOf<GameBoxNavigationRequest?>(null)
    private var navigationSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptNavigation(intent)
        setContent {
            val saveFactory = androidx.compose.runtime.remember(container) { container::createSaveSafetyController }
            GameBoxTheme {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
                    OfflineStatusBanner(this@MainActivity)
                    GameBoxApp(
                        container.gameRepository,
                        container.downloadRepository,
                        container.authorizedDownloadController,
                        container.remoteDownloadController,
                        container.gameLaunchController,
                        container.saveSafetyController,
                        container.settingsRepository,
                        container.catalogDiscoveryRepository,
                        container.authorizedRomImporter,
                        container.managedSaveDiscovery,
                        saveControllerFactory = saveFactory,
                        navigationRequest = navigationRequest.value,
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
