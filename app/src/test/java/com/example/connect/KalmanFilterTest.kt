package com.example.connect

import com.example.connect.logic.TouchSmoother
import org.junit.Assert.assertEquals
import org.junit.Test

class KalmanFilterTest {

    @Test
    fun testTouchSmoothing() {
        val smoother = TouchSmoother()
        
        // Initial state
        val initialX = 100f
        val initialY = 100f
        smoother.reset(initialX, initialY)
        var (sx, sy) = smoother.smooth(initialX, initialY)
        
        // Initial smooth result (first sample)
        assertEquals(100f, sx, 1.0f)
        assertEquals(100f, sy, 1.0f)

        // Move slightly
        val nextX = 110f
        val nextY = 110f
        val result = smoother.smooth(nextX, nextY)
        
        // The output should be moving towards 110 but dampened
        assert(result.first > 100f && result.first < 110f)
        assert(result.second > 100f && result.second < 110f)
        
        println("Kalman Filter Test Passed: Input=(110,110) Output=(${result.first}, ${result.second})")
    }
}
