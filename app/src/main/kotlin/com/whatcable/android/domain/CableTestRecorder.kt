package com.whatcable.android.domain

import com.whatcable.android.data.charging.ChargingMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a timed capture of charging power so two cables can be compared on the
 * same charger. Comparison only isolates the cable when the charger and the
 * starting battery level are held roughly constant between runs, so the result
 * carries those conditions for the UI to surface.
 */
@Singleton
class CableTestRecorder @Inject constructor(
    private val chargingMonitor: ChargingMonitor
) {

    /** Live progress emitted during a recording so the UI can show the capture. */
    data class Progress(
        val elapsedMs: Long,
        val durationMs: Long,
        val latestWatts: Double?,
        val peakWatts: Double,
        val sampleCount: Int
    ) {
        val fraction: Float get() = if (durationMs > 0) (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    }

    /** The measured outcome of a run, before the user names and saves it. */
    data class Result(
        val peakWatts: Double,
        val sustainedWatts: Double,
        val negotiatedMaxWatts: Double?,
        val negotiatedMaxVoltageMv: Int?,
        val chargerClass: String?,
        val startBatteryPercent: Int,
        val endBatteryPercent: Int,
        val sampleCount: Int,
        val durationMs: Long,
        val timestamp: Long
    )

    sealed interface Outcome {
        data class Success(val result: Result) : Outcome
        /** Nothing was plugged in / charging when the run started. */
        data object NotCharging : Outcome
        /** Charging dropped out mid-run, or no usable wattage samples were seen. */
        data object NoData : Outcome
    }

    /**
     * Sample charging power on a fixed timer for [durationMs], reporting
     * [onProgress] each tick, then summarise. Polling (rather than listening for
     * ACTION_BATTERY_CHANGED) is deliberate: the broadcast only fires when
     * something changes, so on a steady or near-full charge it can stay silent
     * and the dial would look frozen. Reading the current sensor on a clock gives
     * a smooth dial and a reliable sample count regardless of charge state.
     * Cancellable by cancelling the calling coroutine.
     */
    suspend fun record(
        durationMs: Long,
        nowMs: Long,
        onProgress: (Progress) -> Unit
    ): Outcome {
        val first = chargingMonitor.readCurrentState()
        if (first == null || !first.isCharging) return Outcome.NotCharging

        val startBattery = first.batteryPercent
        val watts = mutableListOf<Double>()
        var endBattery = startBattery
        var peak = 0.0

        var elapsed = 0L
        while (elapsed < durationMs) {
            currentCoroutineContext().ensureActive()
            val state = chargingMonitor.readCurrentState()
            val w = state?.wattage
            if (state != null) endBattery = state.batteryPercent
            if (w != null && w > 0) {
                watts.add(w)
                if (w > peak) peak = w
            }
            onProgress(
                Progress(
                    elapsedMs = elapsed,
                    durationMs = durationMs,
                    latestWatts = w,
                    peakWatts = peak,
                    sampleCount = watts.size
                )
            )
            delay(SAMPLE_INTERVAL_MS)
            elapsed += SAMPLE_INTERVAL_MS
        }

        if (watts.isEmpty()) return Outcome.NoData

        val sustained = sustainedWatts(watts)
        return Outcome.Success(
            Result(
                peakWatts = peak,
                sustainedWatts = sustained,
                negotiatedMaxWatts = first.negotiatedMaxWatts,
                negotiatedMaxVoltageMv = first.negotiatedMaxVoltageMv,
                chargerClass = first.chargerClass,
                startBatteryPercent = startBattery,
                endBatteryPercent = endBattery,
                sampleCount = watts.size,
                durationMs = durationMs,
                timestamp = nowMs
            )
        )
    }

    /**
     * A robust "real" rate that ignores momentary spikes: the mean of the top
     * half of samples. With few samples this just falls back to the plain mean.
     */
    private fun sustainedWatts(watts: List<Double>): Double {
        if (watts.size < 4) return watts.average()
        val sorted = watts.sorted()
        val topHalf = sorted.subList(sorted.size / 2, sorted.size)
        return topHalf.average()
    }

    companion object {
        /** How often the charging sensor is polled during a test. */
        const val SAMPLE_INTERVAL_MS = 1_000L
    }
}
