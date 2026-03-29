package com.example.connect.logic

/**
 * 1D Kalman Filter for touch coordinate smoothing.
 * Reduces jitter while maintaining low latency.
 */
class KalmanFilter(
    private var processNoise: Float = 0.05f,   
    private var measurementNoise: Float = 0.8f 
) {
    private var x: Float = 0f
    private var v: Float = 0f
    private var p11: Float = 1f
    private var p12: Float = 0f
    private var p21: Float = 0f
    private var p22: Float = 1f
    
    fun update(z: Float, dt: Float = 0.010f): Float {
        // 1. Predict (Constant Velocity)
        x += v * dt
        p11 += (p12 + p21) * dt + p22 * dt * dt + processNoise
        p12 += p22 * dt
        p21 += p22 * dt
        p22 += processNoise
        
        // 2. Update (Correct)
        val s = p11 + measurementNoise
        val k1 = p11 / s
        val k2 = p21 / s
        
        val y = z - x
        x += k1 * y
        v += k2 * y
        
        val l11 = 1f - k1
        p12 = l11 * p12
        p22 = p22 - k2 * p12
        p11 = l11 * p11
        p21 = p21 - k2 * p11
        
        return x
    }

    fun reset(value: Float) {
        x = value
        v = 0f
        p11 = 1f; p12 = 0f; p21 = 0f; p22 = 1f
    }
}

/**
 * Helper to manage 2D coordinates with Kalman smoothing.
 */
class TouchSmoother {
    private val filterX = KalmanFilter()
    private val filterY = KalmanFilter()

    fun smooth(x: Float, y: Float): Pair<Float, Float> {
        return filterX.update(x) to filterY.update(y)
    }
    
    fun reset(x: Float, y: Float) {
        filterX.reset(x)
        filterY.reset(y)
    }
}
