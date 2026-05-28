package com.whatcable.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.CapabilityTier
import com.whatcable.android.core.model.ComplianceWarning
import com.whatcable.android.core.model.PowerRole
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.data.charging.ChargingState
import com.whatcable.android.domain.AltModeInfo
import com.whatcable.android.domain.CableReportGenerator
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
    onDeviceClick: (String) -> Unit = {},
    onShareReport: (String) -> Unit = {},
    onChargingClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        if (!uiState.snapshot.isPluggedIn && !uiState.isLoading) {
            EmptyState()
        } else {
            DashboardContent(
                snapshot = uiState.snapshot,
                onDeviceClick = onDeviceClick,
                onShareReport = onShareReport,
                onChargingClick = onChargingClick
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "USB",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "WhatCable",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect a USB-C device to diagnose your cable",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DashboardContent(
    snapshot: CableSnapshot,
    onDeviceClick: (String) -> Unit,
    onShareReport: (String) -> Unit,
    onChargingClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderRow(snapshot.capabilityTier, onShareReport = onShareReport, snapshot = snapshot)
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
            item { ChargingCard(snapshot.charging, onClick = onChargingClick) }
        }

        if (!snapshot.isConnected && snapshot.batteryState != null) {
            item { BatteryCard(snapshot.batteryState, onClick = onChargingClick) }
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

        if (snapshot.connectedDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Connected Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(snapshot.connectedDevices, key = { it.deviceName }) { device ->
                DeviceCard(device, onClick = { onDeviceClick(device.deviceName) })
            }
        }
    }
}

@Composable
private fun HeaderRow(
    tier: CapabilityTier,
    onShareReport: (String) -> Unit,
    snapshot: CableSnapshot
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "WhatCable",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (snapshot.isConnected) {
                IconButton(onClick = {
                    val report = CableReportGenerator().generate(snapshot)
                    onShareReport(report)
                }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share report",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TierBadge(tier)
        }
    }
}

@Composable
private fun TierBadge(tier: CapabilityTier) {
    val color = when (tier) {
        CapabilityTier.BASIC -> MaterialTheme.colorScheme.outline
        CapabilityTier.FULL -> MaterialTheme.colorScheme.tertiary
    }
    Text(
        text = tier.label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
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
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Speed", style = MaterialTheme.typography.titleMedium)
                ConfidenceDot(speed.confidence)
            }
            Spacer(modifier = Modifier.height(8.dp))
            speed.tier?.let { tier ->
                Text(
                    text = tier.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${tier.gbps} Gbps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (speed.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Source: ${speed.sources.joinToString(", ") { it.name.lowercase().replace('_', ' ') }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChargingCard(charging: ChargingAssessment, onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Power", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConfidenceDot(charging.confidence)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val powerLabel = when (charging.powerRole) {
                PowerRole.SOURCE -> "Powering device"
                PowerRole.SINK -> "Charging"
                PowerRole.NONE -> if (charging.isCharging) "Charging" else "Not charging"
            }
            Text(
                text = powerLabel,
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
private fun BatteryCard(state: ChargingState, onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Charging", style = MaterialTheme.typography.titleMedium)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Battery", style = MaterialTheme.typography.labelSmall)
                    Text("${state.batteryPercent}%", style = MaterialTheme.typography.bodyMedium)
                }
                state.wattage?.let {
                    Column {
                        Text("Power", style = MaterialTheme.typography.labelSmall)
                        Text("%.1fW".format(it), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                state.currentMa?.let {
                    Column {
                        Text("Current", style = MaterialTheme.typography.labelSmall)
                        Text("${kotlin.math.abs(it)} mA", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                state.voltageMv?.let {
                    Column {
                        Text("Voltage", style = MaterialTheme.typography.labelSmall)
                        Text("$it mV", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Temp", style = MaterialTheme.typography.labelSmall)
                    Text("%.1f C".format(state.temperatureCelsius), style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Plug", style = MaterialTheme.typography.labelSmall)
                    Text(state.plugType.label, style = MaterialTheme.typography.bodyMedium)
                }
                state.chargerType?.let {
                    Column {
                        Text("Type", style = MaterialTheme.typography.labelSmall)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cable Quality", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${trust.percentage}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ratingColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { trust.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = ratingColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(12.dp))
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
private fun DeviceCard(device: UsbDeviceInfo, onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.productName ?: device.manufacturerName ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

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
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
