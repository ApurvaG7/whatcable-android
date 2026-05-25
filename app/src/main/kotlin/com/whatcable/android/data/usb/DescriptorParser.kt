package com.whatcable.android.data.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.whatcable.android.core.model.BosDescriptor
import com.whatcable.android.core.model.EndpointDirection
import com.whatcable.android.core.model.EndpointType
import com.whatcable.android.core.model.UsbConfigInfo
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbEndpointInfo
import com.whatcable.android.core.model.UsbInterfaceInfo

object DescriptorParser {

    fun parse(device: UsbDevice, connection: UsbDeviceConnection?, bosDescriptor: BosDescriptor? = null): UsbDeviceInfo {
        val rawDescriptors = connection?.rawDescriptors
        val usbVersion = rawDescriptors?.let { parseUsbVersion(it) }
        val deviceVersion = rawDescriptors?.let { parseDeviceVersion(it) }

        val configurations = (0 until device.configurationCount).map { i ->
            val config = device.getConfiguration(i)
            val interfaces = (0 until config.interfaceCount).map { j ->
                val iface = config.getInterface(j)
                val endpoints = (0 until iface.endpointCount).map { k ->
                    val ep = iface.getEndpoint(k)
                    UsbEndpointInfo(
                        address = ep.address,
                        direction = if (ep.direction == UsbConstants.USB_DIR_IN) {
                            EndpointDirection.IN
                        } else {
                            EndpointDirection.OUT
                        },
                        type = when (ep.type) {
                            UsbConstants.USB_ENDPOINT_XFER_CONTROL -> EndpointType.CONTROL
                            UsbConstants.USB_ENDPOINT_XFER_ISOC -> EndpointType.ISOCHRONOUS
                            UsbConstants.USB_ENDPOINT_XFER_BULK -> EndpointType.BULK
                            UsbConstants.USB_ENDPOINT_XFER_INT -> EndpointType.INTERRUPT
                            else -> EndpointType.CONTROL
                        },
                        maxPacketSize = ep.maxPacketSize,
                        interval = ep.interval
                    )
                }
                UsbInterfaceInfo(
                    id = iface.id,
                    alternateSetting = iface.alternateSetting,
                    name = iface.name,
                    interfaceClass = iface.interfaceClass,
                    interfaceSubclass = iface.interfaceSubclass,
                    interfaceProtocol = iface.interfaceProtocol,
                    endpoints = endpoints
                )
            }
            UsbConfigInfo(
                id = config.id,
                name = config.name,
                maxPower = config.maxPower,
                isSelfPowered = config.isSelfPowered,
                interfaces = interfaces
            )
        }

        return UsbDeviceInfo(
            vendorId = device.vendorId,
            productId = device.productId,
            deviceName = device.deviceName,
            manufacturerName = device.manufacturerName,
            productName = device.productName,
            serialNumber = connection?.serial,
            deviceClass = device.deviceClass,
            deviceSubclass = device.deviceSubclass,
            deviceProtocol = device.deviceProtocol,
            usbVersion = usbVersion,
            deviceVersion = deviceVersion,
            configurations = configurations,
            bosDescriptor = bosDescriptor
        )
    }

    // bcdUSB at offset 2-3 in the device descriptor
    private fun parseUsbVersion(raw: ByteArray): String? {
        if (raw.size < 4) return null
        return parseBcd(raw[3], raw[2])
    }

    // bcdDevice at offset 12-13 in the device descriptor
    private fun parseDeviceVersion(raw: ByteArray): String? {
        if (raw.size < 14) return null
        return parseBcd(raw[13], raw[12])
    }

    private fun parseBcd(high: Byte, low: Byte): String {
        val h = high.toInt() and 0xFF
        val l = low.toInt() and 0xFF
        val major = (h shr 4) and 0x0F
        val minor = h and 0x0F
        val patch = (l shr 4) and 0x0F
        return if (patch == 0) "$major.$minor" else "$major.$minor.$patch"
    }
}
