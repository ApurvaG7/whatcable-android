package com.whatcable.android.domain

import com.whatcable.android.data.charging.ChargingMonitor
import com.whatcable.android.data.charging.ChargingState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
     * Collect charging samples for [durationMs], reporting [onProgress] as they
     * arrive, then summarise. Cancellable by cancelling the calling coroutine.
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
        val startElapsed = nowMs

        // The recorder is time-bounded; we stop collecting once the window passes.
        withTimeoutOrNull(durationMs) {
            chargingMonitor.observeCharging().collect { state: ChargingState ->
                endBattery = state.batteryPercent
                val w = state.wattage
                if (w != null && w > 0) {
                    watts.add(w)
                    if (w > peak) peak = w
                }
                onProgress(
                    Progress(
                        elapsedMs = (startElapsed + watts.size).coerceAtMost(durationMs),
                        durationMs = durationMs,
                        latestWatts = w,
                        peakWatts = peak,
                        sampleCount = watts.size
                    )
                )
            }
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
}
