package com.whatcable.android.ui.home

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatcable.android.data.shizuku.ShizukuUsbPortReader
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
    private val shizukuPortReader: ShizukuUsbPortReader,
    private val diagnosticEngine: CableDiagnosticEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        shizukuPortReader.init()
        observeUsbEvents()
        observeShizukuState()
        refresh()
    }

    override fun onCleared() {
        super.onCleared()
        shizukuPortReader.destroy()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val snapshot = diagnosticEngine.diagnose()
            _uiState.value = _uiState.value.copy(
                snapshot = snapshot,
                shizukuState = shizukuPortReader.state.value,
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

    private fun observeShizukuState() {
        viewModelScope.launch {
            shizukuPortReader.state.collect { state ->
                if (state == ShizukuUsbPortReader.ShizukuState.Ready) {
                    refresh()
                }
                _uiState.value = _uiState.value.copy(shizukuState = state)
            }
        }
    }
}

data class HomeUiState(
    val snapshot: CableSnapshot = CableSnapshot(),
    val shizukuState: ShizukuUsbPortReader.ShizukuState = ShizukuUsbPortReader.ShizukuState.NotRunning,
    val isLoading: Boolean = false
)
