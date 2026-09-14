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
}
