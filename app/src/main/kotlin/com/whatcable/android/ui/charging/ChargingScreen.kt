package com.whatcable.android.ui.charging

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatcable.android.data.charging.ChargingState
import com.whatcable.android.data.db.ChargingSample

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChargingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charging") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                uiState.currentState?.let { state ->
                    LiveStatsCard(state)
                } ?: Text(
                    "Waiting for charging data...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.recentSamples.size >= 2) {
                item { WattageChart(uiState.recentSamples) }
            }

            if (uiState.recentSamples.isNotEmpty()) {
                item { SampleSummary(uiState.recentSamples) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveStatsCard(state: ChargingState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Live Stats", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (state.isCharging) "Charging" else "Not charging",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.isCharging) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox("Battery", "${state.batteryPercent}%")
                state.wattage?.let { StatBox("Power", "${"%.1f".format(it)}W") }
                state.currentMa?.let { StatBox("Current", "${kotlin.math.abs(it)} mA") }
                state.voltageMv?.let { StatBox("Voltage", "${it} mV") }
                StatBox("Temp", "${"%.1f".format(state.temperatureCelsius)} C")
                StatBox("Plug", state.plugType.label)
                state.chargerClass?.let { StatBox("Charger", it) }
                state.negotiatedMaxWatts?.let { StatBox("Negotiated", "${"%.0f".format(it)}W") }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
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
private fun WattageChart(samples: List<ChargingSample>) {
    val chartColor = MaterialTheme.colorScheme.primary

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Power Over Time", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            val wattages = samples.mapNotNull { it.wattage }
            if (wattages.size >= 2) {
                val maxWattage = wattages.max().coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val width = size.width
                    val height = size.height
                    val padding = 8f

                    val path = Path()
                    wattages.forEachIndexed { index, wattage ->
                        val x = padding + (index.toFloat() / (wattages.size - 1)) * (width - 2 * padding)
                        val y = height - padding - ((wattage / maxWattage) * (height - 2 * padding)).toFloat()
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, chartColor, style = Stroke(width = 3f))
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0W",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.1f".format(maxWattage)}W",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleSummary(samples: List<ChargingSample>) {
    val wattages = samples.mapNotNull { it.wattage }
    if (wattages.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Session Summary", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val durationMin = if (samples.size >= 2) {
                (samples.last().timestamp - samples.first().timestamp) / 60_000
            } else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Samples", style = MaterialTheme.typography.bodySmall)
                Text("${samples.size}", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Duration", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = if (durationMin > 0) "$durationMin min" else "< 1 min",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Avg power", style = MaterialTheme.typography.bodySmall)
                Text("${"%.1f".format(wattages.average())}W", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Peak power", style = MaterialTheme.typography.bodySmall)
                Text("${"%.1f".format(wattages.max())}W", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
