package com.gamebox.os

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class PhoneDockDeploymentManifestTest {
    @Test
    fun launcherTargetsPhoneInstallWithoutAndroidTvRequirements() {
        val manifest = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File("src/main/AndroidManifest.xml"))

        val features = manifest.getElementsByTagName("uses-feature")
            .let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }
            .map { it.getAttributeNS(ANDROID_NAMESPACE, "name") }
        assertFalse("Phone install must not require Android TV software", "android.software.leanback" in features)
        assertFalse("Phone install must not require TV hardware", "android.hardware.type.television" in features)

        val filters = manifest.getElementsByTagName("intent-filter")
            .let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }
        val launcherFilters = filters.filter { filter ->
            val actions = filter.getElementsByTagName("action")
                .let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }
            val categories = filter.getElementsByTagName("category")
                .let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }
            actions.any { it.getAttributeNS(ANDROID_NAMESPACE, "name") == "android.intent.action.MAIN" } &&
                categories.any { it.getAttributeNS(ANDROID_NAMESPACE, "name") == "android.intent.category.LAUNCHER" }
        }
        assertEquals(1, launcherFilters.size)
        val allCategories = filters.flatMap { filter ->
            val nodes = filter.getElementsByTagName("category")
            (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID_NAMESPACE, "name") }
        }
        assertTrue("Phone app keeps its standard Android launcher entry", "android.intent.category.LAUNCHER" in allCategories)
        assertFalse("GameBox must not register as an Android TV launcher", "android.intent.category.LEANBACK_LAUNCHER" in allCategories)
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
