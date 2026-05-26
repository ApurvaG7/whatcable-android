package com.whatcable.android.domain

import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.CapabilityTier
import com.whatcable.android.core.model.ComplianceWarning
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbPortInfo
import com.whatcable.android.data.shizuku.ShizukuUsbPortReader
import com.whatcable.android.data.usb.UsbHostScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CableDiagnosticEngine @Inject constructor(
    private val usbScanner: UsbHostScanner,
    private val shizukuPortReader: ShizukuUsbPortReader,
    private val speedClassifier: SpeedClassifier,
    private val chargingAnalyser: ChargingAnalyser,
    private val trustScorer: TrustScorer
) {

    suspend fun diagnose(): CableSnapshot = withContext(Dispatchers.IO) {
        val devices = usbScanner.scan()
        val ports = if (shizukuPortReader.hasPermission()) {
            shizukuPortReader.readPorts()
        } else emptyList()

        buildSnapshot(devices, ports)
    }

    fun buildSnapshot(
        devices: List<UsbDeviceInfo>,
        ports: List<UsbPortInfo>
    ): CableSnapshot {
        val primaryPort = ports.firstOrNull { it.isConnected } ?: ports.firstOrNull()
        val tier = when {
            shizukuPortReader.hasPermission() -> CapabilityTier.ENHANCED
            else -> CapabilityTier.BASIC
        }

        val speed = speedClassifier.classify(devices, primaryPort)
        val charging = chargingAnalyser.assess(primaryPort, devices)
        val trust = trustScorer.score(devices, primaryPort, speed)
        val altModes = extractAltModes(devices)
        val portState = primaryPort?.let {
            PortState(
                isConnected = it.isConnected,
                mode = it.mode,
                powerRole = it.powerRole,
                dataRole = it.dataRole,
                orientation = it.orientation
            )
        }

        val complianceWarnings = ports.flatMap { it.complianceWarnings }.distinct()

        return CableSnapshot(
            capabilityTier = tier,
            speed = speed,
            charging = charging,
            altModes = altModes,
            portState = portState,
            connectedDevices = devices,
            trustScore = trust,
            complianceWarnings = complianceWarnings
        )
    }

    private fun extractAltModes(devices: List<UsbDeviceInfo>): List<AltModeInfo> {
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
