package com.whatcable.android.domain

import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.BosCapability
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbPortInfo
import com.whatcable.android.core.model.UsbSpeedTier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustScorer @Inject constructor() {

    fun score(
        devices: List<UsbDeviceInfo>,
        portInfo: UsbPortInfo?,
        speed: SpeedClassification
    ): TrustScore {
        val signals = mutableListOf<TrustSignal>()

        signals.add(checkBosPresent(devices))
        signals.add(checkManufacturerIdentified(devices))
        signals.add(checkSuperSpeedCapable(speed))
        signals.add(checkNoComplianceWarnings(portInfo))
        signals.add(checkNoPowerLimitation(portInfo))
        signals.add(checkAltModeNegotiation(devices))
        signals.add(checkLpmSupport(devices))

        val earned = signals.filter { it.present }.sumOf { it.points }
        val maxPossible = signals.sumOf { it.points }

        return TrustScore(
            score = earned,
            maxPossible = maxPossible,
            signals = signals
        )
    }

    private fun checkBosPresent(devices: List<UsbDeviceInfo>): TrustSignal {
        val present = devices.any { it.bosDescriptor != null }
        return TrustSignal(
            name = "BOS Descriptor",
            present = present,
            points = 20,
            description = if (present) "Device reports USB capabilities" else "No BOS descriptor found"
        )
    }

    private fun checkManufacturerIdentified(devices: List<UsbDeviceInfo>): TrustSignal {
        val present = devices.any { it.manufacturerName != null }
        return TrustSignal(
            name = "Manufacturer ID",
            present = present,
            points = 15,
            description = if (present) "Manufacturer identified" else "No manufacturer string"
        )
    }

    private fun checkSuperSpeedCapable(speed: SpeedClassification): TrustSignal {
        val present = speed.tier != null && speed.tier.gbps >= UsbSpeedTier.SUPER_SPEED_GEN1.gbps
        return TrustSignal(
            name = "SuperSpeed",
            present = present,
            points = 20,
            description = if (present) "SuperSpeed or higher" else "USB 2.0 or below"
        )
    }

    private fun checkNoComplianceWarnings(portInfo: UsbPortInfo?): TrustSignal {
        val present = portInfo != null && portInfo.complianceWarnings.isEmpty()
        return TrustSignal(
            name = "Compliance",
            present = present,
            points = 20,
            description = when {
                portInfo == null -> "Port info unavailable"
                present -> "No compliance warnings"
                else -> "${portInfo.complianceWarnings.size} compliance issue(s)"
            }
        )
    }

    private fun checkNoPowerLimitation(portInfo: UsbPortInfo?): TrustSignal {
        val present = portInfo != null && portInfo.powerTransferLimited != true
        return TrustSignal(
            name = "Power delivery",
            present = present,
            points = 10,
            description = when {
                portInfo == null -> "Port info unavailable"
                present -> "Power transfer normal"
                else -> "Power transfer limited"
            }
        )
    }

    private fun checkAltModeNegotiation(devices: List<UsbDeviceInfo>): TrustSignal {
        val billboard = devices.firstOrNull { it.billboardDescriptor != null }?.billboardDescriptor
        val present = billboard != null &&
            billboard.altModes.any { it.status == AltModeStatus.CONFIGURATION_SUCCESSFUL }
        val hasBillboard = billboard != null
        return TrustSignal(
            name = "Alt Mode",
            present = present,
            points = 10,
            description = when {
                !hasBillboard -> "No alt-mode info available"
                present -> "Alt mode negotiated successfully"
                else -> "Alt mode negotiation failed"
            }
        )
    }

    private fun checkLpmSupport(devices: List<UsbDeviceInfo>): TrustSignal {
        val present = devices.any { device ->
            device.bosDescriptor?.capabilities?.any { cap ->
                cap is BosCapability.Usb2Extension && cap.supportsLpm
            } == true
        }
        return TrustSignal(
            name = "LPM",
            present = present,
            points = 5,
            description = if (present) "Link Power Management supported" else "No LPM support"
        )
    }
}
