package com.example.connect.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.connect.ui.theme.ConnectDesign

@Composable
fun RadarAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "RadarPulse")
    
    // Scale and Alpha for 3 waves
    val wave1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave1"
    )
    
    val wave2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave2"
    )
    
    val wave3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave3"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val waves = listOf(wave1, wave2, wave3)
            waves.forEach { waveProgress ->
                val radius = size.minDimension / 2 * waveProgress
                val alpha = (1f - waveProgress) * 0.5f
                
                drawCircle(
                    color = ConnectDesign.NeonBlue,
                    radius = radius,
                    alpha = alpha,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
