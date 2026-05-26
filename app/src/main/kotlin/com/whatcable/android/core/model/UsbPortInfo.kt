package com.whatcable.android.core.model

data class UsbPortInfo(
    val id: String,
    val isConnected: Boolean,
    val mode: PortMode,
    val powerRole: PowerRole,
    val dataRole: DataRole,
    val orientation: PortOrientation,
    val contaminantDetected: Boolean?,
    val usbDataEnabled: Boolean?,
    val powerTransferLimited: Boolean?,
    val complianceWarnings: List<ComplianceWarning>
)

enum class PortMode(val label: String) {
    NONE("None"),
    DFP("Host (DFP)"),
    UFP("Device (UFP)"),
    DRP("Dual Role");

    companion object {
        fun fromInt(value: Int): PortMode = when (value) {
            1 -> DFP
            2 -> UFP
            3 -> DRP
            else -> NONE
        }
    }
}

enum class PowerRole(val label: String) {
    NONE("None"),
    SOURCE("Source"),
    SINK("Sink");

    companion object {
        fun fromInt(value: Int): PowerRole = when (value) {
            1 -> SOURCE
            2 -> SINK
            else -> NONE
        }
    }
}

enum class DataRole(val label: String) {
    NONE("None"),
    HOST("Host"),
    DEVICE("Device");

    companion object {
        fun fromInt(value: Int): DataRole = when (value) {
            1 -> HOST
            2 -> DEVICE
            else -> NONE
        }
    }
}

enum class PortOrientation(val label: String) {
    UNKNOWN("Unknown"),
    NORMAL("Normal (CC1)"),
    FLIPPED("Flipped (CC2)");

    companion object {
        fun fromPlugState(value: Int?): PortOrientation = when (value) {
            2 -> NORMAL
            3 -> FLIPPED
            else -> UNKNOWN
        }
    }
}

enum class ComplianceWarning(val label: String) {
    OTHER("Other issue"),
    DEBUG_ACCESSORY("Debug accessory connected"),
    BC_12("BC 1.2 issue"),
    PD_POLICY("PD policy issue"),
    INPUT_POWER_LIMITED("Input power limited"),
    MISSING_RP("Missing Rp");

    companion object {
        fun fromInt(value: Int): ComplianceWarning = when (value) {
            1 -> DEBUG_ACCESSORY
            2 -> BC_12
            3 -> PD_POLICY
            4 -> INPUT_POWER_LIMITED
            5 -> MISSING_RP
            else -> OTHER
        }
    }
}
