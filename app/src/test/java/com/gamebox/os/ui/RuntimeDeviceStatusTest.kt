package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeDeviceStatusTest {
    @Test
    fun statusLabelsDistinguishOfflineLocalAndInternet() {
        assertEquals("Offline", RuntimeDeviceStatus(networkState = GameBoxNetworkState.OFFLINE).networkLabel)
        assertEquals("Local network", RuntimeDeviceStatus(networkState = GameBoxNetworkState.LOCAL).networkLabel)
        assertEquals("Internet connected", RuntimeDeviceStatus(networkState = GameBoxNetworkState.INTERNET).networkLabel)
    }

    @Test
    fun networkTransportLabelsExposeEthernetAndWifiSeparately() {
        assertEquals("No active transport", RuntimeDeviceStatus().networkTransportLabel)
        assertEquals(
            "Ethernet",
            RuntimeDeviceStatus(networkTransport = GameBoxNetworkTransport.ETHERNET).networkTransportLabel,
        )
        assertEquals(
            "Wi-Fi",
            RuntimeDeviceStatus(networkTransport = GameBoxNetworkTransport.WIFI).networkTransportLabel,
        )
    }

    @Test
    fun controllerLabelDescribesZeroOneAndMultipleDevices() {
        assertEquals("Not connected", RuntimeDeviceStatus().controllerLabel)
        assertEquals("Living room pad", RuntimeDeviceStatus(controllerNames = listOf("Living room pad")).controllerLabel)
        assertEquals("2 controllers", RuntimeDeviceStatus(controllerNames = listOf("Pad 1", "Pad 2")).controllerLabel)
    }

    @Test
    fun audioOutputFallsBackWithoutInventingARoute() {
        assertEquals("System default", RuntimeDeviceStatus().audioOutputLabel)
        assertEquals("HDMI monitor", RuntimeDeviceStatus(audioOutputNames = listOf("HDMI monitor")).audioOutputLabel)
    }

    @Test
    fun displayLabelDistinguishesNoOneAndMultipleExternalDisplays() {
        assertEquals("No secondary display detected", RuntimeDeviceStatus().externalDisplayLabel)
        assertEquals(
            "Living room TV · 1920×1080",
            RuntimeDeviceStatus(externalDisplayNames = listOf("Living room TV · 1920×1080")).externalDisplayLabel,
        )
        assertEquals(
            "2 external displays",
            RuntimeDeviceStatus(externalDisplayNames = listOf("TV", "Capture")).externalDisplayLabel,
        )
    }

    @Test
    fun powerLabelReportsBatteryPercentAndUsbCharging() {
        assertEquals(
            "73% · Charging over USB",
            RuntimeDeviceStatus(
                batteryPercent = 73,
                charging = true,
                powerSource = GameBoxPowerSource.USB,
            ).powerLabel,
        )
        assertEquals(
            "41% · On battery",
            RuntimeDeviceStatus(
                batteryPercent = 41,
                charging = false,
                powerSource = GameBoxPowerSource.BATTERY,
            ).powerLabel,
        )
    }
}
