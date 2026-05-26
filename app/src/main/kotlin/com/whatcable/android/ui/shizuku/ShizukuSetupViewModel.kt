package com.whatcable.android.ui.shizuku

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatcable.android.data.shizuku.ShizukuSetupHelper
import com.whatcable.android.data.shizuku.ShizukuUsbPortReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShizukuSetupViewModel @Inject constructor(
    private val shizukuPortReader: ShizukuUsbPortReader,
    private val setupHelper: ShizukuSetupHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<ShizukuSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shizukuPortReader.state.collect {
                _uiState.value = buildState()
            }
        }
    }

    fun refreshInstallState() {
        _uiState.value = buildState()
    }

    fun requestPermission() {
        shizukuPortReader.requestPermission()
    }

    fun getPlayStoreIntent(): Intent = setupHelper.openPlayStore()

    fun getShizukuAppIntent(): Intent? = setupHelper.openShizukuApp()

    fun getAppSettingsIntent(): Intent = setupHelper.openAppSettings()

    private fun buildState(): ShizukuSetupUiState {
        val shizukuState = shizukuPortReader.state.value
        val installed = setupHelper.isShizukuInstalled

        val step = when {
            !installed -> SetupStep.INSTALL
            shizukuState == ShizukuUsbPortReader.ShizukuState.NotRunning -> SetupStep.START
            shizukuState == ShizukuUsbPortReader.ShizukuState.PermissionDenied -> SetupStep.GRANT
            shizukuState == ShizukuUsbPortReader.ShizukuState.Ready -> SetupStep.DONE
            else -> SetupStep.START
        }

        val permPermanentlyDenied = shizukuState == ShizukuUsbPortReader.ShizukuState.PermissionDenied
                && shizukuPortReader.isPermissionPermanentlyDenied()

        return ShizukuSetupUiState(
            currentStep = step,
            isInstalled = installed,
            shizukuState = shizukuState,
            permissionPermanentlyDenied = permPermanentlyDenied
        )
    }
}

data class ShizukuSetupUiState(
    val currentStep: SetupStep = SetupStep.INSTALL,
    val isInstalled: Boolean = false,
    val shizukuState: ShizukuUsbPortReader.ShizukuState = ShizukuUsbPortReader.ShizukuState.NotRunning,
    val permissionPermanentlyDenied: Boolean = false
)

enum class SetupStep {
    INSTALL,
    START,
    GRANT,
    DONE
}
