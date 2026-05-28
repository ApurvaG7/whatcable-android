package com.whatcable.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
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
import com.whatcable.android.ui.theme.Blue40
import com.whatcable.android.ui.theme.Blue60
import com.whatcable.android.ui.theme.Green60

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
    val glowBlue = Blue40
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowBlue.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width / 2, size.height * 0.4f),
                        radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width / 2, size.height * 0.4f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(Blue60, Blue40)),
                        CircleShape
                    )
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "USB-C",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "WhatCable",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Connect a USB-C device to begin diagnostics",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            HeaderRow(snapshot.capabilityTier, onShareReport = onShareReport, snapshot = snapshot)
        }

        item {
            HeroSection(snapshot)
        }

        if (snapshot.speed.tier != null) {
            item { SpeedCard(snapshot.speed) }
        }

        if (snapshot.portState != null) {
            item { PortStateCard(snapshot.portState) }
        }

        if (snapshot.charging.confidence != Confidence.NONE) {
            item { ChargingCard(snapshot.charging, hasDevices = snapshot.connectedDevices.isNotEmpty(), onClick = onChargingClick) }
        }

        if (!snapshot.isConnected && snapshot.batteryState != null) {
            item { BatteryCard(snapshot.batteryState, onClick = onChargingClick) }
        }

        if (snapshot.trustScore.signals.isNotEmpty()) {
            item { TrustCard(snapshot.trustScore) }
        }

        if (snapshot.altModes.isNotEmpty()) {
            item { AltModesCard(snapshot.altModes) }
        }

        if (snapshot.complianceWarnings.isNotEmpty()) {
            item { ComplianceCard(snapshot.complianceWarnings) }
        }

        if (snapshot.connectedDevices.isNotEmpty()) {
            item {
                Text(
                    text = "CONNECTED DEVICES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
            items(snapshot.connectedDevices, key = { it.deviceName }) { device ->
                DeviceCard(device, onClick = { onDeviceClick(device.deviceName) })
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            TierBadge(tier)
        }
    }
}

@Composable
private fun HeroSection(snapshot: CableSnapshot) {
    val accentBrush = Brush.linearGradient(listOf(Blue60, Blue40))
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = snapshot.summary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        BadgeRow(snapshot)
    }
}

@Composable
private fun TierBadge(tier: CapabilityTier) {
    val color = when (tier) {
        CapabilityTier.BASIC -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        CapabilityTier.FULL -> Green60
    }
    Text(
        text = tier.label.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 1.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
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
            InfoBadge(label = "SPEED", value = "${tier.gbps} Gbps", accent = Blue60)
        }
        if (snapshot.charging.isCharging) {
            val powerText = snapshot.charging.maxPowerWatts?.let { "${it.toInt()}W" } ?: "Active"
            InfoBadge(label = "POWER", value = powerText, accent = Green60)
        }
        snapshot.portState?.let { port ->
            if (port.isConnected) {
                InfoBadge(label = "ORIENT.", value = port.orientation.label)
            }
        }
        if (snapshot.trustScore.signals.isNotEmpty()) {
            val ratingColor = when (snapshot.trustScore.rating) {
                TrustRating.HIGH -> Green60
                TrustRating.MEDIUM -> Color(0xFFFBBF24)
                TrustRating.LOW -> Color(0xFFF87171)
                TrustRating.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            InfoBadge(label = "QUALITY", value = "${snapshot.trustScore.percentage}%", accent = ratingColor)
        }
    }
}

@Composable
private fun InfoBadge(
    label: String,
    value: String,
    accent: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent)
        )
        Column(modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = accent
            )
        }
    }
}

@Composable
private fun AccentCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )

    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = cardColors) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.3f))
                            )
                        )
                )
                Box(modifier = Modifier.weight(1f).padding(16.dp)) { content() }
            }
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = cardColors) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.3f))
                            )
                        )
                )
                Box(modifier = Modifier.weight(1f).padding(16.dp)) { content() }
            }
        }
    }
}

@Composable
private fun CardTitle(title: String, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp
        )
        trailing()
    }
}

