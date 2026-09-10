package com.gamebox.os.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrandMarksTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeAndLibrarySteamNamesRenderTheSameGraphic() {
        compose.setContent {
            MaterialTheme {
                Row {
                    AppBrandMark("Steam", Modifier.size(48.dp).testTag("home-steam"))
                    AppBrandMark("Steam Library", Modifier.size(48.dp).testTag("library-steam"))
                }
            }
        }
        compose.onNodeWithText("ST", useUnmergedTree = true).assertDoesNotExist()
        val home = compose.onNodeWithTag("home-steam").captureToImage().asAndroidBitmap()
        val library = compose.onNodeWithTag("library-steam").captureToImage().asAndroidBitmap()
        assertTrue("Both launcher names must render the existing Steam graphic", home.sameAs(library))
    }
}

