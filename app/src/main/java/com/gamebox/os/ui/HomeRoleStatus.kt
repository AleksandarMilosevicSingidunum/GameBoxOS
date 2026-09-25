package com.gamebox.os.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

internal fun isGameBoxDefaultHome(context: Context): Boolean {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolvedPackage = context.packageManager
        .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo
        ?.packageName
    return defaultHomeMatchesApp(resolvedPackage, context.packageName)
}

internal fun defaultHomeMatchesApp(resolvedPackage: String?, appPackage: String): Boolean =
    !resolvedPackage.isNullOrBlank() && resolvedPackage == appPackage

internal fun defaultHomeSettingsIntent(): Intent = Intent(Settings.ACTION_HOME_SETTINGS)
