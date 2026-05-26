package com.whatcable.android.data.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuSetupHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }

    val isShizukuInstalled: Boolean
        get() = try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    fun openPlayStore(): Intent {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHIZUKU_PACKAGE"))
        if (marketIntent.resolveActivity(context.packageManager) != null) {
            return marketIntent
        }
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE")
        )
    }

    fun openShizukuApp(): Intent? {
        return context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
    }

    fun openAppSettings(): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
    }
}
