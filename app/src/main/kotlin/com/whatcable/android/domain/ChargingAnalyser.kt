package com.whatcable.android.domain

import com.whatcable.android.core.model.PowerRole
import com.whatcable.android.core.model.UsbDeviceInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargingAnalyser @Inject constructor() {

    /**
     * Assesses charging from connected USB devices alone. Port-level power role and
     * PD negotiation came from root sysfs, which has been removed, so without a
     * connected device there is nothing to assess and PD cannot be confirmed; the
     * best we can do is estimate max power from a device's reported configuration.
     */
    fun assess(devices: List<UsbDeviceInfo>): ChargingAssessment {
        if (devices.isEmpty()) return ChargingAssessment()

        val maxPower = devices.maxOfOrNull { device ->
            device.configurations.maxOfOrNull { config ->
                config.maxPower.toDouble() * 2.0 / 1000.0
            } ?: 0.0
        }?.takeIf { it > 0 }

        return ChargingAssessment(
            powerRole = PowerRole.NONE,
            isCharging = false,
            powerDeliverySupported = false,
            powerTransferLimited = false,
            maxPowerWatts = maxPower,
            confidence = Confidence.LOW
        )
    }
}
