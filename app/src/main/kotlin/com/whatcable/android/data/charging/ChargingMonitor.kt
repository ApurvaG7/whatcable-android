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

        // Negotiated charging ceiling: what the charger + cable agreed the link can
        // carry, regardless of how much is actually flowing right now. The framework
        // constants EXTRA_MAX_CHARGING_VOLTAGE / EXTRA_MAX_CHARGING_CURRENT are @hide,
        // so we read the extras by their stable string keys. The values are present in
        // the ACTION_BATTERY_CHANGED bundle and need no permission to read.
        // Voltage is in microvolts, current in microamps.
        val maxChargingVoltageUv = intent.getIntExtra("max_charging_voltage", 0)
        val maxChargingCurrentUa = intent.getIntExtra("max_charging_current", 0)

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentMicroAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (currentMicroAmps != Int.MIN_VALUE) currentMicroAmps / 1000 else null

        val voltageMv = if (voltage > 0) voltage else null
        val wattage = if (currentMa != null && voltageMv != null && voltageMv > 0) {
            kotlin.math.abs(currentMa.toDouble() * voltageMv.toDouble() / 1_000_000.0)
        } else null

        val negotiatedMaxVoltageMv = if (maxChargingVoltageUv > 0) maxChargingVoltageUv / 1000 else null
        val negotiatedMaxCurrentMa = if (maxChargingCurrentUa > 0) maxChargingCurrentUa / 1000 else null

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
            negotiatedMaxVoltageMv = negotiatedMaxVoltageMv,
            negotiatedMaxCurrentMa = negotiatedMaxCurrentMa
        )
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
    val negotiatedMaxVoltageMv: Int?,
    val negotiatedMaxCurrentMa: Int?
) {
    val temperatureCelsius: Double get() = temperatureTenths / 10.0

    /** Negotiated ceiling in watts (max voltage x max current the link agreed to). */
    val negotiatedMaxWatts: Double?
        get() {
            val v = negotiatedMaxVoltageMv ?: return null
            val a = negotiatedMaxCurrentMa ?: return null
            if (v <= 0 || a <= 0) return null
            return (v.toDouble() * a.toDouble()) / 1_000_000.0
        }

    /**
     * Rough charger class inferred from the negotiated voltage. A basic charger only
     * ever offers 5V; PD/QC fast chargers negotiate 9V or higher. This is an inference
     * from the negotiated ceiling, not a read of the charger's identity (which is not
     * available to a non-root app).
     */
    val chargerClass: String?
        get() {
            val mv = negotiatedMaxVoltageMv ?: return null
            return when {
                mv >= 8500 -> "Fast charger (${mv / 1000}V)"
                mv in 1..6000 -> "Basic 5V charger"
                else -> null
            }
        }
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
