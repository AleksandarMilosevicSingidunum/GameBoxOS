package com.gamebox.os.importer

enum class RomImportStage(val label: String) {
    COPYING_AND_VERIFYING("Copying and verifying"),
    COPYING_ARCHIVE("Copying archive"),
    EXTRACTING_ARCHIVE("Extracting archive"),
    VERIFYING_EXTRACTED_FILE("Verifying extracted file"),
}

data class RomImportProgress(
    val stage: RomImportStage,
    val bytesProcessed: Long,
    val totalBytes: Long?,
    val fileIndex: Int,
    val fileCount: Int,
) {
    val fraction: Float?
        get() = totalBytes
            ?.takeIf { it > 0L }
            ?.let { (bytesProcessed.toDouble() / it).toFloat().coerceIn(0f, 1f) }

    val description: String
        get() = "${stage.label} · file $fileIndex of $fileCount"
}
