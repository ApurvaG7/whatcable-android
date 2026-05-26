package com.whatcable.android.data.charging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargingMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun observeCharging(): Flow<ChargingState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    trySend(parseState(intent))
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        stickyIntent?.let { trySend(parseState(it)) }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {}
        }
    }

    fun readCurrentState(): ChargingState? {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter) ?: return null
        return parseState(intent)
    }

    private fun parseState(intent: Intent): ChargingState {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentMicroAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (currentMicroAmps != Int.MIN_VALUE) currentMicroAmps / 1000 else null

        val voltageMv = if (voltage > 0) voltage else null
        val wattage = if (currentMa != null && voltageMv != null && voltageMv > 0) {
            kotlin.math.abs(currentMa.toDouble() * voltageMv.toDouble() / 1_000_000.0)
        } else null

        val chargerType = readChargerType()
        val percentFull = if (scale > 0) (level * 100) / scale else 0

        return ChargingState(
            timestamp = System.currentTimeMillis(),
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            plugType = PlugType.fromInt(plugged),
            batteryPercent = percentFull,
            currentMa = currentMa,
            voltageMv = voltageMv,
            wattage = wattage,
            temperatureTenths = temperature,
            chargerType = chargerType
        )
    }

    private fun readChargerType(): String? {
        val paths = listOf(
            "/sys/class/power_supply/usb/type",
            "/sys/class/power_supply/usb/real_type",
            "/sys/class/power_supply/charger/type"
        )
        for (path in paths) {
            try {
                val content = File(path).readText().trim()
                if (content.isNotEmpty()) return content
            } catch (_: Exception) {}
        }
        return null
    }
}

data class ChargingState(
    val timestamp: Long,
    val isCharging: Boolean,
    val plugType: PlugType,
    val batteryPercent: Int,
    val currentMa: Int?,
    val voltageMv: Int?,
    val wattage: Double?,
    val temperatureTenths: Int,
    val chargerType: String?
) {
    val temperatureCelsius: Double get() = temperatureTenths / 10.0
}

enum class PlugType(val label: String) {
    NONE("Not plugged in"),
    AC("AC"),
    USB("USB"),
    WIRELESS("Wireless"),
    DOCK("Dock");

    companion object {
        fun fromInt(value: Int): PlugType = when (value) {
            BatteryManager.BATTERY_PLUGGED_AC -> AC
            BatteryManager.BATTERY_PLUGGED_USB -> USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> WIRELESS
            BatteryManager.BATTERY_PLUGGED_DOCK -> DOCK
            else -> NONE
        }
    }
}
