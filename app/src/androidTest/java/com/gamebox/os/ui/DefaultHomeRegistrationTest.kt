package com.gamebox.os.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultHomeRegistrationTest {
    @Test
    fun gameBoxIsRegisteredAsHomeCandidate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        val candidates = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )

        assertTrue(
            "GameBox MainActivity must remain an Android HOME candidate",
            candidates.any { candidate ->
                candidate.activityInfo.packageName == context.packageName &&
                    candidate.activityInfo.name == MainActivity::class.java.name
            },
        )
    }
}
