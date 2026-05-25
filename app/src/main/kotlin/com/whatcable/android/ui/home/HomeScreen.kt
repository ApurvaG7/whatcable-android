package com.whatcable.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatcable.android.core.model.AltModeStatus
import com.whatcable.android.core.model.BosCapability
import com.whatcable.android.core.model.UsbDeviceInfo

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
        if (uiState.devices.isEmpty() && !uiState.isLoading) {
            EmptyState()
        } else {
            DeviceList(devices = uiState.devices)
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
private fun DeviceList(devices: List<UsbDeviceInfo>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Connected Devices",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(devices, key = { it.deviceName }) { device ->
            DeviceCard(device)
        }
    }
}

@Composable
private fun DeviceCard(device: UsbDeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
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

            Row {
                DetailChip("VID ${device.vendorIdHex}")
                Spacer(modifier = Modifier.width(8.dp))
                DetailChip("PID ${device.productIdHex}")
                if (device.usbVersion != null) {
                    Spacer(modifier = Modifier.width(8.dp))
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

            device.maxSpeed?.let { speed ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Max speed: ${speed.label} (${speed.gbps} Gbps)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            device.bosDescriptor?.let { bos ->
                val lpm = bos.capabilities
                    .filterIsInstance<BosCapability.Usb2Extension>()
                    .any { it.supportsLpm }
                if (lpm) {
                    Text(
                        text = "LPM supported",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            device.billboardDescriptor?.let { billboard ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Alt Modes (Billboard)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                billboard.altModes.forEach { altMode ->
                    val statusColor = when (altMode.status) {
                        AltModeStatus.CONFIGURATION_SUCCESSFUL -> MaterialTheme.colorScheme.primary
                        AltModeStatus.CONFIGURATION_FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = "${altMode.svidLabel}: ${altMode.status.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            } ?: run {
                val billboardInterfaces = device.configurations
                    .flatMap { it.interfaces }
                    .filter { it.isBillboard }
                if (billboardInterfaces.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Billboard device (grant permission for details)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val interfaceCount = device.configurations.sumOf { it.interfaces.size }
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
            .padding(horizontal = 2.dp)
    )
}
