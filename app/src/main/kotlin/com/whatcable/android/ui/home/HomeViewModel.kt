package com.whatcable.android.ui.home

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatcable.android.data.charging.ChargingMonitor
import com.whatcable.android.data.root.RootChecker
import com.whatcable.android.data.root.SuExecutor
import com.whatcable.android.data.usb.UsbEvent
import com.whatcable.android.data.usb.UsbHostScanner
import com.whatcable.android.domain.CableDiagnosticEngine
import com.whatcable.android.domain.CableSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val usbScanner: UsbHostScanner,
    private val chargingMonitor: ChargingMonitor,
    private val diagnosticEngine: CableDiagnosticEngine,
    private val rootChecker: RootChecker,
    private val suExecutor: SuExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkRoot()
        observeUsbEvents()
        observeChargingState()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val snapshot = diagnosticEngine.diagnose()
            _uiState.value = _uiState.value.copy(
                snapshot = snapshot,
                isLoading = false
            )
        }
    }

    fun requestUsbPermission(rawDevice: UsbDevice) {
        viewModelScope.launch {
            val granted = usbScanner.requestPermission(rawDevice).first()
            if (granted) refresh()
        }
    }

    private fun checkRoot() {
        viewModelScope.launch {
            val hasRoot = rootChecker.isRooted && suExecutor.isAvailable()
            _uiState.value = _uiState.value.copy(hasRoot = hasRoot)
        }
    }

    private fun observeUsbEvents() {
        viewModelScope.launch {
            usbScanner.observeUsbEvents().collect { event ->
                when (event) {
                    is UsbEvent.Attached -> refresh()
                    is UsbEvent.Detached -> refresh()
                }
            }
        }
    }

    private fun observeChargingState() {
        var lastChargingState: Boolean? = null
        viewModelScope.launch {
            chargingMonitor.observeCharging().collect { state ->
                if (lastChargingState != null && lastChargingState != state.isCharging) {
                    refresh()
                }
                lastChargingState = state.isCharging
            }
        }
    }
}

data class HomeUiState(
    val snapshot: CableSnapshot = CableSnapshot(),
    val hasRoot: Boolean = false,
    val isLoading: Boolean = false
)
