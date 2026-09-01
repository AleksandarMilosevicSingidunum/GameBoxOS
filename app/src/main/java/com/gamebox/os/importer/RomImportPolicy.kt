package com.gamebox.os.importer

import com.gamebox.os.domain.GameId

object RomImportPolicy {
    private data class FormatProfile(
        val label: String,
        val aliases: Set<String>,
        val extensions: Set<String>,
    )

    private fun profile(label: String, aliases: Set<String>, vararg extensions: String) =
        FormatProfile(label, aliases.map(::normalizePlatform).toSet(), extensions.toSet())

    private val profiles = listOf(
        profile("Nintendo Entertainment System", setOf("NES", "Famicom", "Nintendo Entertainment System"), "nes", "unf", "unif", "fds", "zip", "7z"),
        profile("Super Nintendo", setOf("SNES", "Super Famicom", "Super Nintendo Entertainment System"), "sfc", "smc", "fig", "swc", "bs", "zip", "7z"),
        profile("Nintendo 64", setOf("N64", "Nintendo 64"), "z64", "n64", "v64", "zip", "7z"),
        profile("Game Boy", setOf("GB", "Game Boy", "Nintendo Game Boy"), "gb", "sgb", "zip", "7z"),
        profile("Game Boy Color", setOf("GBC", "Game Boy Color", "Nintendo Game Boy Color"), "gbc", "sgb", "zip", "7z"),
        profile("Game Boy Advance", setOf("GBA", "Game Boy Advance", "Nintendo Game Boy Advance"), "gba", "agb", "zip", "7z"),
        profile("Nintendo DS", setOf("NDS", "Nintendo DS"), "nds", "dsi", "zip", "7z"),
        profile("Nintendo 3DS", setOf("3DS", "Nintendo 3DS"), "3ds", "cci", "cxi", "cia", "3dsx"),
        profile("Nintendo GameCube", setOf("GameCube", "Nintendo GameCube"), "iso", "gcm", "rvz", "gcz", "wia", "ciso"),
        profile("Nintendo Wii", setOf("Wii", "Nintendo Wii"), "iso", "wbfs", "rvz", "gcz", "wia", "wad", "ciso"),
        profile("Nintendo Wii U", setOf("Wii U", "Nintendo Wii U"), "wud", "wux", "wua", "rpx"),
        profile("Nintendo Switch", setOf("Switch", "Nintendo Switch"), "xci", "xcz", "nsp", "nsz", "nca", "nro", "nso"),
        profile("PlayStation", setOf("PS1", "PSX", "PlayStation", "Sony PlayStation"), "cue", "bin", "chd", "pbp", "ccd", "img", "sub", "wav", "mdf", "mds"),
        profile("PlayStation 2", setOf("PS2", "PlayStation 2", "Sony PlayStation 2"), "iso", "chd", "cso", "bin", "cue", "wav", "mdf", "mds", "nrg"),
        profile("PlayStation 3", setOf("PS3", "PlayStation 3", "Sony PlayStation 3"), "iso", "pkg"),
        profile("PSP", setOf("PSP", "PlayStation Portable", "Sony PlayStation Portable"), "iso", "cso", "pbp"),
        profile("PlayStation Vita", setOf("PS Vita", "PlayStation Vita", "Sony PlayStation Vita"), "vpk", "pkg"),
        profile("Sega Master System", setOf("Master System", "Sega Master System"), "sms", "zip", "7z"),
        profile("Sega Game Gear", setOf("Game Gear", "Sega Game Gear"), "gg", "zip", "7z"),
        profile("Sega Genesis / Mega Drive", setOf("Genesis", "Mega Drive", "Sega Genesis", "Sega Mega Drive"), "md", "gen", "smd", "bin", "zip", "7z"),
        profile("Sega CD", setOf("Sega CD", "Mega CD"), "cue", "bin", "raw", "wav", "chd", "iso"),
        profile("Sega Saturn", setOf("Saturn", "Sega Saturn"), "cue", "bin", "raw", "wav", "chd", "iso", "mdf", "mds"),
        profile("Sega Dreamcast", setOf("Dreamcast", "Sega Dreamcast"), "gdi", "cdi", "chd", "cue", "bin", "raw", "wav"),
        profile("Xbox", setOf("Xbox", "Microsoft Xbox"), "iso", "xbe"),
        profile("Xbox 360", setOf("Xbox 360", "Microsoft Xbox 360"), "iso", "xex"),
        profile("PC Engine / TurboGrafx-16", setOf("PC Engine", "TurboGrafx 16", "TurboGrafx-16"), "pce", "sgx", "cue", "bin", "chd", "zip", "7z"),
        profile("Neo Geo", setOf("Neo Geo", "SNK Neo Geo", "Neo Geo Pocket", "Neo Geo Pocket Color"), "zip", "7z", "ngp", "ngc"),
        profile("Atari", setOf("Atari 2600", "Atari 5200", "Atari 7800", "Atari Jaguar", "Atari Lynx"), "a26", "a52", "a78", "j64", "jag", "lnx", "zip", "7z"),
        profile("WonderSwan", setOf("WonderSwan", "WonderSwan Color"), "ws", "wsc", "zip", "7z"),
        profile("Arcade", setOf("Arcade", "MAME"), "zip", "7z", "chd"),
        profile("Homebrew", setOf("Homebrew"), "zip", "7z", "rom", "elf", "dol", "nro", "3dsx", "wad", "apk"),
        profile("Retro", setOf("Retro"), "nes", "sfc", "smc", "gb", "gbc", "gba", "n64", "z64", "v64", "md", "gen", "sms", "gg", "pce", "zip", "7z", "rom"),
    )

