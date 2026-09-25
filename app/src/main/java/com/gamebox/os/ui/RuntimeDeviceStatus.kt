package com.gamebox.os.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.Display
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

internal enum class GameBoxNetworkTransport {
    NONE, ETHERNET, WIFI, CELLULAR, OTHER
}

internal enum class GameBoxPowerSource {
    UNKNOWN, BATTERY, USB, AC, WIRELESS
}

internal data class RuntimeDeviceStatus(
    val controllerNames: List<String> = emptyList(),
    val audioOutputNames: List<String> = emptyList(),
    val networkState: GameBoxNetworkState = GameBoxNetworkState.OFFLINE,
    val networkTransport: GameBoxNetworkTransport = GameBoxNetworkTransport.NONE,
    val externalDisplayNames: List<String> = emptyList(),
    val batteryPercent: Int? = null,
    val charging: Boolean = false,
    val powerSource: GameBoxPowerSource = GameBoxPowerSource.UNKNOWN,
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

    val networkTransportLabel: String
        get() = when (networkTransport) {
            GameBoxNetworkTransport.NONE -> "No active transport"
            GameBoxNetworkTransport.ETHERNET -> "Ethernet"
            GameBoxNetworkTransport.WIFI -> "Wi-Fi"
            GameBoxNetworkTransport.CELLULAR -> "Mobile network"
            GameBoxNetworkTransport.OTHER -> "Other network"
        }

    val externalDisplayLabel: String
        get() = when (externalDisplayNames.size) {
            0 -> "No secondary display detected"
            1 -> externalDisplayNames.single()
            else -> externalDisplayNames.size.toString() + " external displays"
        }

    val powerLabel: String
        get() {
            val percent = batteryPercent?.let { it.toString() + "% · " }.orEmpty()
            val source = when (powerSource) {
                GameBoxPowerSource.UNKNOWN -> if (charging) "Charging" else "Power state unavailable"
                GameBoxPowerSource.BATTERY -> "On battery"
                GameBoxPowerSource.USB -> if (charging) "Charging over USB" else "USB connected"
                GameBoxPowerSource.AC -> if (charging) "Charging from AC" else "AC connected"
                GameBoxPowerSource.WIRELESS -> if (charging) "Wireless charging" else "Wireless power connected"
            }
            return percent + source
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
        val displayManager = context.getSystemService(DisplayManager::class.java)
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
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = changed()
            override fun onDisplayRemoved(displayId: Int) = changed()
            override fun onDisplayChanged(displayId: Int) = changed()
        }
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = changed()
        }

        inputManager?.registerInputDeviceListener(inputListener, handler)
        audioManager?.registerAudioDeviceCallback(audioCallback, handler)
        displayManager?.registerDisplayListener(displayListener, handler)
        runCatching { connectivityManager?.registerDefaultNetworkCallback(networkCallback, handler) }
        runCatching {
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }

        onDispose {
            inputManager?.unregisterInputDeviceListener(inputListener)
            audioManager?.unregisterAudioDeviceCallback(audioCallback)
            displayManager?.unregisterDisplayListener(displayListener)
            runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
            runCatching { context.unregisterReceiver(batteryReceiver) }
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
    val networkTransport = networkTransport(capabilities)

    val displayManager = context.getSystemService(DisplayManager::class.java)
    val externalDisplays = displayManager?.displays.orEmpty()
        .asSequence()
        .filter { display ->
            display.displayId != Display.DEFAULT_DISPLAY &&
                display.state != Display.STATE_OFF
        }
        .map { display ->
            val mode = display.mode
            val name = display.name.takeIf(String::isNotBlank) ?: "External display"
            name + " · " + mode.physicalWidth + "×" + mode.physicalHeight
        }
        .distinct()
        .sorted()
        .toList()

    val battery = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull()
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPercent = if (level >= 0 && scale > 0) {
        ((level * 100f) / scale).toInt().coerceIn(0, 100)
    } else null
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val powerSource = when {
        plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> GameBoxPowerSource.USB
        plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> GameBoxPowerSource.AC
        plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> GameBoxPowerSource.WIRELESS
        plugged == 0 -> GameBoxPowerSource.BATTERY
        else -> GameBoxPowerSource.UNKNOWN
    }

    return RuntimeDeviceStatus(
        controllerNames = controllers,
        audioOutputNames = audioOutputs,
        networkState = networkState,
        networkTransport = networkTransport,
        externalDisplayNames = externalDisplays,
        batteryPercent = batteryPercent,
        charging = charging,
        powerSource = powerSource,
    )
}

internal fun networkTransport(capabilities: NetworkCapabilities?): GameBoxNetworkTransport = when {
    capabilities == null -> GameBoxNetworkTransport.NONE
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> GameBoxNetworkTransport.ETHERNET
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> GameBoxNetworkTransport.WIFI
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> GameBoxNetworkTransport.CELLULAR
    else -> GameBoxNetworkTransport.OTHER
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
