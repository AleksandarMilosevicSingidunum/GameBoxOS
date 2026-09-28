package com.gamebox.os.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.app.role.RoleManager

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

/** Prefer the system's direct HOME-role chooser so console mode can own the TV surface. */
internal fun defaultHomeSelectionIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true &&
            roleManager.isRoleHeld(RoleManager.ROLE_HOME).not()
        ) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
    }
    return defaultHomeSettingsIntent()
}
