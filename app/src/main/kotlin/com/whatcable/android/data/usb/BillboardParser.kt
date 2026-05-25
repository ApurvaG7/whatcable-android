package com.whatcable.android.data.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.BillboardAltMode
import com.whatcable.android.core.model.BillboardDescriptor
import com.whatcable.android.core.model.BosCapability
import com.whatcable.android.core.model.BosDescriptor
import com.whatcable.android.core.model.VconnPower
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillboardParser @Inject constructor() {

    companion object {
        private const val BILLBOARD_CLASS = 0x11
        private const val BILLBOARD_CAP_TYPE = 0x0D
        private const val BILLBOARD_EX_CAP_TYPE = 0x0F

        // Billboard Capability descriptor minimum length
        private const val MIN_BILLBOARD_CAP_LENGTH = 44
    }

    fun isBillboardDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == BILLBOARD_CLASS) return true
        for (i in 0 until device.configurationCount) {
            val config = device.getConfiguration(i)
            for (j in 0 until config.interfaceCount) {
                if (config.getInterface(j).interfaceClass == BILLBOARD_CLASS) return true
            }
        }
        return false
    }

    fun parse(bosDescriptor: BosDescriptor): BillboardDescriptor? {
        val capData = bosDescriptor.capabilities
            .filterIsInstance<BosCapability.Unknown>()
            .firstOrNull { it.type == BILLBOARD_CAP_TYPE }
            ?: return null

        return parseBillboardCapability(capData.rawData)
    }

    private fun parseBillboardCapability(data: ByteArray): BillboardDescriptor? {
        if (data.size < MIN_BILLBOARD_CAP_LENGTH) return null

        // Billboard Capability Descriptor layout:
        // Offset 3: iAdditionalInfoURL (string index)
        // Offset 4-7: bNumberOfAlternateModes, bPreferredAlternateMode, VCONN Power
        val numAltModes = data[4].toInt() and 0xFF
        val preferredAltMode = data[5].toInt() and 0xFF
        val vconnPower = VconnPower((data[6].toInt() and 0xFF) or
                ((data[7].toInt() and 0xFF) shl 8))

        // Offset 8-11: bmConfigured (32 bits of additional failure info)
        val additionalFailureInfo = readInt32(data, 8)

        // Offset 12-43: bcdVersion, bAdditionalFailureInfo, bReserved, ...
        // Offset 44+: Alternate Mode descriptors (4 bytes each: wSVID + bAlternateMode + iAlternateModeSetting)
        // Actually the standard layout has alternate modes starting at offset 12
        // Each alt mode entry: wSVID (2 bytes) + bAlternateMode (1 byte) + iAlternateModeSetting (1 byte)

        val altModes = mutableListOf<BillboardAltMode>()

        // Alt mode status bits are packed 2 bits per mode in the bmConfigured field
        // starting from offset 8
        val statusBitfield = additionalFailureInfo

        val altModeOffset = 12
        for (i in 0 until numAltModes) {
            val base = altModeOffset + (i * 4)
            if (base + 4 > data.size) break

            val svid = (data[base].toInt() and 0xFF) or
                    ((data[base + 1].toInt() and 0xFF) shl 8)
            val altModeIndex = data[base + 2].toInt() and 0xFF

            val statusBits = (statusBitfield shr (i * 2)) and 0x03
            val status = AltModeStatus.fromBits(statusBits)

            altModes.add(
                BillboardAltMode(
                    svid = svid,
                    altModeIndex = altModeIndex,
                    status = status
                )
            )
        }

        return BillboardDescriptor(
            numAltModes = numAltModes,
            preferredAltModeIndex = preferredAltMode,
            vconnPower = vconnPower,
            altModes = altModes,
            additionalFailureInfo = additionalFailureInfo
        )
    }

    private fun readInt32(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }
}
