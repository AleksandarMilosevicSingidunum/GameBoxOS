package com.gamebox.os.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalDisplayContentInsetsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun insetConversionOnlyAppliesValidExternalDisplayInsets() {
        assertEquals(43f, externalDisplayBottomInsetDp(true, 43, 1f), 0.01f)
        assertEquals(21.5f, externalDisplayBottomInsetDp(true, 43, 2f), 0.01f)
        assertEquals(0f, externalDisplayBottomInsetDp(false, 43, 1f), 0f)
        assertEquals(0f, externalDisplayBottomInsetDp(true, -1, 1f), 0f)
        assertEquals(0f, externalDisplayBottomInsetDp(true, 43, 0f), 0f)
    }

    @Test
    fun externalHostReservesReportedBottomOverlayWithoutChangingPhoneLayout() {
        var phoneContentHeight = Dp.Unspecified
        var externalContentHeight = Dp.Unspecified
        compose.setContent {
            val density = LocalDensity.current
            val bottomInset = remember { 40.dp }
            Column {
                Box(
                    Modifier.size(100.dp)
                        .externalDisplaySystemOverlayInsets(externalDisplay = false, bottomInset = bottomInset)
                        .onSizeChanged { phoneContentHeight = with(density) { it.height.toDp() } },
                )
                Box(
                    Modifier.size(100.dp)
                        .externalDisplaySystemOverlayInsets(externalDisplay = true, bottomInset = bottomInset)
                        .onSizeChanged { externalContentHeight = with(density) { it.height.toDp() } },
                )
            }
        }

        compose.runOnIdle {
            assertEquals(100f, phoneContentHeight.value, 0.1f)
            assertEquals(60f, externalContentHeight.value, 0.1f)
        }
    }
}
