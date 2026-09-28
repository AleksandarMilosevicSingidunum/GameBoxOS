package com.gamebox.os.storage

import java.io.File

/** Creates the stable per-console managed game tree on first app start. */
object GameStorageTree {
    private val consoles = listOf(
        "nintendo-entertainment-system", "super-nintendo", "nintendo-64", "game-boy",
        "game-boy-color", "game-boy-advance", "nintendo-ds", "nintendo-3ds",
        "nintendo-gamecube", "nintendo-wii", "nintendo-wii-u", "nintendo-switch",
        "playstation", "playstation-2", "playstation-3", "playstation-portable", "playstation-vita",
        "sega-master-system", "sega-game-gear", "sega-genesis-mega-drive", "sega-cd",
        "sega-saturn", "sega-dreamcast", "xbox", "xbox-360", "pc-engine-turbografx-16",
        "neo-geo", "atari", "wonderswan", "arcade", "homebrew", "retro",
    )

    fun ensure(filesDirectory: File) {
        val root = filesDirectory.resolve("imports")
        consoles.forEach { console ->
            root.resolve(console).resolve(".keep").apply {
                requireNotNull(parentFile).mkdirs()
                if (!exists()) writeText("GameBox managed $console directory\n")
            }
        }
    }
}
