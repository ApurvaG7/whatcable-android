package com.whatcable.android.core.model

data class BillboardDescriptor(
    val numAltModes: Int,
    val preferredAltModeIndex: Int,
    val vconnPower: VconnPower,
    val altModes: List<BillboardAltMode>,
    val additionalFailureInfo: Int
) {
    val hasFailedNegotiation: Boolean
        get() = altModes.any { it.status == AltModeStatus.CONFIGURATION_NOT_ATTEMPTED ||
                it.status == AltModeStatus.CONFIGURATION_FAILED }
}

data class BillboardAltMode(
    val svid: Int,
    val altModeIndex: Int,
    val status: AltModeStatus
) {
    val svidHex: String get() = "0x%04X".format(svid)

    val svidLabel: String
        get() = when (svid) {
            0xFF01 -> "DisplayPort"
            0x8087 -> "Thunderbolt"
            0x04B4 -> "Cypress"
            0x8086 -> "Intel"
            0x1D17 -> "MediaTek"
            0x2109 -> "VIA Labs"
            0x04E8 -> "Samsung"
            0x0451 -> "Texas Instruments"
            0x1AF8 -> "Fresco Logic"
            else -> "Vendor ($svidHex)"
        }
}

enum class AltModeStatus(val label: String) {
    UNSPECIFIED("Unspecified error"),
    CONFIGURATION_NOT_ATTEMPTED("Not attempted"),
    CONFIGURATION_FAILED("Failed"),
    CONFIGURATION_SUCCESSFUL("Active"),
    UNKNOWN("Unknown");

    companion object {
        fun fromBits(value: Int): AltModeStatus = when (value) {
            0 -> UNSPECIFIED
            1 -> CONFIGURATION_NOT_ATTEMPTED
            2 -> CONFIGURATION_FAILED
            3 -> CONFIGURATION_SUCCESSFUL
            else -> UNKNOWN
        }
    }
}

data class VconnPower(val raw: Int) {
    val wattsRequired: Double
        get() = when (raw and 0x07) {
            0 -> 1.0
            1 -> 1.5
            2 -> 2.0
            3 -> 3.0
            4 -> 4.0
            5 -> 5.0
            6 -> 6.0
            else -> 0.0
        }
}
