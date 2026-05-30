package com.whatcable.android.domain

import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.data.charging.ChargingMonitor
import com.whatcable.android.data.usb.UsbHostScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CableDiagnosticEngine @Inject constructor(
    private val usbScanner: UsbHostScanner,
    private val speedClassifier: SpeedClassifier,
    private val chargingAnalyser: ChargingAnalyser,
    private val trustScorer: TrustScorer,
    private val chargingMonitor: ChargingMonitor
) {

    suspend fun diagnose(): CableSnapshot = withContext(Dispatchers.IO) {
        val devices = usbScanner.scan()
        val batteryState = chargingMonitor.readCurrentState()
        buildSnapshot(devices, batteryState)
    }

    fun buildSnapshot(
        devices: List<UsbDeviceInfo>,
        batteryState: com.whatcable.android.data.charging.ChargingState? = null
    ): CableSnapshot {
        // Port status, cable e-marker, and alt-mode sysfs were root-only sources and
        // have been removed. Everything here now comes from public APIs: USB host
        // descriptors (BOS/Billboard) and the battery broadcast. portInfo is therefore
        // always null; the classifiers accept that and fall back to descriptor data.
        val speed = speedClassifier.classify(devices, null)
        val charging = chargingAnalyser.assess(null, devices)
        val trust = trustScorer.score(devices, null, speed)
        val altModes = mergeAltModes(devices)

        return CableSnapshot(
            speed = speed,
            charging = charging,
            altModes = altModes,
            portState = null,
            connectedDevices = devices,
            trustScore = trust,
            complianceWarnings = emptyList(),
            batteryState = batteryState
        )
    }

    private fun mergeAltModes(devices: List<UsbDeviceInfo>): List<AltModeInfo> {
        return devices.flatMap { device ->
            device.billboardDescriptor?.altModes?.map { altMode ->
                AltModeInfo(
                    name = altMode.svidLabel,
                    svid = altMode.svid,
                    status = altMode.status,
                    isActive = altMode.status == AltModeStatus.CONFIGURATION_SUCCESSFUL
                )
            } ?: emptyList()
        }
    }
}
