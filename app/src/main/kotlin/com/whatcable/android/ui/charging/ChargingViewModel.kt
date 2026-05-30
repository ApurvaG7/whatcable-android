package com.whatcable.android.ui.charging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatcable.android.data.charging.ChargingMonitor
import com.whatcable.android.data.charging.ChargingState
import com.whatcable.android.data.db.ChargingSample
import com.whatcable.android.data.db.ChargingSampleDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChargingViewModel @Inject constructor(
    private val chargingMonitor: ChargingMonitor,
    private val sampleDao: ChargingSampleDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChargingUiState())
    val uiState: StateFlow<ChargingUiState> = _uiState.asStateFlow()

    init {
        observeCharging()
        loadHistory()
    }

    private fun observeCharging() {
        viewModelScope.launch {
            chargingMonitor.observeCharging().collect { state ->
                _uiState.value = _uiState.value.copy(currentState = state)
                recordSample(state)
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val oneHourAgo = System.currentTimeMillis() - 3_600_000L
            sampleDao.getSamplesSince(oneHourAgo).collect { samples ->
                _uiState.value = _uiState.value.copy(recentSamples = samples)
            }
        }
    }

    private var lastCleanup = 0L

    private suspend fun recordSample(state: ChargingState) {
        val lastSample = _uiState.value.recentSamples.lastOrNull()
        val minInterval = 10_000L
        if (lastSample != null && (state.timestamp - lastSample.timestamp) < minInterval) return

        sampleDao.insert(
            ChargingSample(
                timestamp = state.timestamp,
                isCharging = state.isCharging,
                batteryPercent = state.batteryPercent,
                currentMa = state.currentMa,
                voltageMv = state.voltageMv,
                wattage = state.wattage,
                temperatureTenths = state.temperatureTenths,
                negotiatedMaxVoltageMv = state.negotiatedMaxVoltageMv,
                negotiatedMaxCurrentMa = state.negotiatedMaxCurrentMa
            )
        )

        val now = System.currentTimeMillis()
        if (now - lastCleanup > 3_600_000L) {
            lastCleanup = now
            sampleDao.deleteOlderThan(now - 86_400_000L)
        }
    }
}

data class ChargingUiState(
    val currentState: ChargingState? = null,
    val recentSamples: List<ChargingSample> = emptyList()
)
