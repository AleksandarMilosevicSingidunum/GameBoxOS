package com.gamebox.os.ui

import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus

internal data class BrowserDownloadSections(
    val active: List<BrowserDownloadRecord>,
    val completed: List<BrowserDownloadRecord>,
)

/** Keeps fast imports and cancelled jobs visible in Download Manager history. */
internal fun browserDownloadSections(records: List<BrowserDownloadRecord>) =
    BrowserDownloadSections(
        active = records.filter { it.status !in setOf(BrowserDownloadStatus.IMPORTED, BrowserDownloadStatus.CANCELLED) },
        completed = records.filter { it.status in setOf(BrowserDownloadStatus.IMPORTED, BrowserDownloadStatus.CANCELLED) },
    )

internal fun shouldShowCompletedDownloadsByDefault(sections: BrowserDownloadSections): Boolean =
    sections.active.isEmpty() && sections.completed.isNotEmpty()
