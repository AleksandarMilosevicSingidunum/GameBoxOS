package com.gamebox.os

import android.content.Intent
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import com.gamebox.os.input.AnalogNavigationDebouncer
import com.gamebox.os.input.gamepadConfirmKeyCode
import com.gamebox.os.display.currentDisplayId
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
    private val hostWindowHasFocus = mutableStateOf(false)
    private val textInputExitSequence = mutableIntStateOf(0)
    private val externalDisplayBottomInsetPx = mutableIntStateOf(0)
    private var navigationSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // GameBox is a living-room launcher: keep its visible host awake without
        // changing the user's system timeout or preventing a manual power-button lock.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        acceptNavigation(intent)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val bottomInset = if (currentDisplayId() != Display.DEFAULT_DISPLAY) {
                insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom
            } else {
                0
            }
            if (externalDisplayBottomInsetPx.intValue != bottomInset) {
                externalDisplayBottomInsetPx.intValue = bottomInset
            }
            insets
        }
        val hostDisplayId = currentDisplayId()
        setContent {
            GameBoxTheme {
                Column(
                Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // The TV Home already carries connection state in its
                    // status rail. Keep the phone-only alert, but don't let
                    // a duplicate connectivity strip consume living-room
                    // viewport height above the Blueprint navigation.
                    if (hostDisplayId == Display.DEFAULT_DISPLAY) {
                        OfflineStatusBanner(this@GameBoxHostActivity)
                    }
                    GameBoxApp(
                        container = container,
                        navigationRequest = navigationRequest.value,
                        externalDisplayOverride = hostDisplayId != Display.DEFAULT_DISPLAY,
                        externalDisplayBottomInsetPx = externalDisplayBottomInsetPx.intValue,
                        hostWindowHasFocus = hostWindowHasFocus.value,
                        textInputExitRequest = textInputExitSequence.intValue,
                    )
                }
            }
        }
        ViewCompat.requestApplyInsets(window.decorView)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptNavigation(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hostWindowHasFocus.value = hasFocus
        container.gameLaunchController.onHostWindowFocusChanged(hasFocus)
    }

    private fun acceptNavigation(intent: Intent?) {
        GameBoxDeepLink.parse(intent)?.let { parsed ->
            navigationRequest.value = parsed.copy(requestId = ++navigationSequence)
        }
    }

    @SuppressLint("RestrictedApi") // Preserve the Activity's normal view dispatch after key translation.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Compose's clickable/focusable controls understand D-pad center,
        // while Android gamepads report their confirm button as BUTTON_A. Keep
        // controller confirmation consistent across the phone and external-display hosts by
        // translating only that confirm key before it reaches the view tree.
        val confirmKeyCode = gamepadConfirmKeyCode(event.keyCode)
        if (confirmKeyCode != null) {
            val translated = KeyEvent(
                event.downTime,
                event.eventTime,
                event.action,
                confirmKeyCode,
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
                if (isTextInputActive()) textInputExitSequence.intValue++
                else onBackPressedDispatcher.onBackPressed()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isTextInputActive(): Boolean {
        val inputMethod = getSystemService(InputMethodManager::class.java)
        return inputMethod?.isAcceptingText == true ||
            ViewCompat.getRootWindowInsets(window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
    }

    @SuppressLint("RestrictedApi") // Route synthesized stick navigation through the focused Compose tree.
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
