package com.gamebox.os.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.gamebox.os.domain.normalizeCatalogTitle

/** A console shown by the Store's catalog/discovery filter. */
internal data class StoreConsole(
    val key: String,
    val label: String,
    val aliases: Set<String>,
    val icon: ImageVector,
    val cardLabel: String = label,
)

/** Shared console mapping for Store filters, metadata discovery, and source search. */
internal val storeConsoles = listOf(
    StoreConsole("dreamcast", "Dreamcast", setOf("segadreamcast", "dreamcast"), Icons.Rounded.SportsEsports),
    StoreConsole("gba", "Game Boy Advance", setOf("nintendogameboyadvance", "gameboyadvance", "gba"), Icons.Rounded.SportsEsports, cardLabel = "GBA"),
    StoreConsole("gbc", "Game Boy Color", setOf("nintendogameboycolor", "gameboycolor", "gbc"), Icons.Rounded.SportsEsports, cardLabel = "GBC"),
    StoreConsole("gamecube", "GameCube", setOf("nintendogamecube", "gamecube"), Icons.Rounded.SportsEsports),
    StoreConsole("n64", "N64", setOf("nintendo64", "n64"), Icons.Rounded.SportsEsports),
    StoreConsole("nes", "NES", setOf("nintendoentertainmentsystem", "nintendoentertainmentsystemnes", "nes", "famicom"), Icons.Rounded.SportsEsports),
    StoreConsole("ps1", "PS1", setOf("sonyplaystation", "sonyplaystation1", "playstation", "playstation1", "ps1", "psx"), Icons.Rounded.SportsEsports),
    StoreConsole("ps2", "PS2", setOf("sonyplaystation2", "playstation2", "ps2"), Icons.Rounded.SportsEsports),
    StoreConsole("ps3", "PS3", setOf("sonyplaystation3", "playstation3", "ps3"), Icons.Rounded.SportsEsports),
    StoreConsole("psp", "PSP", setOf("sonyplaystationportable", "playstationportable", "psp"), Icons.Rounded.SportsEsports),
    StoreConsole("snes", "SNES", setOf("nintendosupernintendoentertainmentsystem", "supernintendoentertainmentsystem", "supernintendo", "superfamicom", "snes"), Icons.Rounded.SportsEsports),
    StoreConsole("wii", "Wii", setOf("nintendowii", "wii"), Icons.Rounded.SportsEsports),
    StoreConsole("wiiware", "WiiWare", setOf("nintendowiiware", "wiiware"), Icons.Rounded.SportsEsports),
    StoreConsole("xbox", "Xbox", setOf("microsoftxbox", "xbox"), Icons.Rounded.SportsEsports),
    StoreConsole("xbox360", "Xbox 360", setOf("microsoftxbox360", "xbox360"), Icons.Rounded.SportsEsports),
    StoreConsole("ds", "DS", setOf("nintendods", "nintendodsi", "ds", "nds", "dsi"), Icons.Rounded.SportsEsports),
    StoreConsole("3ds", "3DS", setOf("nintendo3ds", "3ds"), Icons.Rounded.SportsEsports),
    StoreConsole("switch", "Switch", setOf("nintendoswitch", "switch"), Icons.Rounded.SportsEsports),
    StoreConsole("homebrew", "Homebrew", emptySet(), Icons.Rounded.Code),
)

internal fun storeConsoleMatches(console: StoreConsole, platformName: String): Boolean =
    normalizeCatalogTitle(platformName) in console.aliases

@Composable
internal fun StoreConsoleFilterRow(
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    sectionSemantics: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).then(
            if (sectionSemantics) Modifier.semantics {
                contentDescription = "Store console filters"
            } else Modifier
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BlueprintFilterChip(
            selected = selectedKey == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
            modifier = Modifier.semantics { contentDescription = "Store console filter: All" },
        )
        storeConsoles.forEach { console ->
            val selected = selectedKey == console.key
            BlueprintFilterChip(
                selected = selected,
                onClick = { onSelect(console.key) },
                leadingIcon = {
                    ConsoleBrandMark(
                        key = console.key,
                        selected = selected,
                        modifier = Modifier.size(17.dp),
                    )
                },
                label = { Text(console.label) },
                modifier = Modifier.semantics {
                    contentDescription = "Store console filter: ${console.label}"
                },
            )
        }
    }
}
