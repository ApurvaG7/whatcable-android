package com.whatcable.android.domain

import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.BillboardAltMode
import com.whatcable.android.core.model.CapabilityTier
import com.whatcable.android.core.model.ComplianceWarning
import com.whatcable.android.core.model.DataRole
import com.whatcable.android.core.model.PortMode
import com.whatcable.android.core.model.PortOrientation
import com.whatcable.android.core.model.PowerRole
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbPortInfo
import com.whatcable.android.core.model.UsbSpeedTier

data class CableSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val capabilityTier: CapabilityTier = CapabilityTier.BASIC,
    val speed: SpeedClassification = SpeedClassification(),
    val charging: ChargingAssessment = ChargingAssessment(),
    val altModes: List<AltModeInfo> = emptyList(),
    val portState: PortState? = null,
    val connectedDevices: List<UsbDeviceInfo> = emptyList(),
    val trustScore: TrustScore = TrustScore(),
    val complianceWarnings: List<ComplianceWarning> = emptyList()
) {
    val isConnected: Boolean
        get() = portState?.isConnected == true || connectedDevices.isNotEmpty()

    val summary: String
        get() = when {
            !isConnected -> "No cable connected"
            speed.tier != null -> "${speed.tier.label} cable"
            else -> "Cable connected"
        }
}

data class SpeedClassification(
    val tier: UsbSpeedTier? = null,
    val advertisedSpeed: UsbSpeedTier? = null,
    val negotiatedSpeed: UsbSpeedTier? = null,
    val confidence: Confidence = Confidence.NONE,
    val sources: List<SpeedSource> = emptyList()
)

enum class SpeedSource {
    BOS_DESCRIPTOR,
    BILLBOARD_ALT_MODE,
    PORT_STATUS,
    SYSFS,
    DEVICE_BCD_USB
}

data class ChargingAssessment(
    val powerRole: PowerRole = PowerRole.NONE,
    val isCharging: Boolean = false,
    val powerDeliverySupported: Boolean = false,
    val powerTransferLimited: Boolean = false,
    val maxPowerWatts: Double? = null,
    val confidence: Confidence = Confidence.NONE
)

data class AltModeInfo(
    val name: String,
    val svid: Int,
    val status: AltModeStatus,
    val isActive: Boolean
)

data class PortState(
    val isConnected: Boolean,
    val mode: PortMode,
    val powerRole: PowerRole,
    val dataRole: DataRole,
    val orientation: PortOrientation
)

data class TrustScore(
    val score: Int = 0,
    val maxPossible: Int = 100,
    val signals: List<TrustSignal> = emptyList()
) {
    val percentage: Int get() = if (maxPossible > 0) (score * 100) / maxPossible else 0

    val rating: TrustRating
        get() = when (percentage) {
            in 80..100 -> TrustRating.HIGH
            in 50..79 -> TrustRating.MEDIUM
            in 1..49 -> TrustRating.LOW
            else -> TrustRating.UNKNOWN
        }
}

data class TrustSignal(
    val name: String,
    val present: Boolean,
    val points: Int,
    val description: String
)

enum class TrustRating(val label: String) {
    HIGH("High quality"),
    MEDIUM("Acceptable"),
    LOW("Low quality"),
    UNKNOWN("Insufficient data")
}

enum class Confidence {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}
