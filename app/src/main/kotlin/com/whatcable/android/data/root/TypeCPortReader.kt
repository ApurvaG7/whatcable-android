package com.whatcable.android.data.root

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TypeCPortReader @Inject constructor(
    private val su: SuExecutor
) {

    data class TypeCPortState(
        val portPath: String,
        val orientation: String?,
        val dataRole: String?,
        val powerRole: String?,
        val usbCapability: String?,
        val powerOperationMode: String?,
        val pdRevision: String?,
        val vconnSource: Boolean?
    )

    private var cachedPortPath: String? = null

    suspend fun readPort(): TypeCPortState? {
        val portPath = findPortPath() ?: return null
        return TypeCPortState(
            portPath = portPath,
            orientation = readNode(portPath, "orientation"),
            dataRole = readNode(portPath, "data_role"),
            powerRole = readNode(portPath, "power_role"),
            usbCapability = readNode(portPath, "usb_capability"),
            powerOperationMode = readNode(portPath, "power_operation_mode"),
            pdRevision = readNode(portPath, "usb_power_delivery_revision"),
            vconnSource = readNode(portPath, "vconn_source")?.let { it == "yes" }
        )
    }

    private suspend fun findPortPath(): String? {
        cachedPortPath?.let { path ->
            val check = su.readFile("$path/type")
            if (check != null) return path
            cachedPortPath = null
        }

        val candidates = listOf(
            "/sys/class/typec/port0",
            "/sys/class/typec/port1",
            "/sys/devices/platform/typec/port0"
        )

        for (path in candidates) {
            val result = su.readFile("$path/data_role")
            if (result != null) {
                cachedPortPath = path
                return path
            }
        }

        val listing = su.listDirectory("/sys/class/typec")
        val port = listing.firstOrNull { it.startsWith("port") && !it.contains("-") }
        if (port != null) {
            val path = "/sys/class/typec/$port"
            cachedPortPath = path
            return path
        }

        return null
    }

    private suspend fun readNode(base: String, node: String): String? {
        val content = su.readFile("$base/$node") ?: return null
        return parseActiveValue(content)
    }

    private fun parseActiveValue(content: String): String {
        val bracketMatch = Regex("\\[(.+?)]").find(content)
        if (bracketMatch != null) return bracketMatch.groupValues[1]
        return content.trim()
    }
}
