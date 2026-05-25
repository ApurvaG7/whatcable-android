package com.whatcable.android.ui.home

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatcable.android.core.model.UsbDeviceInfo
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
    private val usbScanner: UsbHostScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeUsbEvents()
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val devices = usbScanner.scan()
        _uiState.value = _uiState.value.copy(
            devices = devices,
            isLoading = false
        )
    }

    fun requestPermission(rawDevice: UsbDevice) {
        viewModelScope.launch {
            val granted = usbScanner.requestPermission(rawDevice).first()
            if (granted) {
                refresh()
            }
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
}

data class HomeUiState(
    val devices: List<UsbDeviceInfo> = emptyList(),
    val isLoading: Boolean = false
)
