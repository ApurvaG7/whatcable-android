package com.whatcable.android.data.usb

import android.hardware.usb.UsbDeviceConnection
import com.whatcable.android.core.model.BosCapability
import com.whatcable.android.core.model.BosDescriptor
import com.whatcable.android.core.model.SubLinkDirection
import com.whatcable.android.core.model.SubLinkSpeed
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BosReader @Inject constructor() {

    companion object {
        private const val GET_DESCRIPTOR = 0x06
        private const val BOS_DESCRIPTOR_TYPE = 0x0F
        private const val REQUEST_TYPE_DEVICE_IN = 0x80
        private const val TIMEOUT_MS = 2000

        // BOS capability types
        private const val CAP_USB2_EXTENSION = 0x02
        private const val CAP_SUPERSPEED = 0x03
        private const val CAP_SUPERSPEED_PLUS = 0x0A
        private const val CAP_CONTAINER_ID = 0x04
    }

    fun read(connection: UsbDeviceConnection): BosDescriptor? {
        // First read: get BOS header (5 bytes) to learn total length
        val header = ByteArray(5)
        val headerLen = connection.controlTransfer(
            REQUEST_TYPE_DEVICE_IN,
            GET_DESCRIPTOR,
            (BOS_DESCRIPTOR_TYPE shl 8),
            0,
            header,
            header.size,
            TIMEOUT_MS
        )
        if (headerLen < 5) return null

        val totalLength = (header[2].toInt() and 0xFF) or
                ((header[3].toInt() and 0xFF) shl 8)
        val numDeviceCaps = header[4].toInt() and 0xFF

        if (totalLength <= 5) return BosDescriptor(totalLength, numDeviceCaps, emptyList())

        // Second read: get the full BOS descriptor
        val full = ByteArray(totalLength)
        val fullLen = connection.controlTransfer(
            REQUEST_TYPE_DEVICE_IN,
            GET_DESCRIPTOR,
            (BOS_DESCRIPTOR_TYPE shl 8),
            0,
            full,
            full.size,
            TIMEOUT_MS
        )
        if (fullLen < totalLength) return null

        val capabilities = parseCapabilities(full, totalLength)
        return BosDescriptor(totalLength, numDeviceCaps, capabilities)
    }

    private fun parseCapabilities(data: ByteArray, totalLength: Int): List<BosCapability> {
        val caps = mutableListOf<BosCapability>()
        var offset = 5 // Skip BOS header

        while (offset < totalLength) {
            if (offset + 2 > totalLength) break
            val capLength = data[offset].toInt() and 0xFF
            if (capLength < 3 || offset + capLength > totalLength) break

            val capType = data[offset + 2].toInt() and 0xFF
            val capData = data.copyOfRange(offset, offset + capLength)

            val cap = when (capType) {
                CAP_USB2_EXTENSION -> parseUsb2Extension(capData)
                CAP_SUPERSPEED -> parseSuperSpeed(capData)
                CAP_SUPERSPEED_PLUS -> parseSuperSpeedPlus(capData)
                CAP_CONTAINER_ID -> parseContainerId(capData)
                else -> BosCapability.Unknown(capType, capData)
            }
            caps.add(cap)
            offset += capLength
        }
        return caps
    }

    private fun parseUsb2Extension(data: ByteArray): BosCapability.Usb2Extension {
        val attributes = if (data.size >= 7) {
            (data[3].toInt() and 0xFF) or
                    ((data[4].toInt() and 0xFF) shl 8) or
                    ((data[5].toInt() and 0xFF) shl 16) or
                    ((data[6].toInt() and 0xFF) shl 24)
        } else 0
        return BosCapability.Usb2Extension(attributes)
    }

    private fun parseSuperSpeed(data: ByteArray): BosCapability.SuperSpeed {
        if (data.size < 10) {
            return BosCapability.SuperSpeed(0, 0, 0, 0, 0)
        }
        return BosCapability.SuperSpeed(
            attributes = data[3].toInt() and 0xFF,
            speedsSupported = (data[4].toInt() and 0xFF) or
                    ((data[5].toInt() and 0xFF) shl 8),
            functionalitySupport = data[6].toInt() and 0xFF,
            u1DevExitLat = data[7].toInt() and 0xFF,
            u2DevExitLat = (data[8].toInt() and 0xFF) or
                    ((data[9].toInt() and 0xFF) shl 8)
        )
    }

    private fun parseSuperSpeedPlus(data: ByteArray): BosCapability.SuperSpeedPlus {
        if (data.size < 12) {
            return BosCapability.SuperSpeedPlus(0, 0, emptyList())
        }

        val attributes = (data[3].toInt() and 0xFF) or
                ((data[4].toInt() and 0xFF) shl 8) or
                ((data[5].toInt() and 0xFF) shl 16) or
                ((data[6].toInt() and 0xFF) shl 24)
        val functionalitySupport = (data[7].toInt() and 0xFF) or
                ((data[8].toInt() and 0xFF) shl 8)

        val numSubLinkSpeedAttrs = (attributes and 0x1F) + 1
        val subLinks = mutableListOf<SubLinkSpeed>()

        for (i in 0 until numSubLinkSpeedAttrs) {
            val base = 12 + (i * 4)
            if (base + 4 > data.size) break

            val dword = (data[base].toInt() and 0xFF) or
                    ((data[base + 1].toInt() and 0xFF) shl 8) or
                    ((data[base + 2].toInt() and 0xFF) shl 16) or
                    ((data[base + 3].toInt() and 0xFF) shl 24)

            val speedId = dword and 0x0F
            val laneExponent = (dword shr 4) and 0x03
            val linkProtocol = (dword shr 6) and 0x03
            val direction = if ((dword shr 8) and 0x01 == 0) SubLinkDirection.RX else SubLinkDirection.TX
            val exponent = (dword shr 16) and 0x03
            val mantissa = (dword shr 18) and 0x3FFF

            subLinks.add(
                SubLinkSpeed(
                    speedId = speedId,
                    exponent = exponent,
                    mantissa = mantissa,
                    laneCount = 1 shl laneExponent,
                    linkProtocol = linkProtocol,
                    direction = direction
                )
            )
        }

        return BosCapability.SuperSpeedPlus(
            attributes = attributes,
            functionalitySupport = functionalitySupport,
            subLinkSpeedAttributes = subLinks
        )
    }

    private fun parseContainerId(data: ByteArray): BosCapability.ContainerId {
        if (data.size < 20) return BosCapability.ContainerId("")
        val uuid = data.copyOfRange(4, 20)
        val formatted = "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".format(
            uuid[3], uuid[2], uuid[1], uuid[0],
            uuid[5], uuid[4],
            uuid[7], uuid[6],
            uuid[8], uuid[9],
            uuid[10], uuid[11], uuid[12], uuid[13], uuid[14], uuid[15]
        )
        return BosCapability.ContainerId(formatted)
    }
}
