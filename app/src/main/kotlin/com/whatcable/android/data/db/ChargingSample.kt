package com.whatcable.android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_samples")
data class ChargingSample(
    @PrimaryKey val timestamp: Long,
    val isCharging: Boolean,
    val batteryPercent: Int,
    val currentMa: Int?,
    val voltageMv: Int?,
    val wattage: Double?,
    val temperatureTenths: Int,
    val chargerType: String?
)
