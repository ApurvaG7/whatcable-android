package com.whatcable.android.data.shizuku

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuSetupHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isShizukuInstalled: Boolean
        get() = try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    val isSuiAvailable: Boolean
        get() = try {
            context.packageManager.getPackageInfo("com.topjohnwu.magisk", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    val shizukuPlayStoreUri: String
        get() = "market://details?id=moe.shizuku.privileged.api"
}
