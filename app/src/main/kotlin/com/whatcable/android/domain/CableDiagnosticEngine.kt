package com.whatcable.android.domain

import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.DataRole
import com.whatcable.android.core.model.PortMode
import com.whatcable.android.core.model.PortOrientation
import com.whatcable.android.core.model.PowerRole
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbSpeedTier
import com.whatcable.android.data.charging.ChargingMonitor
import com.whatcable.android.data.root.AltModeReader
import com.whatcable.android.data.root.CableIdentityReader
import com.whatcable.android.data.root.RootChecker
import com.whatcable.android.data.root.SuExecutor
import com.whatcable.android.data.root.TypeCPortReader
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
    private val rootChecker: RootChecker,
    private val suExecutor: SuExecutor,
    private val typeCPortReader: TypeCPortReader,
    private val cableIdentityReader: CableIdentityReader,
    private val altModeReader: AltModeReader,
    private val chargingMonitor: ChargingMonitor
) {

    @Volatile
    private var rootAvailable: Boolean? = null

    suspend fun diagnose(): CableSnapshot = withContext(Dispatchers.IO) {
        val devices = usbScanner.scan()
        val rootData = tryReadRootData()
        val batteryState = chargingMonitor.readCurrentState()

        buildSnapshot(devices, rootData, batteryState)
    }

    fun buildSnapshot(
        devices: List<UsbDeviceInfo>,
        rootData: RootData? = null,
        batteryState: com.whatcable.android.data.charging.ChargingState? = null
    ): CableSnapshot {
        var speed = speedClassifier.classify(devices, null)
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

        val charging = chargingAnalyser.assess(null, devices)
        val trust = trustScorer.score(devices, null, speed)

        val altModes = mergeAltModes(devices, rootData)

        val portState = rootData?.portState?.let { state ->
            val orientation = when (state.orientation) {
                "normal" -> PortOrientation.NORMAL
                "reversed" -> PortOrientation.FLIPPED
                else -> PortOrientation.UNKNOWN
            }
            val powerRole = when {
                state.powerRole?.contains("source") == true -> PowerRole.SOURCE
                state.powerRole?.contains("sink") == true -> PowerRole.SINK
                else -> PowerRole.NONE
            }
            val dataRole = when {
                state.dataRole?.contains("host") == true -> DataRole.HOST
                state.dataRole?.contains("device") == true -> DataRole.DEVICE
                else -> DataRole.NONE
            }
            PortState(
                isConnected = true,
                mode = PortMode.DRP,
                powerRole = powerRole,
                dataRole = dataRole,
                orientation = orientation
            )
        }

        return CableSnapshot(
            speed = speed,
            charging = charging,
            altModes = altModes,
            portState = portState,
            connectedDevices = devices,
            trustScore = trust,
            complianceWarnings = emptyList(),
            cableIdentity = rootData?.cableIdentity,
            batteryState = batteryState
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

        val portState = try { typeCPortReader.readPort() } catch (_: Exception) { null }
        val identity = try { cableIdentityReader.readIdentity() } catch (_: Exception) { null }
        val partnerAltModes = try { altModeReader.readPartnerAltModes() } catch (_: Exception) { emptyList() }
        val cableAltModes = try { altModeReader.readCableAltModes() } catch (_: Exception) { emptyList() }

        if (portState == null && identity == null && partnerAltModes.isEmpty() && cableAltModes.isEmpty()) return null

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