@Composable
private fun PortStateCard(port: PortState) {
    AccentCard(accentColor = MaterialTheme.colorScheme.primary) {
        Column {
            CardTitle("USB-C Port")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("Mode", port.mode.label)
                MetricColumn("Power", port.powerRole.label)
                MetricColumn("Data", port.dataRole.label)
                MetricColumn("Flip", port.orientation.label)
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SpeedCard(speed: SpeedClassification) {
    AccentCard(accentColor = Blue60) {
        Column {
            CardTitle("Speed") { ConfidenceDot(speed.confidence) }
            Spacer(modifier = Modifier.height(8.dp))
            speed.tier?.let { tier ->
                Text(
                    text = tier.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${tier.gbps}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Blue60,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = " Gbps",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Blue60.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            if (speed.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "via ${speed.sources.joinToString(", ") { it.name.lowercase().replace('_', ' ') }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ChargingCard(charging: ChargingAssessment, hasDevices: Boolean = false, onClick: () -> Unit = {}) {
    val accentColor = if (charging.isCharging) Green60 else MaterialTheme.colorScheme.onSurfaceVariant
    AccentCard(accentColor = accentColor, onClick = onClick) {
        Column {
            CardTitle("Power") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConfidenceDot(charging.confidence)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val powerLabel = when (charging.powerRole) {
                PowerRole.SOURCE -> "Powering device"
                PowerRole.SINK -> "Charging"
                PowerRole.NONE -> when {
                    charging.isCharging -> "Charging"
                    hasDevices -> "Powering device"
                    else -> "Not charging"
                }
            }
            Text(
                text = powerLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (charging.powerDeliverySupported) {
                Text(
                    text = "USB Power Delivery",
                    fontSize = 12.sp,
                    color = Green60
                )
            }
            charging.maxPowerWatts?.let { watts ->
                Text(
                    text = "Max ${watts.toInt()}W",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            if (charging.powerTransferLimited) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "POWER LIMITED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun BatteryCard(state: ChargingState, onClick: () -> Unit = {}) {
    AccentCard(accentColor = Green60, onClick = onClick) {
        Column {
            CardTitle("Charging") {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("Battery", "${state.batteryPercent}%")
                state.wattage?.let { MetricColumn("Power", "%.1fW".format(it)) }
                state.currentMa?.let { MetricColumn("Current", "${kotlin.math.abs(it)} mA") }
                state.voltageMv?.let { MetricColumn("Voltage", "$it mV") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                MetricColumn("Temp", "%.1f°C".format(state.temperatureCelsius))
                MetricColumn("Plug", state.plugType.label)
                state.chargerType?.let { MetricColumn("Type", it) }
            }
        }
    }
}

@Composable
private fun AltModesCard(altModes: List<AltModeInfo>) {
    AccentCard(accentColor = Color(0xFF8B5CF6)) {
        Column {
            CardTitle("Alt Modes")
            Spacer(modifier = Modifier.height(10.dp))
            altModes.forEach { altMode ->
                val statusColor = when (altMode.status) {
                    AltModeStatus.CONFIGURATION_SUCCESSFUL -> Green60
                    AltModeStatus.CONFIGURATION_FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = altMode.name,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = altMode.status.label,
                            fontSize = 11.sp,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustCard(trust: TrustScore) {
    val ratingColor = when (trust.rating) {
        TrustRating.HIGH -> Green60
        TrustRating.MEDIUM -> Color(0xFFFBBF24)
        TrustRating.LOW -> Color(0xFFF87171)
        TrustRating.UNKNOWN -> MaterialTheme.colorScheme.outline
    }

    AccentCard(accentColor = ratingColor) {
        Column {
            CardTitle("Cable Quality") {
                Text(
                    text = "${trust.percentage}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = ratingColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { trust.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ratingColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(14.dp))
            trust.signals.forEach { signal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (signal.present) Green60
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = signal.name,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = signal.description,
                        fontSize = 11.sp,
                        color = if (signal.present) Green60.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComplianceCard(warnings: List<ComplianceWarning>) {
    AccentCard(accentColor = MaterialTheme.colorScheme.error) {
        Column {
            CardTitle("Compliance Warnings")
            Spacer(modifier = Modifier.height(8.dp))
            warnings.forEach { warning ->
                Text(
                    text = warning.label,
                    fontSize = 12.sp,
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
        Confidence.HIGH -> Green60
        Confidence.MEDIUM -> Color(0xFFFBBF24)
        Confidence.LOW -> MaterialTheme.colorScheme.outline
        Confidence.NONE -> MaterialTheme.colorScheme.outlineVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = confidence.name.lowercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun DeviceCard(device: UsbDeviceInfo, onClick: () -> Unit = {}) {
    AccentCard(accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), onClick = onClick) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.productName ?: device.manufacturerName ?: "Unknown Device",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (device.manufacturerName != null && device.productName != null) {
                Text(
                    text = device.manufacturerName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailChip("VID ${device.vendorIdHex}")
                DetailChip("PID ${device.productIdHex}")
                if (device.usbVersion != null) {
                    DetailChip("USB ${device.usbVersion}")
                }
            }

            val interfaceCount = device.configurations.sumOf { it.interfaces.size }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$interfaceCount interface${if (interfaceCount != 1) "s" else ""}, ${device.configurations.size} config${if (device.configurations.size != 1) "s" else ""}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}
