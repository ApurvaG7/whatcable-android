package com.whatcable.android.domain

import com.whatcable.android.core.model.BosCapability
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbPortInfo
import com.whatcable.android.core.model.UsbSpeedTier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeedClassifier @Inject constructor() {

    fun classify(
        devices: List<UsbDeviceInfo>,
        portInfo: UsbPortInfo?
    ): SpeedClassification {
        val sources = mutableListOf<SpeedSource>()
        val candidates = mutableListOf<UsbSpeedTier>()

        val bosSpeed = classifyFromBos(devices)
        if (bosSpeed != null) {
            candidates.add(bosSpeed)
            sources.add(SpeedSource.BOS_DESCRIPTOR)
        }

        val bcdSpeed = classifyFromBcdUsb(devices)
        if (bcdSpeed != null && bosSpeed == null) {
            candidates.add(bcdSpeed)
            sources.add(SpeedSource.DEVICE_BCD_USB)
        }

        val billboardSpeed = classifyFromBillboard(devices)
        if (billboardSpeed != null) {
            candidates.add(billboardSpeed)
            sources.add(SpeedSource.BILLBOARD_ALT_MODE)
        }

        if (candidates.isEmpty()) {
            return SpeedClassification(
                confidence = Confidence.NONE,
                sources = sources
            )
        }

        val bestSpeed = candidates.maxByOrNull { it.gbps } ?: return SpeedClassification()
        val confidence = when {
            sources.contains(SpeedSource.BOS_DESCRIPTOR) -> Confidence.HIGH
            sources.size >= 2 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        return SpeedClassification(
            tier = bestSpeed,
            advertisedSpeed = bosSpeed ?: bcdSpeed,
            negotiatedSpeed = null,
            confidence = confidence,
            sources = sources
        )
    }

    private fun classifyFromBos(devices: List<UsbDeviceInfo>): UsbSpeedTier? {
        return devices.mapNotNull { it.bosDescriptor?.maxSpeed }.maxByOrNull { it.gbps }
    }

    private fun classifyFromBcdUsb(devices: List<UsbDeviceInfo>): UsbSpeedTier? {
        return devices.mapNotNull { device ->
            device.usbVersion?.let { version ->
                when {
                    version.startsWith("3.2") || version.startsWith("3.20") -> UsbSpeedTier.SUPER_SPEED_GEN2
                    version.startsWith("3.1") || version.startsWith("3.10") -> UsbSpeedTier.SUPER_SPEED_GEN1
                    version.startsWith("3.0") || version.startsWith("3.00") -> UsbSpeedTier.SUPER_SPEED_GEN1
                    version.startsWith("2.") -> UsbSpeedTier.HIGH_SPEED
                    version.startsWith("1.1") -> UsbSpeedTier.FULL_SPEED
                    version.startsWith("1.0") -> UsbSpeedTier.LOW_SPEED
                    else -> null
                }
            }
        }.maxByOrNull { it.gbps }
    }

    private fun classifyFromBillboard(devices: List<UsbDeviceInfo>): UsbSpeedTier? {
        for (device in devices) {
            val billboard = device.billboardDescriptor ?: continue
            val hasThunderbolt = billboard.altModes.any {
                it.svid == 0x8087 && it.status == com.whatcable.android.core.model.AltModeStatus.CONFIGURATION_SUCCESSFUL
            }
            // Thunderbolt implies at least USB4 Gen2; Gen3 requires USB4 discovery data (root tier)
            if (hasThunderbolt) return UsbSpeedTier.USB4_GEN2

            val hasDP = billboard.altModes.any {
                it.svid == 0xFF01 && it.status == com.whatcable.android.core.model.AltModeStatus.CONFIGURATION_SUCCESSFUL
            }
            if (hasDP) return UsbSpeedTier.SUPER_SPEED_GEN2
        }
        return null
    }
}
