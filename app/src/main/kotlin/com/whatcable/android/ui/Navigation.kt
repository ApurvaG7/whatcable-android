package com.whatcable.android.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.whatcable.android.ui.charging.ChargingScreen
import com.whatcable.android.ui.detail.DeviceDetailScreen
import com.whatcable.android.ui.home.HomeScreen
import com.whatcable.android.ui.pro.ProScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun WhatCableNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onDeviceClick = { deviceName ->
                    val encoded = URLEncoder.encode(deviceName, "UTF-8")
                    navController.navigate("device/$encoded")
                },
                onShareReport = { report ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, report)
                        putExtra(Intent.EXTRA_SUBJECT, "WhatCable Report")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share cable report"))
                },
                onChargingClick = { navController.navigate("charging") },
                onTierClick = { navController.navigate("pro") }
            )
        }

        composable("pro") {
            ProScreen(onBack = { navController.popBackStack() })
        }

        composable("charging") {
            ChargingScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "device/{deviceName}",
            arguments = listOf(navArgument("deviceName") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceName = backStackEntry.arguments?.getString("deviceName")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: ""
            DeviceDetailScreen(
                deviceName = deviceName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
