package com.whatcable.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.CapabilityTier
import com.whatcable.android.core.model.ComplianceWarning
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.data.shizuku.ShizukuUsbPortReader
import com.whatcable.android.domain.AltModeInfo
import com.whatcable.android.domain.CableSnapshot
import com.whatcable.android.domain.ChargingAssessment
import com.whatcable.android.domain.Confidence
import com.whatcable.android.domain.PortState
import com.whatcable.android.domain.SpeedClassification
import com.whatcable.android.domain.TrustRating
import com.whatcable.android.domain.TrustScore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        if (!uiState.snapshot.isConnected && !uiState.isLoading) {
            EmptyState()
        } else {
            DashboardContent(
                snapshot = uiState.snapshot,
                shizukuState = uiState.shizukuState,
                onRequestShizuku = { viewModel.requestShizukuPermission() }
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "WhatCable",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect a USB device to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardContent(
    snapshot: CableSnapshot,
    shizukuState: ShizukuUsbPortReader.ShizukuState,
    onRequestShizuku: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderRow(snapshot.capabilityTier)
        }

        item {
            Text(
                text = snapshot.summary,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            BadgeRow(snapshot)
        }

        if (snapshot.portState != null) {
            item { PortStateCard(snapshot.portState) }
        }

        if (snapshot.speed.tier != null) {
            item { SpeedCard(snapshot.speed) }
        }

        if (snapshot.charging.confidence != Confidence.NONE) {
            item { ChargingCard(snapshot.charging) }
        }

        if (snapshot.altModes.isNotEmpty()) {
            item { AltModesCard(snapshot.altModes) }
        }

        if (snapshot.trustScore.signals.isNotEmpty()) {
            item { TrustCard(snapshot.trustScore) }
        }

        if (snapshot.complianceWarnings.isNotEmpty()) {
            item { ComplianceCard(snapshot.complianceWarnings) }
        }

        if (shizukuState != ShizukuUsbPortReader.ShizukuState.Ready) {
            item { ShizukuPrompt(shizukuState, onRequestShizuku) }
        }

        if (snapshot.connectedDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Connected Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(snapshot.connectedDevices, key = { it.deviceName }) { device ->
                DeviceCard(device)
            }
        }
    }
}

@Composable
private fun HeaderRow(tier: CapabilityTier) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "WhatCable",
            style = MaterialTheme.typography.titleLarge
        )
        TierBadge(tier)
    }
}

@Composable
private fun TierBadge(tier: CapabilityTier) {
    val color = when (tier) {
        CapabilityTier.BASIC -> MaterialTheme.colorScheme.outline
        CapabilityTier.ENHANCED -> MaterialTheme.colorScheme.primary
        CapabilityTier.FULL -> MaterialTheme.colorScheme.tertiary
    }
    Text(
        text = tier.label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BadgeRow(snapshot: CableSnapshot) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        snapshot.speed.tier?.let { tier ->
            InfoBadge(label = tier.label, value = "${tier.gbps} Gbps")
        }
        if (snapshot.charging.isCharging) {
            val powerText = snapshot.charging.maxPowerWatts?.let { "${it.toInt()}W max" } ?: "Active"
            InfoBadge(label = "Charging", value = powerText)
        }
        snapshot.portState?.let { port ->
            if (port.isConnected) {
                InfoBadge(label = "Orientation", value = port.orientation.label)
            }
        }
        if (snapshot.trustScore.signals.isNotEmpty()) {
            val rating = snapshot.trustScore.rating
            InfoBadge(label = "Quality", value = rating.label)
        }
    }
}

