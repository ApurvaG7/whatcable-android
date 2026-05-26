package com.whatcable.android.ui.shizuku

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuSetupScreen(
    onBack: () -> Unit,
    viewModel: ShizukuSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshInstallState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shizuku Setup") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IntroCard()

            StepIndicator(currentStep = uiState.currentStep)

            when (uiState.currentStep) {
                SetupStep.INSTALL -> InstallStep(
                    onInstall = { context.startActivity(viewModel.getPlayStoreIntent()) }
                )
                SetupStep.START -> StartStep(
                    onOpenShizuku = {
                        viewModel.getShizukuAppIntent()?.let { context.startActivity(it) }
                    }
                )
                SetupStep.GRANT -> GrantStep(
                    onGrant = { viewModel.requestPermission() },
                    onOpenSettings = { context.startActivity(viewModel.getAppSettingsIntent()) },
                    permanentlyDenied = uiState.permissionPermanentlyDenied
                )
                SetupStep.DONE -> DoneStep(onBack = onBack)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Why Shizuku?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Shizuku gives WhatCable access to system-level USB port data without rooting your device. This unlocks:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletItem("Port orientation (CC1/CC2)")
            BulletItem("Power Delivery role detection")
            BulletItem("USB data status and compliance checks")
            BulletItem("Contaminant detection alerts")
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepIndicator(currentStep: SetupStep) {
    val steps = listOf("Install", "Start", "Grant", "Done")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val stepEnum = SetupStep.entries[index]
            val isComplete = stepEnum.ordinal < currentStep.ordinal
            val isCurrent = stepEnum == currentStep

            val bgColor by animateColorAsState(
                targetValue = when {
                    isComplete -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "step_bg"
            )
            val textColor by animateColorAsState(
                targetValue = when {
                    isComplete || isCurrent -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "step_text"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isComplete) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InstallStep(onInstall: () -> Unit) {
    StepCard(
        title = "Step 1: Install Shizuku",
        description = "Shizuku is a free, open-source app that provides a way for normal apps to access system APIs directly."
    ) {
        Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
            Text("Get Shizuku from Play Store")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Also available on GitHub (github.com/RikkaApps/Shizuku) and F-Droid.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StartStep(onOpenShizuku: () -> Unit) {
    StepCard(
        title = "Step 2: Start Shizuku",
        description = "Shizuku needs to be activated once after each reboot. Open the Shizuku app and follow the activation method that works for your device."
    ) {
        Button(onClick = onOpenShizuku, modifier = Modifier.fillMaxWidth()) {
            Text("Open Shizuku")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Activation methods",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WirelessDebugInstructions()
        } else {
            AdbInstructions()
        }
    }
}

@Composable
private fun WirelessDebugInstructions() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Wireless Debugging (Android 11+)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            NumberedStep(1, "Open Settings > Developer Options")
            NumberedStep(2, "Enable \"Wireless debugging\"")
            NumberedStep(3, "Open the Shizuku app and tap \"Start via Wireless debugging\"")
            NumberedStep(4, "Follow the pairing prompts in Shizuku")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Developer Options: Settings > About phone > tap \"Build number\" 7 times.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "ADB from computer (any Android version)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            NumberedStep(1, "Connect phone to computer via USB")
            NumberedStep(2, "Enable USB debugging in Developer Options")
            NumberedStep(3, "Run this command on your computer:")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.1f))
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun AdbInstructions() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "ADB from computer",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            NumberedStep(1, "Connect phone to computer via USB")
            NumberedStep(2, "Enable USB debugging in Developer Options")
            NumberedStep(3, "Run this command on your computer:")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.1f))
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Developer Options: Settings > About phone > tap \"Build number\" 7 times.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun GrantStep(
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    permanentlyDenied: Boolean
) {
    if (permanentlyDenied) {
        StepCard(
            title = "Step 3: Grant Permission",
            description = "Permission was previously denied. You'll need to grant it manually in app settings."
        ) {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Open App Settings")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Look for \"Shizuku\" under the app's permissions and enable it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        StepCard(
            title = "Step 3: Grant Permission",
            description = "Shizuku is running. WhatCable now needs permission to use it."
        ) {
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                Text("Grant Permission")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Open App Settings")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "If the permission dialog doesn't appear, open app settings and grant Shizuku access manually.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DoneStep(onBack: () -> Unit) {
    StepCard(
        title = "All set!",
        description = "Shizuku is active and WhatCable has permission. You now have access to enhanced USB-C port diagnostics."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back to Dashboard")
            }
        }
    }
}

@Composable
private fun StepCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
