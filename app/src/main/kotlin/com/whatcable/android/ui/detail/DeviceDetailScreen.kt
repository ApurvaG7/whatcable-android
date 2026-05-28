package com.whatcable.android.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.BosCapability
import com.whatcable.android.core.model.BosDescriptor
import com.whatcable.android.core.model.UsbConfigInfo
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbInterfaceInfo
import com.whatcable.android.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val device = uiState.snapshot.connectedDevices.find { it.deviceName == deviceName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.productName ?: device?.manufacturerName ?: "Device") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (device == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Device not found or disconnected.")
            }
        } else {
            DeviceDetailContent(
                device = device,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DeviceDetailContent(
    device: UsbDeviceInfo,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { IdentitySection(device) }

        device.bosDescriptor?.let { bos ->
            item { BosSection(bos) }
        }

        device.billboardDescriptor?.let { billboard ->
            item { BillboardSection(billboard) }
        }

        item {
            Text(
                text = "Configurations",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        items(device.configurations) { config ->
            ConfigurationCard(config)
        }
    }
}

@Composable
private fun IdentitySection(device: UsbDeviceInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Identity", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow("Vendor ID", device.vendorIdHex)
            DetailRow("Product ID", device.productIdHex)
            device.manufacturerName?.let { DetailRow("Manufacturer", it) }
            device.productName?.let { DetailRow("Product", it) }
            device.serialNumber?.let { DetailRow("Serial", it) }
            device.usbVersion?.let { DetailRow("USB Version", it) }
            device.deviceVersion?.let { DetailRow("Device Version", it) }
            DetailRow("Class", "${device.deviceClassLabel} (0x${"%02X".format(device.deviceClass)})")
            if (device.deviceSubclass != 0) {
                DetailRow("Subclass", "0x${"%02X".format(device.deviceSubclass)}")
            }
            if (device.deviceProtocol != 0) {
                DetailRow("Protocol", "0x${"%02X".format(device.deviceProtocol)}")
            }
            device.maxSpeed?.let { speed ->
                DetailRow("Max Speed", "${speed.label} (${speed.gbps} Gbps)")
            }
        }
    }
}

@Composable
private fun BosSection(bos: BosDescriptor) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("BOS Descriptor", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${bos.numDeviceCaps} capabilities, ${bos.totalLength} bytes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            bos.capabilities.forEach { cap ->
                when (cap) {
                    is BosCapability.Usb2Extension -> {
                        CapabilityRow("USB 2.0 Extension", "LPM: ${if (cap.supportsLpm) "Yes" else "No"}")
                    }
                    is BosCapability.SuperSpeed -> {
                        CapabilityRow("SuperSpeed", cap.maxSpeed.label)
                        if (cap.supportsGen1) CapabilityDetail("Gen 1 (5 Gbps)")
                        if (cap.supportsGen2) CapabilityDetail("Gen 2 (10 Gbps)")
                        if (cap.u1DevExitLat > 0) CapabilityDetail("U1 exit latency: ${cap.u1DevExitLat} µs")
                        if (cap.u2DevExitLat > 0) CapabilityDetail("U2 exit latency: ${cap.u2DevExitLat} µs")
                    }
                    is BosCapability.SuperSpeedPlus -> {
                        CapabilityRow("SuperSpeed+", cap.maxSpeed.label)
                        cap.subLinkSpeedAttributes.forEach { link ->
                            CapabilityDetail("${link.direction}: ${"%.1f".format(link.speedGbps)} Gbps (${link.laneCount} lane)")
                        }
                    }
                    is BosCapability.ContainerId -> {
                        CapabilityRow("Container ID", cap.uuid)
                    }
                    is BosCapability.Unknown -> {
                        CapabilityRow("Type 0x${"%02X".format(cap.type)}", "${cap.rawData.size} bytes")
                    }
                }
            }
        }
    }
}

@Composable
private fun BillboardSection(billboard: com.whatcable.android.core.model.BillboardDescriptor) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Billboard Descriptor", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow("Alt Modes", "${billboard.numAltModes}")
            DetailRow("VCONN Power", "${"%.1f".format(billboard.vconnPower.wattsRequired)}W")
            if (billboard.preferredAltModeIndex >= 0) {
                DetailRow("Preferred", "Alt mode #${billboard.preferredAltModeIndex}")
            }
            if (billboard.hasFailedNegotiation) {
                DetailRow("Negotiation", "Failed")
                if (billboard.additionalFailureInfo != 0) {
                    DetailRow("Failure Info", "0x${"%02X".format(billboard.additionalFailureInfo)}")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            billboard.altModes.forEach { altMode ->
                val statusColor = when (altMode.status) {
                    AltModeStatus.CONFIGURATION_SUCCESSFUL -> MaterialTheme.colorScheme.primary
                    AltModeStatus.CONFIGURATION_FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${altMode.svidLabel} (${altMode.svidHex})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = altMode.status.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigurationCard(config: UsbConfigInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Config #${config.id}",
                style = MaterialTheme.typography.titleSmall
            )
            config.name?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DetailRow("Max Power", "${config.maxPower * 2} mA")
            DetailRow("Self Powered", if (config.isSelfPowered) "Yes" else "No")

            if (config.interfaces.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                config.interfaces.forEach { iface ->
                    InterfaceRow(iface)
                }
            }
        }
    }
}

@Composable
private fun InterfaceRow(iface: UsbInterfaceInfo) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Interface #${iface.id}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = iface.classLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        iface.name?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (iface.interfaceSubclass != 0 || iface.interfaceProtocol != 0) {
            Text(
                text = "Subclass: 0x${"%02X".format(iface.interfaceSubclass)}, Protocol: 0x${"%02X".format(iface.interfaceProtocol)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (iface.alternateSetting != 0) {
            Text(
                text = "Alt setting: ${iface.alternateSetting}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        iface.endpoints.forEach { endpoint ->
            EndpointRow(endpoint)
        }
    }
}

@Composable
private fun EndpointRow(endpoint: com.whatcable.android.core.model.UsbEndpointInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "EP 0x${"%02X".format(endpoint.address)} ${endpoint.direction.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${endpoint.type.name.lowercase()} ${endpoint.maxPacketSize}B",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CapabilityRow(name: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, style = MaterialTheme.typography.bodySmall)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CapabilityDetail(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
    )
}
