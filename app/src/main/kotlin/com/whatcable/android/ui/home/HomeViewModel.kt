package com.whatcable.android.ui.home

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatcable.android.core.model.CapabilityTier
import com.whatcable.android.core.model.UsbDeviceInfo
import com.whatcable.android.core.model.UsbPortInfo
import com.whatcable.android.data.shizuku.ShizukuSetupHelper
import com.whatcable.android.data.shizuku.ShizukuUsbPortReader
import com.whatcable.android.data.usb.UsbEvent
import com.whatcable.android.data.usb.UsbHostScanner
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
    private val shizukuSetup: ShizukuSetupHelper
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
            val devices = usbScanner.scan()
            val ports = if (shizukuPortReader.hasPermission()) {
                shizukuPortReader.readPorts()
            } else emptyList()

            _uiState.value = _uiState.value.copy(
                devices = devices,
                portInfo = ports,
                isLoading = false,
                capabilityTier = currentTier()
            )
        }
    }

    fun requestUsbPermission(rawDevice: UsbDevice) {
        viewModelScope.launch {
            val granted = usbScanner.requestPermission(rawDevice).first()
            if (granted) refresh()
        }
    }

    fun requestShizukuPermission() {
        shizukuPortReader.requestPermission()
    }

    val isShizukuInstalled: Boolean get() = shizukuSetup.isShizukuInstalled

    private fun currentTier(): CapabilityTier = when {
        shizukuPortReader.hasPermission() -> CapabilityTier.ENHANCED
        else -> CapabilityTier.BASIC
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
                _uiState.value = _uiState.value.copy(
                    shizukuState = state,
                    capabilityTier = currentTier()
                )
            }
        }
    }
}

data class HomeUiState(
    val devices: List<UsbDeviceInfo> = emptyList(),
    val portInfo: List<UsbPortInfo> = emptyList(),
    val isLoading: Boolean = false,
    val capabilityTier: CapabilityTier = CapabilityTier.BASIC,
    val shizukuState: ShizukuUsbPortReader.ShizukuState = ShizukuUsbPortReader.ShizukuState.NotRunning
)
