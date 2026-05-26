package com.whatcable.android.domain

import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.CapabilityTier
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbPortInfo
import com.whatcable.android.core.model.UsbSpeedTier
import com.whatcable.android.data.root.AltModeReader
import com.whatcable.android.data.root.CableIdentityReader
import com.whatcable.android.data.root.RootChecker
import com.whatcable.android.data.root.SuExecutor
import com.whatcable.android.data.root.TypeCPortReader
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
    private val trustScorer: TrustScorer,
    private val rootChecker: RootChecker,
    private val suExecutor: SuExecutor,
    private val typeCPortReader: TypeCPortReader,
    private val cableIdentityReader: CableIdentityReader,
    private val altModeReader: AltModeReader
) {

    private var rootAvailable: Boolean? = null

    suspend fun diagnose(): CableSnapshot = withContext(Dispatchers.IO) {
        val devices = usbScanner.scan()
        val ports = if (shizukuPortReader.hasPermission()) {
            shizukuPortReader.readPorts()
        } else emptyList()

        val rootData = tryReadRootData()

        buildSnapshot(devices, ports, rootData)
    }

    fun buildSnapshot(
        devices: List<UsbDeviceInfo>,
        ports: List<UsbPortInfo>,
        rootData: RootData? = null
    ): CableSnapshot {
        val hasRoot = rootData != null
        val tier = when {
            hasRoot -> CapabilityTier.FULL
            shizukuPortReader.hasPermission() -> CapabilityTier.ENHANCED
            else -> CapabilityTier.BASIC
        }

        val primaryPort = ports.firstOrNull { it.isConnected } ?: ports.firstOrNull()

        var speed = speedClassifier.classify(devices, primaryPort)
        if (rootData?.cableIdentity?.maxSpeedGbps != null) {
            val rootSpeed = speedFromGbps(rootData.cableIdentity.maxSpeedGbps)
            if (rootSpeed != null && (speed.tier == null || rootSpeed.gbps > speed.tier.gbps)) {
                speed = speed.copy(
                    tier = rootSpeed,
                    confidence = Confidence.HIGH,
                    sources = speed.sources + SpeedSource.SYSFS
                )
            }
        }

        val charging = chargingAnalyser.assess(primaryPort, devices)
        val trust = trustScorer.score(devices, primaryPort, speed)

        val altModes = mergeAltModes(devices, rootData)

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
            complianceWarnings = complianceWarnings,
            cableIdentity = rootData?.cableIdentity
        )
    }

    private suspend fun tryReadRootData(): RootData? {
        if (rootAvailable == false) return null
        if (!rootChecker.isRooted) {
            rootAvailable = false
            return null
        }

        if (rootAvailable == null) {
            rootAvailable = suExecutor.isAvailable()
            if (rootAvailable != true) return null
        }

        val portState = typeCPortReader.readPort()
        val identity = cableIdentityReader.readIdentity()
        val partnerAltModes = altModeReader.readPartnerAltModes()
        val cableAltModes = altModeReader.readCableAltModes()

        if (portState == null && identity == null && partnerAltModes.isEmpty()) return null

        return RootData(
            portState = portState,
            cableIdentity = identity,
            partnerAltModes = partnerAltModes,
            cableAltModes = cableAltModes
        )
    }

    private fun mergeAltModes(
        devices: List<UsbDeviceInfo>,
        rootData: RootData?
    ): List<AltModeInfo> {
        val billboardModes = devices.flatMap { device ->
            device.billboardDescriptor?.altModes?.map { altMode ->
                AltModeInfo(
                    name = altMode.svidLabel,
                    svid = altMode.svid,
                    status = altMode.status,
                    isActive = altMode.status == AltModeStatus.CONFIGURATION_SUCCESSFUL
                )
            } ?: emptyList()
        }

        if (rootData == null) return billboardModes

        val sysfsModes = rootData.partnerAltModes.map { mode ->
            AltModeInfo(
                name = mode.svidLabel,
                svid = mode.svid,
                status = if (mode.active) AltModeStatus.CONFIGURATION_SUCCESSFUL else AltModeStatus.CONFIGURATION_NOT_ATTEMPTED,
                isActive = mode.active
            )
        }

        if (sysfsModes.isNotEmpty()) return sysfsModes
        return billboardModes
    }

    private fun speedFromGbps(gbps: Double): UsbSpeedTier? = when {
        gbps >= 40.0 -> UsbSpeedTier.USB4_GEN3
        gbps >= 20.0 -> UsbSpeedTier.USB4_GEN2
        gbps >= 10.0 -> UsbSpeedTier.SUPER_SPEED_GEN2
        gbps >= 5.0 -> UsbSpeedTier.SUPER_SPEED_GEN1
        gbps >= 0.48 -> UsbSpeedTier.HIGH_SPEED
        else -> null
    }
}

data class RootData(
    val portState: TypeCPortReader.TypeCPortState?,
    val cableIdentity: CableIdentityReader.CableIdentity?,
    val partnerAltModes: List<AltModeReader.SysfsAltMode>,
    val cableAltModes: List<AltModeReader.SysfsAltMode>
)
