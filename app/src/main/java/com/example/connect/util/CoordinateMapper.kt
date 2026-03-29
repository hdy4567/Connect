package com.example.connect.util

import android.graphics.PointF

/**
 * CoordinateMapper handles translating between screen pixel coordinates
 * and the 0-4095 absolute range used by the HID protocol and session packets.
 */
object CoordinateMapper {
    private const val ABSOLUTE_MAX = 4095f

    /**
     * Maps screen coordinates to absolute range (0-4095), accounting for aspect ratio fit.
     * Returns null if the touch is outside the actual video content (e.g., on black bars).
     */
    fun toAbsolute(
        x: Float,
        y: Float,
        viewWidth: Float,
        viewHeight: Float,
        sourceWidth: Float = 1920f,
        sourceHeight: Float = 1080f
    ): Pair<Int, Int>? {
        if (viewWidth <= 0 || viewHeight <= 0) return null

        val viewAspect = viewWidth / viewHeight
        val sourceAspect = sourceWidth / sourceHeight

        val actualWidth: Float
        val actualHeight: Float
        val offsetX: Float
        val offsetY: Float

        if (viewAspect > sourceAspect) {
            // Pillarboxed (bars on left/right)
            actualHeight = viewHeight
            actualWidth = viewHeight * sourceAspect
            offsetX = (viewWidth - actualWidth) / 2f
            offsetY = 0f
        } else {
            // Letterboxed (bars on top/bottom)
            actualWidth = viewWidth
            actualHeight = viewWidth / sourceAspect
            offsetX = 0f
            offsetY = (viewHeight - actualHeight) / 2f
        }

        // Clip touch to actual content area
        val cx = (x - offsetX).coerceIn(0f, actualWidth)
        val cy = (y - offsetY).coerceIn(0f, actualHeight)

        val nx = ((cx / actualWidth) * ABSOLUTE_MAX).toInt()
        val ny = ((cy / actualHeight) * ABSOLUTE_MAX).toInt()

        return nx to ny
    }

    /**
     * Maps absolute range (0-4095) back to local coordinates for visual feedback.
     */
    fun fromAbsolute(
        nx: Int,
        ny: Int,
        viewWidth: Float,
        viewHeight: Float,
        sourceWidth: Float = 1920f,
        sourceHeight: Float = 1080f
    ): PointF {
        val viewAspect = viewWidth / viewHeight
        val sourceAspect = sourceWidth / sourceHeight

        val actualWidth: Float
        val actualHeight: Float
        val offsetX: Float
        val offsetY: Float

        if (viewAspect > sourceAspect) {
            actualHeight = viewHeight
            actualWidth = viewHeight * sourceAspect
            offsetX = (viewWidth - actualWidth) / 2f
            offsetY = 0f
        } else {
            actualWidth = viewWidth
            actualHeight = viewWidth / sourceAspect
            offsetX = 0f
            offsetY = (viewHeight - actualHeight) / 2f
        }

        val px = (nx.toFloat() / ABSOLUTE_MAX) * actualWidth + offsetX
        val py = (ny.toFloat() / ABSOLUTE_MAX) * actualHeight + offsetY
        return PointF(px, py)
    }
}
