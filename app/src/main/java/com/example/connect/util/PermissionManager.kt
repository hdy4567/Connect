package com.example.connect.util

import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(
    private val activity: ComponentActivity,
    private val onAllPermissionsGranted: () -> Unit
) {
    private val locationPermissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val nearbyPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(android.Manifest.permission.NEARBY_WIFI_DEVICES)
    } else emptyArray()

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.BLUETOOTH_SCAN
        )
    } else emptyArray()

    private val locationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { checkNext() }

    private val nearbyLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { checkNext() }

    private val bluetoothLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { checkNext() }

    fun startPermissionChain() {
        checkNext()
    }

    private fun checkNext() {
        // 1. Location
        if (!hasPermissions(locationPermissions)) {
            locationLauncher.launch(locationPermissions)
            return
        }

        // 2. Nearby
        if (nearbyPermissions.isNotEmpty() && !hasPermissions(nearbyPermissions)) {
            nearbyLauncher.launch(nearbyPermissions)
            return
        }

        // 3. Bluetooth
        if (bluetoothPermissions.isNotEmpty() && !hasPermissions(bluetoothPermissions)) {
            bluetoothLauncher.launch(bluetoothPermissions)
            return
        }

        onAllPermissionsGranted()
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}
