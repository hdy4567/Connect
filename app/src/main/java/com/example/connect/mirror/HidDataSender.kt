package com.example.connect.mirror

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log

/**
 * Handles Bluetooth HID (Human Interface Device) role.
 * Allows this device to act as a Mouse/Keyboard for another device.
 */
import com.example.connect.config.HidConfig
import androidx.core.content.ContextCompat

@SuppressLint("NewApi")
class HidDataSender(private val context: Context) {

    private var bHid: BluetoothHidDevice? = null
    private var targetDevice: BluetoothDevice? = null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bHid = proxy as BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bHid = null
            }
        }
    }

    init {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
    }

    private fun registerApp() {
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            HidConfig.HID_APP_NAME,
            HidConfig.HID_APP_DESCRIPTION,
            HidConfig.HID_MANUFACTURER,
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidConfig.COMBO_REPORT_DESCRIPTOR // Use Combo descriptor (Touch + Mouse)
        )
        
        bHid?.registerApp(
            sdpSettings,
            null,
            null,
            ContextCompat.getMainExecutor(context),
            object : BluetoothHidDevice.Callback() {
                override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                    Log.d("HidDataSender", "App status changed: registered=$registered")
                }

                override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                    Log.d("HidDataSender", "Connection state changed: device=$device, state=$state")
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        targetDevice = device
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        targetDevice = null
                    }
                }
            }
        )
    }

    fun sendAbsoluteTouch(x: Int, y: Int, isDown: Boolean) {
        val device = targetDevice ?: return
        val tipSwitch = if (isDown) 0x01 else 0x00
        val report = byteArrayOf(
            tipSwitch.toByte(),
            (x and 0xFF).toByte(),
            ((x shr 8) and 0xFF).toByte(),
            (y and 0xFF).toByte(),
            ((y shr 8) and 0xFF).toByte()
        )
        bHid?.sendReport(device, 1, report)
    }


    /**
     * Sends a mouse report (Report ID 2) for scrolling and standard buttons.
     */
    fun sendMouseReport(buttons: Byte, wheel: Byte) {
        val device = targetDevice ?: return
        val report = byteArrayOf(
            buttons, // Bit 0: Mid, Bit 1: Right, Bit 2: Left
            wheel    // Vertical scroll wheel (-127 to 127)
        )
        bHid?.sendReport(device, 2, report)
    }

    fun sendMouseMovement(dx: Int, dy: Int, leftClick: Boolean = false) {
        // Fallback for relative movement if needed
    }

    fun unregister() {
        bHid?.unregisterApp()
        BluetoothAdapter.getDefaultAdapter()?.closeProfileProxy(BluetoothProfile.HID_DEVICE, bHid)
    }
}
