package com.example.connect.mirror

import android.media.MediaFormat

/**
 * Shared constants for the media pipeline to avoid duplication and configuration drift.
 */
object MediaConstants {
    const val MIMETYPE_VIDEO_AVC = MediaFormat.MIMETYPE_VIDEO_AVC
    const val KEY_LOW_LATENCY = 1
    const val KEY_PRIORITY_REALTIME = 0
    
    // Vendor-specific low-latency keys
    const val VENDOR_LOW_LATENCY = "vendor.rtc-ext-dec-low-latency.enable"
    const val VENDOR_INTRA_REFRESH = "intra-refresh-period"
    
    // Default Resolution
    const val DEFAULT_WIDTH = 1920
    const val DEFAULT_HEIGHT = 1080
}
