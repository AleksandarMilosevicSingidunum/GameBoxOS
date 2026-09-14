package com.gamebox.os.ui

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesktopHomeIntentTest {
    @Test
    fun desktopEscapeTargetsTheDeviceHomeSurface() {
        val intent = desktopHomeIntent()

        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertNotNull(intent.categories)
        assertTrue(Intent.CATEGORY_HOME in intent.categories.orEmpty())
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertEquals(null, intent.component)
        assertEquals(null, intent.getPackage())
    }
}
