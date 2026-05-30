package com.whatcable.android.ui.cabletest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatcable.android.data.db.CableTest
import com.whatcable.android.data.db.CableTestDao
import com.whatcable.android.domain.CableTestRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CableTestViewModel @Inject constructor(
    private val recorder: CableTestRecorder,
    private val cableTestDao: CableTestDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CableTestUiState())
    val uiState: StateFlow<CableTestUiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null

    init {
        viewModelScope.launch {
            cableTestDao.getAllByPeak().collect { tests ->
                _uiState.update { it.copy(savedTests = tests) }
            }
        }
    }

    fun startTest() {
        if (_uiState.value.phase is TestPhase.Recording) return
        recordingJob = viewModelScope.launch {
            _uiState.update { it.copy(phase = TestPhase.Recording(null), message = null) }
            val outcome = recorder.record(
                durationMs = TEST_DURATION_MS,
                nowMs = System.currentTimeMillis()
            ) { progress ->
                _uiState.update { state ->
                    if (state.phase is TestPhase.Recording) {
                        state.copy(phase = TestPhase.Recording(progress))
                    } else state
                }
            }
            when (outcome) {
                is CableTestRecorder.Outcome.Success ->
                    _uiState.update { it.copy(phase = TestPhase.Review(outcome.result)) }
                CableTestRecorder.Outcome.NotCharging ->
                    _uiState.update {
                        it.copy(
                            phase = TestPhase.Idle,
                            message = "Plug the cable into a charger first, then start the test."
                        )
                    }
                CableTestRecorder.Outcome.NoData ->
                    _uiState.update {
                        it.copy(
                            phase = TestPhase.Idle,
                            message = "No charging data captured. Make sure it stayed plugged in."
                        )
                    }
            }
        }
    }

    fun cancelTest() {
        recordingJob?.cancel()
        recordingJob = null
        _uiState.update { it.copy(phase = TestPhase.Idle) }
    }

    fun discardReview() {
        _uiState.update { it.copy(phase = TestPhase.Idle) }
    }

    fun saveResult(label: String) {
        val phase = _uiState.value.phase
        if (phase !is TestPhase.Review) return
        val name = label.trim().ifBlank { "Cable ${_uiState.value.savedTests.size + 1}" }
        val r = phase.result
        viewModelScope.launch {
            cableTestDao.insert(
                CableTest(
                    label = name,
                    timestamp = r.timestamp,
                    peakWatts = r.peakWatts,
                    sustainedWatts = r.sustainedWatts,
                    negotiatedMaxWatts = r.negotiatedMaxWatts,
                    negotiatedMaxVoltageMv = r.negotiatedMaxVoltageMv,
                    chargerClass = r.chargerClass,
                    startBatteryPercent = r.startBatteryPercent,
                    endBatteryPercent = r.endBatteryPercent,
                    sampleCount = r.sampleCount,
                    durationMs = r.durationMs
                )
            )
            _uiState.update { it.copy(phase = TestPhase.Idle, message = "Saved \"$name\".") }
        }
    }

    fun deleteTest(test: CableTest) {
        viewModelScope.launch { cableTestDao.delete(test) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        const val TEST_DURATION_MS = 30_000L
    }
}

sealed interface TestPhase {
    data object Idle : TestPhase
    data class Recording(val progress: CableTestRecorder.Progress?) : TestPhase
    data class Review(val result: CableTestRecorder.Result) : TestPhase
}

data class CableTestUiState(
    val phase: TestPhase = TestPhase.Idle,
    val savedTests: List<CableTest> = emptyList(),
    val message: String? = null
)
