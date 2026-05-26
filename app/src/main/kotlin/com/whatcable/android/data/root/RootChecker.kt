package com.whatcable.android.data.root

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootChecker @Inject constructor() {

    val isRooted: Boolean
        get() = hasSuBinary() || hasMagisk()

    private fun hasSuBinary(): Boolean {
        val paths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/su/bin/su",
            "/apex/com.android.runtime/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun hasMagisk(): Boolean {
        return File("/data/adb/magisk").exists() ||
            File("/sbin/.magisk").exists()
    }
}