@Composable
private fun InfoBadge(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PortStateCard(port: PortState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("USB-C Port", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Mode", style = MaterialTheme.typography.labelSmall)
                    Text(port.mode.label, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("Power", style = MaterialTheme.typography.labelSmall)
                    Text(port.powerRole.label, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("Data", style = MaterialTheme.typography.labelSmall)
                    Text(port.dataRole.label, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("Orientation", style = MaterialTheme.typography.labelSmall)
                    Text(port.orientation.label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SpeedCard(speed: SpeedClassification) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Speed", style = MaterialTheme.typography.titleMedium)
                ConfidenceDot(speed.confidence)
            }
            Spacer(modifier = Modifier.height(4.dp))
            speed.tier?.let { tier ->
                Text(
                    text = "${tier.label} (${tier.gbps} Gbps)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (speed.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sources: ${speed.sources.joinToString(", ") { it.name.lowercase().replace('_', ' ') }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChargingCard(charging: ChargingAssessment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Power", style = MaterialTheme.typography.titleMedium)
                ConfidenceDot(charging.confidence)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = charging.powerRole.label,
                style = MaterialTheme.typography.bodyLarge
            )
            if (charging.powerDeliverySupported) {
                Text(
                    text = "USB Power Delivery supported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            charging.maxPowerWatts?.let { watts ->
                Text(
                    text = "Estimated max: ${watts.toInt()}W",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (charging.powerTransferLimited) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Power transfer limited",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AltModesCard(altModes: List<AltModeInfo>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Alt Modes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            altModes.forEach { altMode ->
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
                        text = altMode.name,
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
private fun TrustCard(trust: TrustScore) {
    val ratingColor = when (trust.rating) {
        TrustRating.HIGH -> MaterialTheme.colorScheme.primary
        TrustRating.MEDIUM -> MaterialTheme.colorScheme.tertiary
        TrustRating.LOW -> MaterialTheme.colorScheme.error
        TrustRating.UNKNOWN -> MaterialTheme.colorScheme.outline
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cable Quality", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${trust.percentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = ratingColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { trust.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ratingColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            trust.signals.forEach { signal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = signal.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = signal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (signal.present) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComplianceCard(warnings: List<ComplianceWarning>) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Compliance Warnings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            warnings.forEach { warning ->
                Text(
                    text = warning.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfidenceDot(confidence: Confidence) {
    val color = when (confidence) {
        Confidence.HIGH -> MaterialTheme.colorScheme.primary
        Confidence.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Confidence.LOW -> MaterialTheme.colorScheme.outline
        Confidence.NONE -> MaterialTheme.colorScheme.outlineVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = confidence.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun ShizukuPrompt(
    state: ShizukuUsbPortReader.ShizukuState,
    onRequest: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val message = when (state) {
                ShizukuUsbPortReader.ShizukuState.NotInstalled ->
                    "Install Shizuku for enhanced USB-C port diagnostics (orientation, power delivery, compliance)"
                ShizukuUsbPortReader.ShizukuState.NotRunning ->
                    "Start Shizuku to unlock enhanced port diagnostics"
                ShizukuUsbPortReader.ShizukuState.PermissionDenied ->
                    "Grant Shizuku permission for enhanced port diagnostics"
                else -> ""
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state == ShizukuUsbPortReader.ShizukuState.PermissionDenied ||
                state == ShizukuUsbPortReader.ShizukuState.NotRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRequest) {
                    Text("Enable Shizuku")
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: UsbDeviceInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = device.productName ?: device.manufacturerName ?: "Unknown Device",
                style = MaterialTheme.typography.titleMedium
            )

            if (device.manufacturerName != null && device.productName != null) {
                Text(
                    text = device.manufacturerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailChip("VID ${device.vendorIdHex}")
                DetailChip("PID ${device.productIdHex}")
                if (device.usbVersion != null) {
                    DetailChip("USB ${device.usbVersion}")
                }
            }

            if (device.deviceClass != 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Class: ${device.deviceClassLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val interfaceCount = device.configurations.sumOf { it.interfaces.size }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$interfaceCount interface${if (interfaceCount != 1) "s" else ""} across ${device.configurations.size} config${if (device.configurations.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
