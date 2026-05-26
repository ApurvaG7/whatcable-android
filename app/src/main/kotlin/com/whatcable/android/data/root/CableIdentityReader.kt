package com.whatcable.android.data.root

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CableIdentityReader @Inject constructor(
    private val su: SuExecutor,
    private val portReader: TypeCPortReader
) {

    data class CableIdentity(
        val idHeader: Long?,
        val certStat: Long?,
        val productTypeVdo1: Long?,
        val productTypeVdo2: Long?,
        val product: Long?,
        val cableType: CableType,
        val supportsUsb4: Boolean,
        val maxCurrentMa: Int?,
        val maxSpeedGbps: Double?
    )

    enum class CableType(val label: String) {
        PASSIVE("Passive"),
        ACTIVE("Active"),
        UNKNOWN("Unknown")
    }

    suspend fun readIdentity(): CableIdentity? {
        val portState = portReader.readPort() ?: return null
        val portPath = portState.portPath

        val cablePaths = listOf(
            "$portPath-cable",
            "$portPath-cable/identity"
        )

        var identityPath: String? = null
        for (path in cablePaths) {
            val check = su.readFile("$path/id_header")
            if (check != null) {
                identityPath = path
                break
            }
        }

        if (identityPath == null) {
            val cableDir = "$portPath-cable"
            val listing = su.listDirectory(cableDir)
            if (listing.contains("identity")) {
                identityPath = "$cableDir/identity"
            } else if (listing.contains("id_header")) {
                identityPath = cableDir
            }
        }

        if (identityPath == null) return null

        val idHeader = readHexNode(identityPath, "id_header")
        val certStat = readHexNode(identityPath, "cert_stat")
        val vdo1 = readHexNode(identityPath, "product_type_vdo1")
        val vdo2 = readHexNode(identityPath, "product_type_vdo2")
        val product = readHexNode(identityPath, "product")

        val cableType = parseCableType(vdo1)
        val supportsUsb4 = parseUsb4Support(vdo1)
        val maxCurrent = parseMaxCurrent(vdo1)
        val maxSpeed = parseMaxSpeed(vdo1, portState.usbCapability)

        return CableIdentity(
            idHeader = idHeader,
            certStat = certStat,
            productTypeVdo1 = vdo1,
            productTypeVdo2 = vdo2,
            product = product,
            cableType = cableType,
            supportsUsb4 = supportsUsb4,
            maxCurrentMa = maxCurrent,
            maxSpeedGbps = maxSpeed
        )
    }

    private suspend fun readHexNode(base: String, node: String): Long? {
        val content = su.readFile("$base/$node") ?: return null
        val trimmed = content.trim().removePrefix("0x")
        return try {
            trimmed.toLong(16)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun parseCableType(vdo1: Long?): CableType {
        if (vdo1 == null) return CableType.UNKNOWN
        // Cable type is determined by which VDO format the kernel exposes.
        // Bit 27 in passive cable VDO1 is reserved (0), in active it indicates VDO version.
        // Heuristic: if bit 27 is set, likely active cable VDO format.
        val bit27 = ((vdo1 shr 27) and 0x01).toInt()
        return if (bit27 == 0) CableType.PASSIVE else CableType.ACTIVE
    }

    private fun parseUsb4Support(vdo1: Long?): Boolean {
        if (vdo1 == null) return false
        // USB PD spec: bits 2:0 = USB Highest Speed
        // 011b = USB4 Gen 3, 010b = USB4 Gen 2
        val speedBits = (vdo1 and 0x07).toInt()
        return speedBits >= 2
    }

    private fun parseMaxCurrent(vdo1: Long?): Int? {
        if (vdo1 == null) return null
        // Bits 6:5 indicate max current capability
        val currentBits = ((vdo1 shr 5) and 0x03).toInt()
        return when (currentBits) {
            1 -> 3000 // 3A
            2 -> 5000 // 5A
            else -> null
        }
    }

    private fun parseMaxSpeed(vdo1: Long?, usbCapability: String?): Double? {
        usbCapability?.let { cap ->
            return when {
                cap.contains("usb4-gen3") -> 40.0
                cap.contains("usb4-gen2") -> 20.0
                cap.contains("usb3-gen2x2") -> 20.0
                cap.contains("usb3-gen2") -> 10.0
                cap.contains("usb3-gen1") -> 5.0
                cap.contains("usb2") -> 0.48
                else -> null
            }
        }

        if (vdo1 == null) return null
        // USB PD spec Table 6-42: bits 2:0 = USB Highest Speed
        val speedBits = (vdo1 and 0x07).toInt()
        return when (speedBits) {
            0 -> 5.0   // USB 3.2 Gen 1
            1 -> 10.0  // USB 3.2 Gen 2
            2 -> 20.0  // USB4 Gen 2
            3 -> 40.0  // USB4 Gen 3
            else -> null
        }
    }
}
