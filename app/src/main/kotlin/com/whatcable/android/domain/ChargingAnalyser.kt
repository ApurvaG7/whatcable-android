package com.whatcable.android.domain

import com.whatcable.android.core.model.PortMode
import com.whatcable.android.core.model.PowerRole
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbPortInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargingAnalyser @Inject constructor() {

    fun assess(
        portInfo: UsbPortInfo?,
        devices: List<UsbDeviceInfo>
    ): ChargingAssessment {
        if (portInfo == null && devices.isEmpty()) {
            return ChargingAssessment()
        }

        val powerRole = portInfo?.powerRole ?: PowerRole.NONE
        val isCharging = powerRole == PowerRole.SINK
        val powerTransferLimited = portInfo?.powerTransferLimited ?: false

        val pdSupported = isPdSupported(portInfo, devices)
        val maxPower = estimateMaxPower(portInfo, devices, pdSupported)

        val confidence = when {
            portInfo != null && pdSupported -> Confidence.HIGH
            portInfo != null -> Confidence.MEDIUM
            devices.isNotEmpty() -> Confidence.LOW
            else -> Confidence.NONE
        }

        return ChargingAssessment(
            powerRole = powerRole,
            isCharging = isCharging,
            powerDeliverySupported = pdSupported,
            powerTransferLimited = powerTransferLimited,
            maxPowerWatts = maxPower,
            confidence = confidence
        )
    }

    private fun isPdSupported(portInfo: UsbPortInfo?, devices: List<UsbDeviceInfo>): Boolean {
        // Authoritative: port reports active power role negotiation
        if (portInfo?.mode == PortMode.DFP || portInfo?.mode == PortMode.UFP || portInfo?.mode == PortMode.DRP) {
            if (portInfo.powerRole != PowerRole.NONE) return true
        }

        // Without system-level port data, we can't confirm PD from device descriptors alone
        return false
    }

    private fun estimateMaxPower(
        portInfo: UsbPortInfo?,
        devices: List<UsbDeviceInfo>,
        pdSupported: Boolean
    ): Double? {
        if (portInfo?.powerTransferLimited == true) return 15.0

        // Without e-marker detection (requires root/sysfs VDO read), assume 60W max for PD
        if (pdSupported) return 60.0

        val maxConfigPower = devices.maxOfOrNull { device ->
            device.configurations.maxOfOrNull { config ->
                config.maxPower.toDouble() * 2.0 / 1000.0
            } ?: 0.0
        }
        if (maxConfigPower != null && maxConfigPower > 0) return maxConfigPower

        return null
    }
}