    private val knownExtensions = profiles.flatMapTo(sortedSetOf<String>()) { it.extensions }

    fun profileLabel(platform: String?): String =
        profileFor(platform)?.label ?: platform?.trim()?.takeIf(String::isNotEmpty) ?: "Unknown platform"

    fun supportedExtensions(platform: String?): Set<String> =
        profileFor(platform)?.extensions ?: knownExtensions

    fun supportedExtensionsLabel(platform: String?, limit: Int = 12): String {
        require(limit > 0)
        val formats = supportedExtensions(platform).sorted()
        val visible = formats.take(limit).joinToString(", ") { ".${it.uppercase()}" }
        return if (formats.size > limit) "$visible, and ${formats.size - limit} more" else visible
    }

    fun supportsMultiFile(platform: String?): Boolean =
        supportedExtensions(platform).any { it in setOf("cue", "gdi", "mds", "ccd") }

    fun safeFileName(value: String, platform: String? = null): String {
        val name = value.trim().substringAfterLast('/').substringAfterLast('\\')
        require(name.length in 1..180) { "Invalid import filename" }
        require(name.none { it.code < 32 }) { "Invalid import filename" }
        require(name != "." && name != "..") { "Invalid import filename" }
        val extension = name.substringAfterLast('.', "").lowercase()
        val supported = supportedExtensions(platform)
        require(extension in supported) {
            val shownExtension = extension.takeIf(String::isNotEmpty)?.let { ".$it" } ?: "Files without an extension"
            "$shownExtension is not supported for ${profileLabel(platform)}. Supported formats: " +
                supportedExtensionsLabel(platform)
        }
        return name
    }

    fun relativePath(gameId: GameId, fileName: String, platform: String? = null): String {
        val id = gameId.value
        require(id.matches(Regex("[a-z0-9][a-z0-9-]{0,95}"))) { "Invalid game id" }
        return "imports/$id/" + safeFileName(fileName, platform)
    }

    fun importRootRelativePath(gameId: GameId, storedRelativePath: String): String {
        val expectedPrefix = "imports/${gameId.value}/"
        require(storedRelativePath.startsWith(expectedPrefix)) { "Import path is outside the selected game" }
        val relative = storedRelativePath.removePrefix("imports/")
        require(relative.none { it == '\\' } && relative.split('/').none { it == ".." || it.isEmpty() }) {
            "Import path is unsafe"
        }
        return relative
    }

    fun mimeType(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "zip" -> "application/zip"
        "7z" -> "application/x-7z-compressed"
        "iso" -> "application/x-iso9660-image"
        "chd" -> "application/x-chd"
        "cue" -> "application/x-cue"
        "cso", "ciso" -> "application/x-compressed-iso"
        "pbp" -> "application/x-playstation-pbp"
        "rvz" -> "application/x-dolphin-rvz"
        "gcz" -> "application/x-dolphin-gcz"
        "wia" -> "application/x-dolphin-wia"
        "wbfs" -> "application/x-wbfs"
        "gcm" -> "application/x-gamecube-rom"
        "gdi" -> "application/x-dreamcast-gdi"
        "cdi" -> "application/x-discjuggler-cdi"
        "nes" -> "application/x-nes-rom"
        "sfc", "smc" -> "application/x-snes-rom"
        "gb" -> "application/x-gameboy-rom"
        "gbc" -> "application/x-gameboy-color-rom"
        "gba" -> "application/x-gba-rom"
        "n64", "z64", "v64" -> "application/x-n64-rom"
        "3ds", "cci" -> "application/x-nintendo-3ds-rom"
        "cxi" -> "application/x-nintendo-3ds-cxi"
        "cia" -> "application/x-nintendo-3ds-cia"
        "3dsx" -> "application/x-nintendo-3ds-homebrew"
        "xci" -> "application/x-nintendo-switch-xci"
        "xcz" -> "application/x-nintendo-switch-xcz"
        "nsp" -> "application/x-nintendo-switch-nsp"
        "nsz" -> "application/x-nintendo-switch-nsz"
        "nca" -> "application/x-nintendo-switch-nca"
        "nro" -> "application/x-nintendo-switch-homebrew"
        "apk" -> "application/vnd.android.package-archive"
        else -> "application/octet-stream"
    }

    private fun profileFor(platform: String?): FormatProfile? {
        val normalized = platform?.let(::normalizePlatform).orEmpty()
        return profiles.firstOrNull { normalized in it.aliases }
    }

    private fun normalizePlatform(value: String): String =
        value.lowercase().filter(Char::isLetterOrDigit)
}
