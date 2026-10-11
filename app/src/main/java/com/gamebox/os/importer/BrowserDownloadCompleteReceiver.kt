package com.gamebox.os.importer

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gamebox.os.GameBoxApplication

/** Resumes a tracked transfer's verify/import work when Android reports completion. */
class BrowserDownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId <= 0L) return
        val app = context.applicationContext as? GameBoxApplication ?: return
        app.browserDownloadCoordinator.resumeAfterSystemCompletion(downloadId)
    }
}
