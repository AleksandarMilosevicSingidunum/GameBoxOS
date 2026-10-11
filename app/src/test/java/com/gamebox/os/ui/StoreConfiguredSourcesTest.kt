package com.gamebox.os.ui

import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreConfiguredSourcesTest {
    @Test
    fun selectedConsoleShowsOnlyEnabledCompatibleInAppSources() {
        val sources = listOf(
            source("vimm-psp", GameSourceProviderType.VIMM_LAIR, platforms = setOf("PSP")),
            source("vimm-3ds", GameSourceProviderType.VIMM_LAIR, platforms = setOf("3DS")),
            source("web-psp", GameSourceProviderType.EXTERNAL_WEB, platforms = setOf("PlayStation Portable")),
            source("json-psp", GameSourceProviderType.GAMEBOX_JSON, platforms = setOf("PSP")),
            source("disabled", GameSourceProviderType.GAMEBOX_JSON, enabled = false),
            source("webdav", GameSourceProviderType.WEBDAV),
            source("s3", GameSourceProviderType.S3),
        )

        assertEquals(
            listOf("vimm-psp", "web-psp", "json-psp"),
            configuredSourcesForStore(sources, storeConsole("psp")).map { it.id },
        )
    }

    @Test
    fun vimmIsHiddenForConsolesWithoutASupportedVimmRoute() {
        val sources = listOf(
            source("vimm", GameSourceProviderType.VIMM_LAIR),
            source("web", GameSourceProviderType.EXTERNAL_WEB),
        )

        assertEquals(
            listOf("web"),
            configuredSourcesForStore(sources, storeConsole("3ds")).map { it.id },
        )
    }

    private fun storeConsole(key: String) = requireNotNull(storeConsoles.firstOrNull { it.key == key })

    private fun source(
        id: String,
        type: GameSourceProviderType,
        platforms: Set<String> = emptySet(),
        enabled: Boolean = true,
    ) = GameSourceConfig(
        id = id,
        name = id,
        type = type,
        baseUrl = if (type == GameSourceProviderType.VIMM_LAIR) "https://vimm.net" else "https://example.org",
        enabled = enabled,
        platforms = platforms,
    )
}
