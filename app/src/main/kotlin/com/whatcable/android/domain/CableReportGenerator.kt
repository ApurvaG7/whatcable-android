package com.whatcable.android.domain

import com.whatcable.android.core.model.UsbDeviceInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CableReportGenerator @Inject constructor() {

    fun generate(snapshot: CableSnapshot): String = buildString {
        appendLine("WhatCable Report")
        appendLine("================")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        appendLine("Generated: ${dateFormat.format(Date(snapshot.timestamp))}")
        appendLine()

        appendLine("## Summary")
        appendLine(snapshot.summary)
        appendLine()

        snapshot.speed.tier?.let { tier ->
            appendLine("## Speed")
            appendLine("Tier: ${tier.label} (${tier.gbps} Gbps)")
            appendLine("Confidence: ${snapshot.speed.confidence.name.lowercase()}")
            if (snapshot.speed.sources.isNotEmpty()) {
                appendLine("Sources: ${snapshot.speed.sources.joinToString(", ") { it.name.lowercase().replace('_', ' ') }}")
            }
            appendLine()
        }

        if (snapshot.charging.confidence != Confidence.NONE) {
            appendLine("## Power")
            appendLine("Role: ${snapshot.charging.powerRole.label}")
            appendLine("Charging: ${if (snapshot.charging.isCharging) "Yes" else "No"}")
            appendLine("PD supported: ${if (snapshot.charging.powerDeliverySupported) "Yes" else "No"}")
            snapshot.charging.maxPowerWatts?.let {
                appendLine("Estimated max: ${it.toInt()}W")
            }
            if (snapshot.charging.powerTransferLimited) {
                appendLine("WARNING: Power transfer limited")
            }
            appendLine()
        }

        snapshot.portState?.let { port ->
            appendLine("## USB-C Port")
            appendLine("Connected: ${if (port.isConnected) "Yes" else "No"}")
            appendLine("Mode: ${port.mode.label}")
            appendLine("Power role: ${port.powerRole.label}")
            appendLine("Data role: ${port.dataRole.label}")
            appendLine("Orientation: ${port.orientation.label}")
            appendLine()
        }

        if (snapshot.altModes.isNotEmpty()) {
            appendLine("## Alt Modes")
            snapshot.altModes.forEach { altMode ->
                appendLine("- ${altMode.name}: ${altMode.status.label}")
            }
            appendLine()
        }

        if (snapshot.trustScore.signals.isNotEmpty()) {
            appendLine("## Cable Quality: ${snapshot.trustScore.percentage}% (${snapshot.trustScore.rating.label})")
            snapshot.trustScore.signals.forEach { signal ->
                val mark = if (signal.present) "+" else "-"
                appendLine("  [$mark] ${signal.name}: ${signal.description}")
            }
            appendLine()
        }

        snapshot.cableIdentity?.let { identity ->
            appendLine("## Cable Identity")
            appendLine("Type: ${identity.cableType.label}")
            appendLine("USB4: ${if (identity.supportsUsb4) "Yes" else "No"}")
            identity.maxCurrentMa?.let { appendLine("Max current: ${it / 1000}A") }
            identity.maxSpeedGbps?.let { appendLine("Max speed: ${it} Gbps") }
            identity.idHeader?.let { appendLine("ID Header: 0x${"%08X".format(it)}") }
            identity.product?.let { appendLine("Product VDO: 0x${"%08X".format(it)}") }
            identity.productTypeVdo1?.let { appendLine("VDO1: 0x${"%08X".format(it)}") }
            identity.productTypeVdo2?.let { appendLine("VDO2: 0x${"%08X".format(it)}") }
            appendLine()
        }

        if (snapshot.complianceWarnings.isNotEmpty()) {
            appendLine("## Compliance Warnings")
            snapshot.complianceWarnings.forEach { warning ->
                appendLine("- ${warning.label}")
            }
            appendLine()
        }

        if (snapshot.connectedDevices.isNotEmpty()) {
            appendLine("## Connected Devices")
            snapshot.connectedDevices.forEach { device ->
                appendDeviceReport(device)
            }
        }
    }

    private fun StringBuilder.appendDeviceReport(device: UsbDeviceInfo) {
        appendLine()
        appendLine("### ${device.productName ?: device.manufacturerName ?: "Unknown Device"}")
        appendLine("VID: ${device.vendorIdHex}  PID: ${device.productIdHex}")
        device.manufacturerName?.let { appendLine("Manufacturer: $it") }
        device.usbVersion?.let { appendLine("USB version: $it") }
        appendLine("Class: ${device.deviceClassLabel}")

        device.bosDescriptor?.let { bos ->
            appendLine("BOS: ${bos.numDeviceCaps} capabilities")
            bos.maxSpeed?.let { appendLine("Max speed: ${it.label} (${it.gbps} Gbps)") }
        }

        device.billboardDescriptor?.let { billboard ->
            appendLine("Billboard: ${billboard.numAltModes} alt modes")
            billboard.altModes.forEach { altMode ->
                appendLine("  ${altMode.svidLabel}: ${altMode.status.label}")
            }
        }

        val ifaceCount = device.configurations.sumOf { it.interfaces.size }
        appendLine("Interfaces: $ifaceCount across ${device.configurations.size} config(s)")
    }
}
