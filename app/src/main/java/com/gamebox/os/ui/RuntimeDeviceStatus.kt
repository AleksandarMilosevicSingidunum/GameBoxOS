package com.gamebox.os.ui

import android.content.Context
import android.hardware.input.InputManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

internal enum class GameBoxNetworkState {
    OFFLINE, LOCAL, INTERNET
}

internal data class RuntimeDeviceStatus(
    val controllerNames: List<String> = emptyList(),
    val audioOutputNames: List<String> = emptyList(),
    val networkState: GameBoxNetworkState = GameBoxNetworkState.OFFLINE,
) {
    val controllerLabel: String
        get() = when (controllerNames.size) {
            0 -> "Not connected"
            1 -> controllerNames.single()
            else -> controllerNames.size.toString() + " controllers"
        }

    val audioOutputLabel: String
        get() = audioOutputNames.firstOrNull() ?: "System default"

    val networkLabel: String
        get() = when (networkState) {
            GameBoxNetworkState.OFFLINE -> "Offline"
            GameBoxNetworkState.LOCAL -> "Local network"
            GameBoxNetworkState.INTERNET -> "Internet connected"
        }
}

internal val LocalRuntimeDeviceStatus = staticCompositionLocalOf { RuntimeDeviceStatus() }

@Composable
internal fun rememberRuntimeDeviceStatus(): RuntimeDeviceStatus {
    val context = LocalContext.current.applicationContext
    var revision by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val handler = Handler(Looper.getMainLooper())
        val inputManager = context.getSystemService(InputManager::class.java)
        val audioManager = context.getSystemService(AudioManager::class.java)
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val changed: () -> Unit = { revision += 1 }

        val inputListener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) = changed()
            override fun onInputDeviceRemoved(deviceId: Int) = changed()
            override fun onInputDeviceChanged(deviceId: Int) = changed()
        }
        val audioCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = changed()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = changed()
        }
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = changed()
            override fun onLost(network: Network) = changed()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = changed()
        }

        inputManager?.registerInputDeviceListener(inputListener, handler)
        audioManager?.registerAudioDeviceCallback(audioCallback, handler)
        runCatching { connectivityManager?.registerDefaultNetworkCallback(networkCallback, handler) }

        onDispose {
            inputManager?.unregisterInputDeviceListener(inputListener)
            audioManager?.unregisterAudioDeviceCallback(audioCallback)
            runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        }
    }

    return remember(context, revision) { readRuntimeDeviceStatus(context) }
}

private fun readRuntimeDeviceStatus(context: Context): RuntimeDeviceStatus {
    val controllers = InputDevice.getDeviceIds().asSequence()
        .mapNotNull(InputDevice::getDevice)
        .filter { device ->
            device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        }
        .map { it.name.takeIf(String::isNotBlank) ?: "Game controller" }
        .distinct()
        .sorted()
        .toList()

    val audioManager = context.getSystemService(AudioManager::class.java)
    val audioOutputs = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
        .asSequence()
        .filterNot { it.type == AudioDeviceInfo.TYPE_TELEPHONY }
        .map { device ->
            device.productName?.toString()?.takeIf(String::isNotBlank)
                ?: audioTypeLabel(device.type)
        }
        .distinct()
        .toList()

    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val capabilities = runCatching {
        connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
    }.getOrNull()
    val networkState = when {
        capabilities == null -> GameBoxNetworkState.OFFLINE
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ->
            GameBoxNetworkState.INTERNET
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
            GameBoxNetworkState.LOCAL
        else -> GameBoxNetworkState.OFFLINE
    }

    return RuntimeDeviceStatus(controllers, audioOutputs, networkState)
}

internal fun audioTypeLabel(type: Int): String = when (type) {
    AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI"
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_BLE_BROADCAST -> "Bluetooth audio"
    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB audio"
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired audio"
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Device speaker"
    else -> "Audio output"
}
