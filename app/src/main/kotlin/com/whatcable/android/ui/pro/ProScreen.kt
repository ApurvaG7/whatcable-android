package com.whatcable.android.ui.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatcable.android.ui.theme.Blue40
import com.whatcable.android.ui.theme.Blue60
import com.whatcable.android.ui.theme.Green60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Full Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            TierComparison()

            FeatureCard(
                accent = Blue60,
                title = "Cable Identity",
                description = "Read the cable's USB PD identity from sysfs, including vendor/product IDs, cable type (passive/active), and max current rating."
            )

            FeatureCard(
                accent = Blue60,
                title = "Speed from VDO",
                description = "Parse the cable's Product Type VDO for the actual USB Highest Speed field. Confirms USB4 Gen 2/Gen 3 support directly from the cable's chip."
            )

            FeatureCard(
                accent = Blue60,
                title = "Port Orientation",
                description = "Read which CC pin is active (CC1/CC2) to determine cable orientation. Useful for diagnosing flaky connections."
            )

            FeatureCard(
                accent = Blue60,
                title = "Alt Modes from sysfs",
                description = "Enumerate partner and cable alt modes (DisplayPort, Thunderbolt) directly from the kernel's typec subsystem."
            )

            FeatureCard(
                accent = Blue60,
                title = "Power Role and Data Role",
                description = "Read the active power role (source/sink) and data role (host/device) from the USB-C port driver."
            )

            HowToCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TierComparison() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "WHAT YOU GET",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            ComparisonRow("Speed detection", basic = true, full = true)
            ComparisonRow("BOS descriptors", basic = true, full = true)
            ComparisonRow("Device details", basic = true, full = true)
            ComparisonRow("Charging monitor", basic = true, full = true)
            ComparisonRow("Quality score", basic = true, full = true)
            ComparisonRow("Cable identity (VDO)", basic = false, full = true)
            ComparisonRow("Port orientation", basic = false, full = true)
            ComparisonRow("Alt modes (sysfs)", basic = false, full = true)
            ComparisonRow("Power/data roles", basic = false, full = true)
            ComparisonRow("Compliance checks", basic = false, full = true)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TierLabel("Basic", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(16.dp))
                TierLabel("Full", Green60)
            }
        }
    }
}

@Composable
private fun ComparisonRow(feature: String, basic: Boolean, full: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusDot(present = basic, color = if (basic) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent)
            StatusDot(present = full, color = if (full) Green60 else Color.Transparent)
        }
    }
}

@Composable
private fun StatusDot(present: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp),
        contentAlignment = Alignment.Center
    ) {
        if (present) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        } else {
            Text(
                text = "-",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun TierLabel(label: String, color: Color) {
    Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun FeatureCard(accent: Color, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent, accent.copy(alpha = 0.3f))
                        )
                    )
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun HowToCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(Green60, Green60.copy(alpha = 0.3f))
                        )
                    )
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HOW TO UNLOCK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green60,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Full diagnostics require root access. WhatCable reads directly from the kernel's USB Type-C subsystem at /sys/class/typec/ to get data that normal apps can't access.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Compatible with Magisk, KernelSU, and APatch. If your device is already rooted, WhatCable detects it automatically and upgrades to Full tier.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
