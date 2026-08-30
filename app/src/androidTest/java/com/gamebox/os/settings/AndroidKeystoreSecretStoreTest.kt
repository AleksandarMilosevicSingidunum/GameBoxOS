package com.gamebox.os.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreSecretStoreTest {
    @Test
    fun roundTripsAndClearsEncryptedProviderSecret() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AndroidKeystoreSecretStore(context)
        val name = "instrumentation_provider_key"

        store.put(name, "  test-secret-value  ")
        assertTrue(store.contains(name))
        assertEquals("test-secret-value", store.get(name))

        store.put(name, null)
        assertFalse(store.contains(name))
        assertEquals(null, store.get(name))
    }
}
