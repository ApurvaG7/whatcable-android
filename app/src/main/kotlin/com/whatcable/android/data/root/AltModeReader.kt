package com.whatcable.android.data.root

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AltModeReader @Inject constructor(
    private val su: SuExecutor,
    private val portReader: TypeCPortReader
) {

    data class SysfsAltMode(
        val index: Int,
        val svid: Int,
        val mode: Int,
        val active: Boolean,
        val svidLabel: String
    )

    suspend fun readPartnerAltModes(): List<SysfsAltMode> {
        val portState = portReader.readPort() ?: return emptyList()
        val portPath = portState.portPath

        val partnerPaths = listOf(
            "$portPath-partner",
            "$portPath-partner/mode"
        )

        for (partnerPath in partnerPaths) {
            val modes = readAltModesFromPath(partnerPath)
            if (modes.isNotEmpty()) return modes
        }

        return emptyList()
    }

    suspend fun readCableAltModes(): List<SysfsAltMode> {
        val portState = portReader.readPort() ?: return emptyList()
        val cablePath = "${portState.portPath}-cable"

        return readAltModesFromPath(cablePath)
    }

    private suspend fun readAltModesFromPath(basePath: String): List<SysfsAltMode> {
        val listing = su.listDirectory(basePath)
        val altModeDirs = listing.filter { it.matches(Regex("altmode\\.\\d+|mode\\d+")) }

        if (altModeDirs.isEmpty()) return emptyList()

        return altModeDirs.mapIndexedNotNull { index, dir ->
            val dirPath = "$basePath/$dir"
            val svidStr = su.readFile("$dirPath/svid")?.trim()?.removePrefix("0x") ?: return@mapIndexedNotNull null
            val svid = try {
                svidStr.toInt(16)
            } catch (_: NumberFormatException) {
                return@mapIndexedNotNull null
            }

            val modeStr = su.readFile("$dirPath/mode")?.trim()?.removePrefix("0x") ?: "0"
            val mode = try { modeStr.toInt(16) } catch (_: NumberFormatException) { 0 }

            val activeStr = su.readFile("$dirPath/active")?.trim() ?: "no"
            val active = activeStr == "yes" || activeStr == "1"

            SysfsAltMode(
                index = index,
                svid = svid,
                mode = mode,
                active = active,
                svidLabel = svidToLabel(svid)
            )
        }
    }

    private fun svidToLabel(svid: Int): String = when (svid) {
        0xFF01 -> "DisplayPort"
        0x8087 -> "Thunderbolt"
        0x04B4 -> "Cypress"
        0x8086 -> "Intel"
        0x1D17 -> "MediaTek"
        0x2109 -> "VIA Labs"
        else -> "Vendor (0x${"%04X".format(svid)})"
    }
}
