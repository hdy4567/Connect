package com.example.connect.logic

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

/**
 * Monitors system thermal state and battery to adjust streaming quality.
 * Complies with Section 0.031 (Thermal Throttling Guard).
 */
class ThermalGuard(private val context: Context) {

    interface ThermalListener {
        fun onThrottleRequired(bitrateScale: Float, fpsScale: Float)
    }

    private var listener: ThermalListener? = null

    fun setListener(listener: ThermalListener) {
        this.listener = listener
    }

    /**
     * Checks current thermal status and notifies listener if adjustment is needed.
     * In a real app, we'd use PowerManager.addThermalStatusListener on API 29+.
     */
    fun checkThermalState() {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        
        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatureCelsius = temp / 10f

        Log.d("ThermalGuard", "Current Device Temperature: $temperatureCelsius°C")

        when {
            temperatureCelsius > 45f -> {
                // Critical: Aggressive throttling
                listener?.onThrottleRequired(0.3f, 0.5f) // 30% bitrate, 30fps
            }
            temperatureCelsius > 40f -> {
                // Warning: Moderate throttling
                listener?.onThrottleRequired(0.6f, 0.8f) // 60% bitrate, 48fps
            }
            else -> {
                // Normal: Full performance
                listener?.onThrottleRequired(1.0f, 1.0f)
            }
        }
    }
}
