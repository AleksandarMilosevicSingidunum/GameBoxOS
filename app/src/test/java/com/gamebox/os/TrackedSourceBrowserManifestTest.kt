package com.gamebox.os

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class TrackedSourceBrowserManifestTest {
    @Test
    fun browserActivityIsResizableForDesktopMode() {
        val activities = manifest().getElementsByTagName("activity")
        val browser = (0 until activities.length)
            .map { activities.item(it) as Element }
            .single { it.getAttributeNS(ANDROID_NAMESPACE, "name") == ".TrackedSourceBrowserActivity" }

        assertEquals("true", browser.getAttributeNS(ANDROID_NAMESPACE, "resizeableActivity"))
    }

    @Test
    fun standaloneEmulatorsAreVisibleToPackageManager() {
        val visiblePackages = manifest().getElementsByTagName("package")
            .let { packages -> (0 until packages.length).map { packages.item(it) as Element } }
            .map { it.getAttributeNS(ANDROID_NAMESPACE, "name") }

        listOf(
            "com.explusalpha.NesEmu",
            "com.explusalpha.Snes9xPlus",
            "com.explusalpha.GbcEmu",
            "com.explusalpha.GbaEmu",
            "com.explusalpha.MdEmu",
            "com.explusalpha.A2600Emu",
        ).forEach { packageName ->
            assertTrue("$packageName must be package-visible", packageName in visiblePackages)
        }
    }

    private fun manifest() = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(File("src/main/AndroidManifest.xml"))

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
