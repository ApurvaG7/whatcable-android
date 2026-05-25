package com.whatcable.android.core.model

data class BosDescriptor(
    val totalLength: Int,
    val numDeviceCaps: Int,
    val capabilities: List<BosCapability>
) {
    val maxSpeed: UsbSpeedTier?
        get() {
            val ssp = capabilities.filterIsInstance<BosCapability.SuperSpeedPlus>().firstOrNull()
            if (ssp != null) return ssp.maxSpeed

            val ss = capabilities.filterIsInstance<BosCapability.SuperSpeed>().firstOrNull()
            if (ss != null) return ss.maxSpeed

            val usb2ext = capabilities.filterIsInstance<BosCapability.Usb2Extension>().firstOrNull()
            if (usb2ext != null) return UsbSpeedTier.HIGH_SPEED

            return null
        }
}

sealed class BosCapability {

    data class Usb2Extension(
        val attributes: Int
    ) : BosCapability() {
        val supportsLpm: Boolean get() = (attributes and 0x02) != 0
    }

    data class SuperSpeed(
        val attributes: Int,
        val speedsSupported: Int,
        val functionalitySupport: Int,
        val u1DevExitLat: Int,
        val u2DevExitLat: Int
    ) : BosCapability() {
        val supportsGen1: Boolean get() = (speedsSupported and 0x02) != 0
        val supportsGen2: Boolean get() = (speedsSupported and 0x04) != 0

        val maxSpeed: UsbSpeedTier
            get() = when {
                supportsGen2 -> UsbSpeedTier.SUPER_SPEED_GEN2
                supportsGen1 -> UsbSpeedTier.SUPER_SPEED_GEN1
                else -> UsbSpeedTier.SUPER_SPEED_GEN1
            }
    }

    data class SuperSpeedPlus(
        val attributes: Int,
        val functionalitySupport: Int,
        val subLinkSpeedAttributes: List<SubLinkSpeed>
    ) : BosCapability() {
        val maxSpeed: UsbSpeedTier
            get() {
                val maxGbps = subLinkSpeedAttributes.maxOfOrNull { it.speedGbps } ?: 0.0
                return when {
                    maxGbps >= 40.0 -> UsbSpeedTier.USB4_GEN3
                    maxGbps >= 20.0 -> UsbSpeedTier.USB4_GEN2
                    maxGbps >= 10.0 -> UsbSpeedTier.SUPER_SPEED_GEN2
                    maxGbps >= 5.0 -> UsbSpeedTier.SUPER_SPEED_GEN1
                    else -> UsbSpeedTier.SUPER_SPEED_GEN1
                }
            }
    }

    data class ContainerId(
        val uuid: String
    ) : BosCapability()

    data class Unknown(
        val type: Int,
        val rawData: ByteArray
    ) : BosCapability() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Unknown) return false
            return type == other.type && rawData.contentEquals(other.rawData)
        }
        override fun hashCode(): Int = 31 * type + rawData.contentHashCode()
    }
}

data class SubLinkSpeed(
    val speedId: Int,
    val exponent: Int,
    val mantissa: Int,
    val laneCount: Int,
    val linkProtocol: Int,
    val direction: SubLinkDirection
) {
    val speedGbps: Double
        get() {
            val base = mantissa.toDouble() * when (exponent) {
                0 -> 1.0          // bits/s
                1 -> 1_000.0      // Kb/s
                2 -> 1_000_000.0  // Mb/s
                3 -> 1_000_000_000.0 // Gb/s
                else -> 1.0
            }
            return (base * laneCount) / 1_000_000_000.0
        }
}

enum class SubLinkDirection { RX, TX }

enum class UsbSpeedTier(val label: String, val gbps: Double) {
    LOW_SPEED("USB 1.0", 0.0015),
    FULL_SPEED("USB 1.1", 0.012),
    HIGH_SPEED("USB 2.0", 0.48),
    SUPER_SPEED_GEN1("USB 3.2 Gen 1", 5.0),
    SUPER_SPEED_GEN2("USB 3.2 Gen 2", 10.0),
    USB4_GEN2("USB4 Gen 2", 20.0),
    USB4_GEN3("USB4 Gen 3", 40.0);
}
