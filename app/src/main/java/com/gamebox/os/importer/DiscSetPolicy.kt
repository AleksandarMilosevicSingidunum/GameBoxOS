package com.gamebox.os.importer

import java.io.File

internal object DiscSetPolicy {
    fun selectLaunchFile(directory: File, names: List<String>): String {
        val descriptors = names.filter {
            it.substringAfterLast('.', "").lowercase() in setOf("cue", "gdi", "mds", "ccd")
        }
        require(descriptors.size <= 1) { "Select one disc descriptor (.CUE, .GDI, .MDS, or .CCD) per import" }
        if (descriptors.isNotEmpty()) {
            validateDescriptor(directory.resolve(descriptors.single()), names)
            return descriptors.single()
        }
        require(names.size == 1) {
            "Multiple track files require one .CUE, .GDI, .MDS, or .CCD descriptor"
        }
        return names.single()
    }

    private fun validateDescriptor(descriptor: File, selectedNames: List<String>) {
        require(descriptor.isFile) { "Disc descriptor is missing" }
        require(descriptor.length() <= 2L * 1024 * 1024) { "Disc descriptor is too large" }
        val selected = selectedNames.mapTo(mutableSetOf()) { it.lowercase() }
        val references = when (descriptor.extension.lowercase()) {
            "cue" -> cueReferences(descriptor)
            "gdi" -> gdiReferences(descriptor)
            "mds" -> listOf(descriptor.nameWithoutExtension + ".mdf")
            "ccd" -> listOf(descriptor.nameWithoutExtension + ".img")
            else -> emptyList()
        }
        references.forEach { reference ->
            require('/' !in reference && '\\' !in reference && reference != "." && reference != "..") {
                "Disc descriptor contains an unsafe track path"
            }
            require(reference.lowercase() in selected) {
                "Disc descriptor references a file that was not selected: $reference"
            }
        }
    }

    private fun cueReferences(descriptor: File): List<String> {
        val pattern = Regex("""(?im)^\s*FILE\s+(?:\"([^\"]+)\"|(\S+))""")
        return pattern.findAll(descriptor.readText()).map { match ->
            match.groupValues[1].ifEmpty { match.groupValues[2] }
        }.toList().also {
            require(it.isNotEmpty()) { "CUE descriptor does not reference any track files" }
        }
    }

    private fun gdiReferences(descriptor: File): List<String> {
        val pattern = Regex("""^\s*\d+\s+\d+\s+\d+\s+\d+\s+(?:\"([^\"]+)\"|(\S+))\s+\d+\s*$""")
        return descriptor.readLines().drop(1).mapNotNull { line ->
            pattern.matchEntire(line)?.let { match ->
                match.groupValues[1].ifEmpty { match.groupValues[2] }
            }
        }.also {
            require(it.isNotEmpty()) { "GDI descriptor does not reference any track files" }
        }
    }
}
