package com.whatcable.android.core.model

data class UsbDeviceInfo(
    val vendorId: Int,
    val productId: Int,
    val deviceName: String,
    val manufacturerName: String?,
    val productName: String?,
    val serialNumber: String?,
    val deviceClass: Int,
    val deviceSubclass: Int,
    val deviceProtocol: Int,
    val usbVersion: String?,
    val deviceVersion: String?,
    val configurations: List<UsbConfigInfo>
) {
    val vendorIdHex: String get() = "0x%04X".format(vendorId)
    val productIdHex: String get() = "0x%04X".format(productId)
    val deviceClassLabel: String get() = usbClassLabel(deviceClass)
}

data class UsbConfigInfo(
    val id: Int,
    val name: String?,
    val maxPower: Int,
    val isSelfPowered: Boolean,
    val interfaces: List<UsbInterfaceInfo>
)

data class UsbInterfaceInfo(
    val id: Int,
    val alternateSetting: Int,
    val name: String?,
    val interfaceClass: Int,
    val interfaceSubclass: Int,
    val interfaceProtocol: Int,
    val endpoints: List<UsbEndpointInfo>
) {
    val classLabel: String get() = usbClassLabel(interfaceClass)
    val isBillboard: Boolean get() = interfaceClass == 0x11
}

data class UsbEndpointInfo(
    val address: Int,
    val direction: EndpointDirection,
    val type: EndpointType,
    val maxPacketSize: Int,
    val interval: Int
)

enum class EndpointDirection { IN, OUT }

enum class EndpointType {
    CONTROL, ISOCHRONOUS, BULK, INTERRUPT
}

private fun usbClassLabel(classCode: Int): String = when (classCode) {
    0x00 -> "Per-Interface"
    0x01 -> "Audio"
    0x02 -> "CDC"
    0x03 -> "HID"
    0x05 -> "Physical"
    0x06 -> "Image"
    0x07 -> "Printer"
    0x08 -> "Mass Storage"
    0x09 -> "Hub"
    0x0A -> "CDC-Data"
    0x0B -> "Smart Card"
    0x0D -> "Content Security"
    0x0E -> "Video"
    0x0F -> "Personal Healthcare"
    0x10 -> "Audio/Video"
    0x11 -> "Billboard"
    0xDC -> "Diagnostic"
    0xE0 -> "Wireless"
    0xEF -> "Miscellaneous"
    0xFE -> "Application Specific"
    0xFF -> "Vendor Specific"
    else -> "Unknown (0x%02X)".format(classCode)
}
