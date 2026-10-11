package com.gamebox.os.ui

import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus

internal data class BrowserDownloadSections(
    val active: List<BrowserDownloadRecord>,
    val completed: List<BrowserDownloadRecord>,
)

/** Keeps fast background imports visible in Download Manager's completed list. */
internal fun browserDownloadSections(records: List<BrowserDownloadRecord>) =
    BrowserDownloadSections(
        active = records.filter { it.status != BrowserDownloadStatus.IMPORTED },
        completed = records.filter { it.status == BrowserDownloadStatus.IMPORTED },
    )

internal fun shouldShowCompletedDownloadsByDefault(sections: BrowserDownloadSections): Boolean =
    sections.active.isEmpty() && sections.completed.isNotEmpty()
