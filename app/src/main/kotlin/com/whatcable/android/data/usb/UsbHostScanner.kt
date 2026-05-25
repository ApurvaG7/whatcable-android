package com.whatcable.android.data.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.whatcable.android.core.model.UsbDeviceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbHostScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbManager: UsbManager,
    private val bosReader: BosReader
) {
    companion object {
        const val ACTION_USB_PERMISSION = "com.whatcable.android.USB_PERMISSION"
    }

    private val _devices = MutableStateFlow<List<UsbDeviceInfo>>(emptyList())
    val devices: StateFlow<List<UsbDeviceInfo>> = _devices.asStateFlow()

    fun scan(): List<UsbDeviceInfo> {
        val deviceList = usbManager.deviceList
        val parsed = deviceList.values.map { device ->
            val connection = if (usbManager.hasPermission(device)) {
                usbManager.openDevice(device)
            } else {
                null
            }
            try {
                val bos = connection?.let { bosReader.read(it) }
                DescriptorParser.parse(device, connection, bos)
            } finally {
                connection?.close()
            }
        }
        _devices.value = parsed
        return parsed
    }

    fun requestPermission(device: UsbDevice): Flow<Boolean> = callbackFlow {
        if (usbManager.hasPermission(device)) {
            trySend(true)
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == ACTION_USB_PERMISSION) {
                    val granted = intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED, false
                    )
                    trySend(granted)
                    close()
                }
            }
        }

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), flags
        )
        usbManager.requestPermission(device, permissionIntent)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered
            }
        }
    }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    fun openDevice(device: UsbDevice) = usbManager.openDevice(device)

    fun getDeviceList(): Map<String, UsbDevice> = usbManager.deviceList

    fun observeUsbEvents(): Flow<UsbEvent> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }

                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        device?.let { trySend(UsbEvent.Attached(it)) }
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        device?.let { trySend(UsbEvent.Detached(it)) }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        // Emit current devices on subscription
        usbManager.deviceList.values.forEach {
            trySend(UsbEvent.Attached(it))
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered
            }
        }
    }
}

sealed class UsbEvent {
    data class Attached(val device: UsbDevice) : UsbEvent()
    data class Detached(val device: UsbDevice) : UsbEvent()
}
