package com.example.connect.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build

object DeviceUtils {

    /**
     * Heuristic to determine if the device is a Laptop (Chromebook) or acting as one.
     */
    fun isLaptop(context: Context): Boolean {
        // Chromebooks often have this system feature
        val isChromebook = context.packageManager.hasSystemFeature("org.chromium.arc.device_management")
        // Also check for large screens with keyboard attached or specific models if needed
        return isChromebook || Build.DEVICE.contains("chromebook", ignoreCase = true)
    }

    /**
     * Determines if the device is a Tablet.
     */
    fun isTablet(context: Context): Boolean {
        val xlarge = (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
        return xlarge && !isLaptop(context)
    }

    /**
     * Gets numerical priority for Wi-Fi Direct Group Owner.
     * Laptop (15) > Tablet (10) > Phone (0)
     */
    fun getGroupOwnerIntent(context: Context): Int {
        return when {
            isLaptop(context) -> 15
            isTablet(context) -> 7  // Intermediate priority
            else -> 0              // Default (Client preferred)
        }
    }
}
