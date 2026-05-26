package com.whatcable.android.data.shizuku

import android.content.pm.PackageManager
import android.hardware.usb.IUsbManager
import android.os.Build
import com.whatcable.android.core.model.ComplianceWarning
import com.whatcable.android.core.model.DataRole
import com.whatcable.android.core.model.PortMode
import com.whatcable.android.core.model.PortOrientation
import com.whatcable.android.core.model.PowerRole
import com.whatcable.android.core.model.UsbPortInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuUsbPortReader @Inject constructor() {

    sealed class ShizukuState {
        object NotInstalled : ShizukuState()
        object NotRunning : ShizukuState()
        object PermissionDenied : ShizukuState()
        object Ready : ShizukuState()
    }

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val _ports = MutableStateFlow<List<UsbPortInfo>>(emptyList())
    val ports: StateFlow<List<UsbPortInfo>> = _ports.asStateFlow()

    private var binderReady = false
    private var iUsbManager: IUsbManager? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        binderReady = true
        updateState()
        if (hasPermission()) {
            iUsbManager = buildUsbManager()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        binderReady = false
        iUsbManager = null
        _state.value = ShizukuState.NotRunning
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == PackageManager.PERMISSION_GRANTED) {
            iUsbManager = buildUsbManager()
            _state.value = ShizukuState.Ready
        } else {
            _state.value = ShizukuState.PermissionDenied
        }
    }

    fun init() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
    }

    fun destroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    fun requestPermission() {
        if (!binderReady) return
        if (Shizuku.isPreV11()) return
        if (hasPermission()) {
            iUsbManager = buildUsbManager()
            _state.value = ShizukuState.Ready
            return
        }
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            _state.value = ShizukuState.PermissionDenied
            return
        }
        Shizuku.requestPermission(0)
    }

    fun hasPermission(): Boolean {
        if (!binderReady) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    suspend fun readPorts(): List<UsbPortInfo> = withContext(Dispatchers.IO) {
        val mgr = iUsbManager ?: return@withContext emptyList()
        try {
            val ports = mgr.ports ?: return@withContext emptyList()
            val result = ports.map { port ->
                val status = mgr.getPortStatus(port.id)
                if (status == null) {
                    UsbPortInfo(
                        id = port.id,
                        isConnected = false,
                        mode = PortMode.NONE,
                        powerRole = PowerRole.NONE,
                        dataRole = DataRole.NONE,
                        orientation = PortOrientation.UNKNOWN,
                        contaminantDetected = null,
                        usbDataEnabled = null,
                        powerTransferLimited = null,
                        complianceWarnings = emptyList()
                    )
                } else {
                    UsbPortInfo(
                        id = port.id,
                        isConnected = status.isConnected,
                        mode = PortMode.fromInt(status.currentMode),
                        powerRole = PowerRole.fromInt(status.currentPowerRole),
                        dataRole = DataRole.fromInt(status.currentDataRole),
                        orientation = if (Build.VERSION.SDK_INT >= 34) {
                            PortOrientation.fromPlugState(status.plugState)
                        } else {
                            PortOrientation.UNKNOWN
                        },
                        contaminantDetected = if (Build.VERSION.SDK_INT >= 29) {
                            status.contaminantDetectionStatus == 3
                        } else null,
                        usbDataEnabled = if (Build.VERSION.SDK_INT >= 31) {
                            status.usbDataStatus == 0
                        } else null,
                        powerTransferLimited = if (Build.VERSION.SDK_INT >= 31) {
                            status.isPowerTransferLimited
                        } else null,
                        complianceWarnings = if (Build.VERSION.SDK_INT >= 34) {
                            status.complianceWarnings?.map { ComplianceWarning.fromInt(it) }
                                ?: emptyList()
                        } else emptyList()
                    )
                }
            }
            _ports.value = result
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun updateState() {
        _state.value = when {
            !binderReady -> ShizukuState.NotRunning
            !hasPermission() -> ShizukuState.PermissionDenied
            else -> ShizukuState.Ready
        }
    }

    private fun buildUsbManager(): IUsbManager? {
        val binder = SystemServiceHelper.getSystemService("usb") ?: return null
        return IUsbManager.Stub.asInterface(ShizukuBinderWrapper(binder))
    }
}
