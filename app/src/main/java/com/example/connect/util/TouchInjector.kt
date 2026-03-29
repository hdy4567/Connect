package com.example.connect.util

import android.app.Instrumentation
import android.os.SystemClock
import android.view.MotionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * TouchInjector allows the app to simulate touch events on its own screen.
 * Note: Requires 'INJECT_EVENTS' permission (usually system app or ADB) 
 * or can be used within the app's own window scope.
 */
class TouchInjector {
    private val instrumentation = Instrumentation()

    suspend fun injectTouch(x: Float, y: Float, isDown: Boolean) = withContext(Dispatchers.IO) {
        try {
            val action = if (isDown) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP
            val event = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                action,
                x,
                y,
                0
            )
            instrumentation.sendPointerSync(event)
            event.recycle()
        } catch (e: Exception) {
            // Log.e("TouchInjector", "Failed to inject: ${e.message}")
        }
    }
}
