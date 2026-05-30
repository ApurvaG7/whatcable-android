package com.whatcable.android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved comparative cable test. Each row is one measured run of a single cable
 * on a charger: how much power actually flowed (peak and sustained) plus the
 * negotiated ceiling and the conditions it was measured under. Comparing rows
 * taken on the same charger at a similar battery level isolates the cable.
 */
@Entity(tableName = "cable_tests")
data class CableTest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val timestamp: Long,
    val peakWatts: Double,
    val sustainedWatts: Double,
    val negotiatedMaxWatts: Double?,
    val negotiatedMaxVoltageMv: Int?,
    val chargerClass: String?,
    val startBatteryPercent: Int,
    val endBatteryPercent: Int,
    val sampleCount: Int,
    val durationMs: Long
)
